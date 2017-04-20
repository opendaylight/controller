/*
 * Copyright (c) 2017 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.messaging;

import akka.actor.ActorRef;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import javax.annotation.Nonnull;
import org.opendaylight.controller.cluster.io.FileBackedOutputStream;
import org.opendaylight.controller.cluster.io.FileBackedOutputStreamFactory;
import org.opendaylight.yangtools.concepts.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class slices messages into smaller chunks. {@link MessageAssembler} is used to re-assemble the messages.
 *
 * @author Thomas Pantelis
 * @see MessageAssembler
 */
public class MessageSlicer {
    private static final Logger LOG = LoggerFactory.getLogger(MessageSlicer.class);
    static final int MAX_RETRIES = 3;

    private final Cache<Identifier, SlicedMessageState<ActorRef>> stateCache;
    private final FileBackedOutputStreamFactory filedBackedStreamFactory;
    private final int messageSliceSize;

    private MessageSlicer(Builder builder) {
        this.filedBackedStreamFactory = Preconditions.checkNotNull(builder.filedBackedStreamFactory,
                "FiledBackedStreamFactory cannot be null");
        Preconditions.checkArgument(builder.messageSliceSize > 0, "messageSliceSize must be > 0");
        this.messageSliceSize = builder.messageSliceSize;

        CacheBuilder<Identifier, SlicedMessageState<ActorRef>> cacheBuilder = CacheBuilder.newBuilder().removalListener(
                (RemovalListener<Identifier, SlicedMessageState<ActorRef>>) notification -> stateRemoved(notification));
        if (builder.expireStateAfterInactivityDuration > 0) {
            cacheBuilder = cacheBuilder.expireAfterAccess(builder.expireStateAfterInactivityDuration,
                    builder.expireStateAfterInactivityUnit);
        }

        stateCache = cacheBuilder.build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static boolean isHandledMessage(Object message) {
        return message instanceof MessageSliceReply;
    }

    /**
     * Slices the given message and sends the first message slice. If the message doesn't need to be sliced, the
     * original message is sent.
     *
     * @param identifier the identifier of the message
     * @param message the message to slice
     * @param sendTo the reference of the actor to which to send
     * @param replyTo the reference of the actor to which to reply
     */
    public <T extends Serializable> void slice(@Nonnull final Identifier identifier, @Nonnull final T message,
            @Nonnull final ActorRef sendTo, @Nonnull final ActorRef replyTo,
            @Nonnull final Consumer<Throwable> onFailureCallback) {
        LOG.debug("slice: identifier: {}, message: {}", identifier, message);

        MessageSliceIdentifier messageSliceId = new MessageSliceIdentifier(identifier);
        FileBackedOutputStream fileBackedStream = filedBackedStreamFactory.newInstance();
        try (ObjectOutputStream out = new ObjectOutputStream(fileBackedStream)) {
            out.writeObject(message);
        } catch (IOException e) {
            LOG.debug("Error serializing message for {}", identifier, e);
            fileBackedStream.cleanup();
            onFailureCallback.accept(e);
            return;
        }

        SlicedMessageState<ActorRef> state = null;
        try {
            state = new SlicedMessageState<>(messageSliceId, fileBackedStream, messageSliceSize, MAX_RETRIES,
                    replyTo, onFailureCallback);

            if (state.getTotalSlices() == 1) {
                LOG.debug("Message does not need to be sliced - sending original message");
                state.close();
                sendTo.tell(message, replyTo);
                return;
            }

            final MessageSlice firstSlice = getNextSliceMessage(state);

            LOG.debug("Sending first slice: {}", firstSlice);

            stateCache.put(messageSliceId, state);
            sendTo.tell(firstSlice, ActorRef.noSender());
        } catch (IOException e) {
            LOG.debug("Error initializing SlicedMessageState for {}", identifier, e);
            if (state != null) {
                state.close();
            } else {
                fileBackedStream.cleanup();
            }

            onFailureCallback.accept(e);
        }
    }

    /**
     * Invoked by the client to handle messages pertaining to this class.
     *
     * @param message the message
     * @return true if the message was handled, false otherwise
     */
    public boolean handleMessage(final Object message) {
        if (message instanceof MessageSliceReply) {
            LOG.debug("handleMessage: {}", message);
            onMessageSliceReply((MessageSliceReply) message);
            return true;
        }

        return false;
    }

    /**
     * Checks for and removes sliced message state that has expired due to inactivity from the assembling component
     * on the other end.
     */
    public void checkExpiredSlicedMessageState() {
        stateCache.cleanUp();
    }

    /**
     * Closes and removes all in-progress sliced message state.
     */
    public void closeAllSlicedMessageState() {
        stateCache.invalidateAll();
    }

    private MessageSlice getNextSliceMessage(SlicedMessageState<ActorRef> state) throws IOException {
        final byte[] firstSliceBytes = state.getNextSlice();
        return new MessageSlice(state.getIdentifier(), firstSliceBytes, state.getCurrentSliceIndex(),
                state.getTotalSlices(), state.getLastSliceHashCode(), state.getReplyTarget());
    }

    private void onMessageSliceReply(final MessageSliceReply reply) {
        final SlicedMessageState<ActorRef> state = stateCache.getIfPresent(reply.getIdentifier());
        if (state == null) {
            LOG.warn("SlicedMessageState not found for {}", reply);
            reply.getSendTo().tell(new AbortSlicing(reply.getIdentifier()), ActorRef.noSender());
            return;
        }

        synchronized (state) {
            try {
                final Optional<MessageSliceException> failure = reply.getFailure();
                if (failure.isPresent()) {
                    LOG.warn("Received failed {}", reply);
                    processMessageSliceException(failure.get(), state, reply.getSendTo());
                    return;
                }

                if (state.getCurrentSliceIndex() != reply.getSliceIndex()) {
                    LOG.error("Slice index {} in {} does not match expected index {}", reply.getSliceIndex(), reply,
                            state.getCurrentSliceIndex());
                    reply.getSendTo().tell(new AbortSlicing(reply.getIdentifier()), ActorRef.noSender());
                    possiblyRetrySlicing(state, reply.getSendTo());
                    return;
                }

                if (state.isLastSlice(reply.getSliceIndex())) {
                    LOG.debug("Received last slice reply for {}", reply.getIdentifier());
                    removeAndClose(state);
                } else {
                    final MessageSlice nextSlice = getNextSliceMessage(state);
                    LOG.debug("Sending next slice: {}", nextSlice);
                    reply.getSendTo().tell(nextSlice, ActorRef.noSender());
                }
            } catch (IOException e) {
                LOG.warn("Error processing {}", reply, e);
                fail(state, e);
            }
        }
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
            LOG.info("Retrying message slicing for {}", state.getIdentifier());
            state.reset();
            sendTo.tell(getNextSliceMessage(state), ActorRef.noSender());
        } else {
            String message = String.format("Maximum slicing retries reached for message Id %s - failing the message",
                    state.getIdentifier());
            LOG.warn(message);
            fail(state, new RuntimeException(message));
        }
    }

    private void removeAndClose(final SlicedMessageState<ActorRef> state) {
        LOG.debug("Removing and closing state for {}", state.getIdentifier());
        stateCache.invalidate(state.getIdentifier());
    }

    private void stateRemoved(RemovalNotification<Identifier, SlicedMessageState<ActorRef>> notification) {
        final SlicedMessageState<ActorRef> state = notification.getValue();
        state.close();
        if (notification.wasEvicted()) {
            LOG.warn("SlicedMessageState for {} was expired from the cache", notification.getKey());
            state.getOnFailureCallback().accept(new RuntimeException(String.format(
                    "The slicing state for message Id %s was expired due to inactivity from the assembling component"
                     + " on the other end", state.getIdentifier())));
        } else {
            LOG.debug("SlicedMessageState for {} was removed from the cache due to {}", notification.getKey(),
                    notification.getCause());
        }
    }

    private void fail(final SlicedMessageState<ActorRef> state, final Throwable failure) {
        removeAndClose(state);
        state.getOnFailureCallback().accept(failure);
    }

    @VisibleForTesting
    boolean hasState(Identifier forIdentifier) {
        boolean exists = stateCache.getIfPresent(forIdentifier) != null;
        stateCache.cleanUp();
        return exists;
    }

    public static class Builder {
        private FileBackedOutputStreamFactory filedBackedStreamFactory;
        private int messageSliceSize = -1;
        private int expireStateAfterInactivityDuration = -1;
        private TimeUnit expireStateAfterInactivityUnit = TimeUnit.MINUTES;

        /**
         * Sets factory for creating FileBackedOutputStream instances used for streaming messages.
         *
         * @param newFiledBackedStreamFactory the factory for creating FileBackedOutputStream instances
         * @return this Builder
         */
        public Builder filedBackedStreamFactory(final FileBackedOutputStreamFactory newFiledBackedStreamFactory) {
            this.filedBackedStreamFactory = newFiledBackedStreamFactory;
            return this;
        }

        /**
         * Sets the maximum size (in bytes) for a message slice.
         *
         * @param newMessageSliceSize the maximum size (in bytes)
         * @return this Builder
         */
        public Builder messageSliceSize(final int newMessageSliceSize) {
            this.messageSliceSize = newMessageSliceSize;
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
        public Builder expireStateAfterInactivity(final int duration, final TimeUnit unit) {
            Preconditions.checkArgument(duration > 0, "duration must be > 0");
            this.expireStateAfterInactivityDuration = duration;
            this.expireStateAfterInactivityUnit = unit;
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
