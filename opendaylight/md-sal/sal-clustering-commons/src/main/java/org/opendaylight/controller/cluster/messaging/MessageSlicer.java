/*
 * Copyright (c) 2017 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.messaging;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalNotification;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;
import org.apache.pekko.actor.ActorRef;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.raft.spi.FileBackedOutputStream;
import org.opendaylight.raft.spi.FileBackedOutputStreamFactory;
import org.opendaylight.yangtools.concepts.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class slices messages into smaller chunks. {@link MessageAssembler} is used to re-assemble the messages.
 *
 * @author Thomas Pantelis
 * @see MessageAssembler
 */
public class MessageSlicer implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(MessageSlicer.class);
    private static final AtomicLong SLICER_ID_COUNTER = new AtomicLong(1);
    public static final int DEFAULT_MAX_SLICING_TRIES = 3;

    private final Cache<MessageSliceIdentifier, SlicedMessageState<ActorRef>> stateCache;
    private final FileBackedOutputStreamFactory fileBackedStreamFactory;
    private final int messageSliceSize;
    private final int maxSlicingTries;
    private final String logContext;
    private final long id;

    MessageSlicer(final Builder builder) {
        fileBackedStreamFactory = builder.fileBackedStreamFactory;
        messageSliceSize = builder.messageSliceSize;
        maxSlicingTries = builder.maxSlicingTries;

        id = SLICER_ID_COUNTER.getAndIncrement();
        logContext = builder.logContext + "_slicer-id-" + id;

        CacheBuilder<Identifier, SlicedMessageState<ActorRef>> cacheBuilder =
                CacheBuilder.newBuilder().removalListener(this::stateRemoved);
        if (builder.expireStateAfterInactivityDuration > 0) {
            cacheBuilder = cacheBuilder.expireAfterAccess(builder.expireStateAfterInactivityDuration,
                    builder.expireStateAfterInactivityUnit);
        }
        stateCache = cacheBuilder.build();
    }

    @VisibleForTesting
    long getId() {
        return id;
    }

    /**
     * Returns a new Builder for creating MessageSlicer instances.
     *
     * @return a Builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Checks if the given message is handled by this class. If so, it should be forwarded to the
     * {@link #handleMessage(Object)} method
     *
     * @param message the message to check
     * @return true if handled, false otherwise
     */
    public static boolean isHandledMessage(final Object message) {
        return message instanceof MessageSliceReply;
    }

    /**
     * Slices a message into chunks based on the serialized size, the maximum message slice size and the given
     * options.
     *
     * @param options the SliceOptions
     * @return true if the message was sliced, false otherwise
     */
    public boolean slice(final SliceOptions options) {
        final Identifier identifier = options.getIdentifier();
        final Serializable message = options.getMessage();
        final FileBackedOutputStream fileBackedStream;
        if (message != null) {
            LOG.debug("{}: slice: identifier: {}, message: {}", logContext, identifier, message);

            requireNonNull(fileBackedStreamFactory,
                    "The FiledBackedStreamFactory must be set in order to call this slice method");

            // Serialize the message to a FileBackedOutputStream.
            fileBackedStream = fileBackedStreamFactory.newInstance();
            try (ObjectOutputStream out = new ObjectOutputStream(fileBackedStream)) {
                out.writeObject(message);
            } catch (IOException e) {
                LOG.debug("{}: Error serializing message for {}", logContext, identifier, e);
                fileBackedStream.cleanup();
                options.getOnFailureCallback().accept(e);
                return false;
            }
        } else {
            fileBackedStream = options.getFileBackedStream();
        }

        return initializeSlicing(options, fileBackedStream);
    }

    private boolean initializeSlicing(final SliceOptions options, final FileBackedOutputStream fileBackedStream) {
        final Identifier identifier = options.getIdentifier();
        MessageSliceIdentifier messageSliceId = new MessageSliceIdentifier(identifier, id);
        SlicedMessageState<ActorRef> state = null;
        try {
            state = new SlicedMessageState<>(messageSliceId, fileBackedStream, messageSliceSize, maxSlicingTries,
                    options.getReplyTo(), options.getOnFailureCallback(), logContext);

            final Serializable message = options.getMessage();
            if (state.getTotalSlices() == 1 && message != null) {
                LOG.debug("{}: Message does not need to be sliced - sending original message", logContext);
                state.close();
                sendTo(options, message, options.getReplyTo());
                return false;
            }

            final MessageSlice firstSlice = getNextSliceMessage(state);

            LOG.debug("{}: Sending first slice: {}", logContext, firstSlice);

            stateCache.put(messageSliceId, state);
            sendTo(options, firstSlice, ActorRef.noSender());
            return true;
        } catch (IOException e) {
            LOG.error("{}: Error initializing SlicedMessageState for {}", logContext, identifier, e);
            if (state != null) {
                state.close();
            } else {
                fileBackedStream.cleanup();
            }

            options.getOnFailureCallback().accept(e);
            return false;
        }
    }

    private static void sendTo(final SliceOptions options, final Object message, final ActorRef sender) {
        if (options.getSendToRef() != null) {
            options.getSendToRef().tell(message, sender);
        } else {
            options.getSendToSelection().tell(message, sender);
        }
    }

    /**
     * Invoked to handle messages pertaining to this class.
     *
     * @param message the message
     * @return true if the message was handled, false otherwise
     */
    public boolean handleMessage(final Object message) {
        if (message instanceof MessageSliceReply sliceReply) {
            LOG.debug("{}: handleMessage: {}", logContext, sliceReply);
            return onMessageSliceReply(sliceReply);
        }

        return false;
    }

    /**
     * Checks for and removes sliced message state that has expired due to inactivity from the assembling component
     * on the other end.
     */
    public void checkExpiredSlicedMessageState() {
        if (stateCache.size() > 0) {
            stateCache.cleanUp();
        }
    }

    /**
     * Closes and removes all in-progress sliced message state.
     */
    @Override
    public void close() {
        LOG.debug("{}: Closing", logContext);
        stateCache.invalidateAll();
    }

    /**
     * Cancels all in-progress sliced message state that matches the given filter.
     *
     * @param filter filters by Identifier
     */
    public void cancelSlicing(final @NonNull Predicate<Identifier> filter) {
        stateCache.asMap().keySet().removeIf(
            messageSliceIdentifier -> filter.test(messageSliceIdentifier.getClientIdentifier()));
    }

    private static MessageSlice getNextSliceMessage(final SlicedMessageState<ActorRef> state) throws IOException {
        final byte[] firstSliceBytes = state.getNextSlice();
        return new MessageSlice(state.getIdentifier(), firstSliceBytes, state.getCurrentSliceIndex(),
                state.getTotalSlices(), state.getLastSliceHashCode(), state.getReplyTarget());
    }

    private boolean onMessageSliceReply(final MessageSliceReply reply) {
        final Identifier identifier = reply.getIdentifier();
        if (!(identifier instanceof MessageSliceIdentifier sliceIdentifier) || sliceIdentifier.getSlicerId() != id) {
            return false;
        }

        final SlicedMessageState<ActorRef> state = stateCache.getIfPresent(identifier);
        if (state == null) {
            LOG.warn("{}: SlicedMessageState not found for {}", logContext, reply);
            reply.getSendTo().tell(new AbortSlicing(identifier), ActorRef.noSender());
            return true;
        }

        synchronized (state) {
            try {
                final Optional<MessageSliceException> failure = reply.getFailure();
                if (failure.isPresent()) {
                    LOG.warn("{}: Received failed {}", logContext, reply);
                    processMessageSliceException(failure.orElseThrow(), state, reply.getSendTo());
                    return true;
                }

                if (state.getCurrentSliceIndex() != reply.getSliceIndex()) {
                    LOG.warn("{}: Slice index {} in {} does not match expected index {}", logContext,
                            reply.getSliceIndex(), reply, state.getCurrentSliceIndex());
                    reply.getSendTo().tell(new AbortSlicing(identifier), ActorRef.noSender());
                    possiblyRetrySlicing(state, reply.getSendTo());
                    return true;
                }

                if (state.isLastSlice(reply.getSliceIndex())) {
                    LOG.debug("{}: Received last slice reply for {}", logContext, identifier);
                    removeState(identifier);
                } else {
                    final MessageSlice nextSlice = getNextSliceMessage(state);
                    LOG.debug("{}: Sending next slice: {}", logContext, nextSlice);
                    reply.getSendTo().tell(nextSlice, ActorRef.noSender());
                }
            } catch (IOException e) {
                LOG.warn("{}: Error processing {}", logContext, reply, e);
                fail(state, e);
            }
        }

        return true;
    }

    private void processMessageSliceException(final MessageSliceException exception,
            final SlicedMessageState<ActorRef> state, final ActorRef sendTo) throws IOException {
        if (exception.isRetriable()) {
            possiblyRetrySlicing(state, sendTo);
        } else {
            fail(state, exception.getCause() != null ? exception.getCause() : exception);
        }
    }

    private void possiblyRetrySlicing(final SlicedMessageState<ActorRef> state, final ActorRef sendTo)
            throws IOException {
        if (state.canRetry()) {
            LOG.info("{}: Retrying message slicing for {}", logContext, state.getIdentifier());
            state.reset();
            sendTo.tell(getNextSliceMessage(state), ActorRef.noSender());
        } else {
            String message = String.format("Maximum slicing retries reached for identifier %s - failing the message",
                    state.getIdentifier());
            LOG.warn(message);
            fail(state, new RuntimeException(message));
        }
    }

    private void removeState(final Identifier identifier) {
        LOG.debug("{}: Removing state for {}", logContext, identifier);
        stateCache.invalidate(identifier);
    }

    private void stateRemoved(final RemovalNotification<Identifier, SlicedMessageState<ActorRef>> notification) {
        final SlicedMessageState<ActorRef> state = notification.getValue();
        state.close();
        if (notification.wasEvicted()) {
            LOG.warn("{}: SlicedMessageState for {} was expired from the cache", logContext, notification.getKey());
            state.getOnFailureCallback().accept(new RuntimeException(String.format(
                    "The slicing state for message identifier %s was expired due to inactivity from the assembling "
                     + "component on the other end", state.getIdentifier())));
        } else {
            LOG.debug("{}: SlicedMessageState for {} was removed from the cache due to {}", logContext,
                    notification.getKey(), notification.getCause());
        }
    }

    private void fail(final SlicedMessageState<ActorRef> state, final Throwable failure) {
        removeState(state.getIdentifier());
        state.getOnFailureCallback().accept(failure);
    }

    @VisibleForTesting
    boolean hasState(final Identifier forIdentifier) {
        boolean exists = stateCache.getIfPresent(forIdentifier) != null;
        stateCache.cleanUp();
        return exists;
    }

    public static class Builder {
        private FileBackedOutputStreamFactory fileBackedStreamFactory;
        private int messageSliceSize = -1;
        private long expireStateAfterInactivityDuration = -1;
        private TimeUnit expireStateAfterInactivityUnit = TimeUnit.MINUTES;
        private int maxSlicingTries = DEFAULT_MAX_SLICING_TRIES;
        private String logContext = "<no-context>";

        /**
         * Sets the factory for creating FileBackedOutputStream instances used for streaming messages. This factory
         * is used by the {@link MessageSlicer#slice(SliceOptions)} method if a Serializable message is passed.
         * If Serializable messages aren't passed then the factory need not be set.
         *
         * @param newFileBackedStreamFactory the factory for creating FileBackedOutputStream instances
         * @return this Builder
         */
        public Builder fileBackedStreamFactory(final FileBackedOutputStreamFactory newFileBackedStreamFactory) {
            fileBackedStreamFactory = requireNonNull(newFileBackedStreamFactory);
            return this;
        }

        /**
         * Sets the maximum size (in bytes) for a message slice.
         *
         * @param newMessageSliceSize the maximum size (in bytes)
         * @return this Builder
         */
        public Builder messageSliceSize(final int newMessageSliceSize) {
            checkArgument(newMessageSliceSize > 0, "messageSliceSize must be > 0");
            messageSliceSize = newMessageSliceSize;
            return this;
        }

        /**
         * Sets the maximum number of tries for slicing a message. If exceeded, slicing fails. The default is
         * defined by {@link #DEFAULT_MAX_SLICING_TRIES}
         *
         * @param newMaxSlicingTries the maximum number of tries
         * @return this Builder
         */
        public Builder maxSlicingTries(final int newMaxSlicingTries) {
            checkArgument(newMaxSlicingTries > 0, "newMaxSlicingTries must be > 0");
            maxSlicingTries = newMaxSlicingTries;
            return this;
        }

        /**
         * Sets the duration and time unit whereby sliced message state is purged from the cache and the associated
         * failure callback is notified due to inactivity from the assembling component on the other end. By default,
         * state is not purged due to inactivity.
         *
         * @param duration the length of time after which a state entry is purged
         * @param unit the unit the duration is expressed in
         * @return this Builder
         */
        public Builder expireStateAfterInactivity(final long duration, final TimeUnit unit) {
            checkArgument(duration > 0, "duration must be > 0");
            expireStateAfterInactivityDuration = duration;
            expireStateAfterInactivityUnit = unit;
            return this;
        }

        /**
         * Sets the context for log messages.
         *
         * @param newLogContext the log context
         * @return this Builder
         */
        public Builder logContext(final String newLogContext) {
            logContext = requireNonNull(newLogContext);
            return this;
        }

        /**
         * Builds a new MessageSlicer instance.
         *
         * @return a new MessageSlicer
         */
        public MessageSlicer build() {
            return new MessageSlicer(this);
        }
    }
}
