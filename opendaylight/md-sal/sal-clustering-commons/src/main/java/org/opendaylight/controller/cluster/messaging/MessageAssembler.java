/*
 * Copyright (c) 2017 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.messaging;

import static java.util.Objects.requireNonNull;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalNotification;
import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import org.apache.pekko.actor.ActorRef;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.raft.spi.FileBackedOutputStream;
import org.opendaylight.raft.spi.FileBackedOutputStreamFactory;
import org.opendaylight.raft.spi.RestrictedObjectStreams;
import org.opendaylight.yangtools.concepts.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class re-assembles messages sliced into smaller chunks by {@link MessageSlicer}.
 *
 * @author Thomas Pantelis
 */
public final class MessageAssembler implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(MessageAssembler.class);

    private final @NonNull Cache<Identifier, AssembledMessageState> stateCache;
    private final @NonNull FileBackedOutputStreamFactory streamFactory;
    private final @NonNull BiConsumer<Object, ActorRef> callback;
    private final @NonNull RestrictedObjectStreams objectStreams;
    private final @NonNull String logContext;

    @NonNullByDefault
    private MessageAssembler(final String logContext, final RestrictedObjectStreams objectStreams,
            final FileBackedOutputStreamFactory streamFactory, final BiConsumer<Object, ActorRef> callback,
            final Duration expireAfterInactivity) {
        this.logContext = requireNonNull(logContext);
        this.objectStreams = requireNonNull(objectStreams);
        this.streamFactory = requireNonNull(streamFactory);
        this.callback = requireNonNull(callback);

        stateCache = CacheBuilder.newBuilder()
            .expireAfterAccess(expireAfterInactivity)
            .removalListener(this::stateRemoved)
            .build();
    }

    /**
     * Returns a new Builder for creating MessageAssembler instances.
     *
     * @return a Builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Checks if the given message is handled by this class. If so, it should be forwarded to the
     * {@link #handleMessage(Object, ActorRef)} method
     *
     * @param message the message to check
     * @return true if handled, false otherwise
     */
    public static boolean isHandledMessage(final Object message) {
        return message instanceof MessageSlice || message instanceof AbortSlicing;
    }

    @Override
    public void close() {
        LOG.debug("{}: Closing", logContext);
        stateCache.invalidateAll();
    }

    /**
     * Checks for and removes assembled message state that has expired due to inactivity from the slicing component
     * on the other end.
     */
    public void checkExpiredAssembledMessageState() {
        if (stateCache.size() > 0) {
            stateCache.cleanUp();
        }
    }

    /**
     * Invoked to handle message slices and other messages pertaining to this class.
     *
     * @param message the message
     * @param sendTo the reference of the actor to which subsequent message slices should be sent
     * @return {@code true} if the message was handled, {@code false} otherwise
     * @throws NullPointerException if {@code message} is {@code null} or if the message is recognized and
     *         {@code sendTo} is {@code null}
     */
    @NonNullByDefault
    public boolean handleMessage(final Object message, final ActorRef sendTo) {
        return switch (message) {
            case AbortSlicing abortSlicing -> {
                LOG.debug("{}: handleMessage: {}", logContext, abortSlicing);
                onAbortSlicing(abortSlicing);
                yield true;
            }
            case MessageSlice messageSlice -> {
                LOG.debug("{}: handleMessage: {}", logContext, messageSlice);
                onMessageSlice(messageSlice, sendTo);
                yield true;
            }
            default -> false;
        };
    }

    private void onMessageSlice(final MessageSlice messageSlice, final ActorRef sendTo) {
        final Identifier identifier = messageSlice.getIdentifier();
        try {
            final AssembledMessageState state = stateCache.get(identifier, () -> createState(messageSlice));
            processMessageSliceForState(messageSlice, state, sendTo);
        } catch (ExecutionException e) {
            final Throwable cause = e.getCause();
            final MessageSliceException messageSliceEx = cause instanceof MessageSliceException sliceEx ? sliceEx
                : new MessageSliceException(String.format("Error creating state for identifier %s", identifier), cause);

            messageSlice.getReplyTo().tell(MessageSliceReply.failed(identifier, messageSliceEx, sendTo),
                    ActorRef.noSender());
        }
    }

    private AssembledMessageState createState(final MessageSlice messageSlice) throws MessageSliceException {
        final Identifier identifier = messageSlice.getIdentifier();
        if (messageSlice.getSliceIndex() == SlicedMessageState.FIRST_SLICE_INDEX) {
            LOG.debug("{}: Received first slice for {} - creating AssembledMessageState", logContext, identifier);
            return new AssembledMessageState(identifier, messageSlice.getTotalSlices(), streamFactory, logContext);
        }

        LOG.debug("{}: AssembledMessageState not found for {} - returning failed reply", logContext, identifier);
        throw new MessageSliceException(String.format(
                "No assembled state found for identifier %s and slice index %s", identifier,
                messageSlice.getSliceIndex()), true);
    }

    private void processMessageSliceForState(final MessageSlice messageSlice, final AssembledMessageState state,
            final ActorRef sendTo) {
        final Identifier identifier = messageSlice.getIdentifier();
        final ActorRef replyTo = messageSlice.getReplyTo();
        Object reAssembledMessage = null;
        synchronized (state) {
            final int sliceIndex = messageSlice.getSliceIndex();
            try {
                final MessageSliceReply successReply = MessageSliceReply.success(identifier, sliceIndex, sendTo);
                if (state.addSlice(sliceIndex, messageSlice.getData(), messageSlice.getLastSliceHashCode())) {
                    LOG.debug("{}: Received last slice for {}", logContext, identifier);

                    reAssembledMessage = reAssembleMessage(objectStreams, state);

                    replyTo.tell(successReply, ActorRef.noSender());
                    removeState(identifier);
                } else {
                    LOG.debug("{}: Added slice for {} - expecting more", logContext, identifier);
                    replyTo.tell(successReply, ActorRef.noSender());
                }
            } catch (MessageSliceException e) {
                LOG.warn("{}: Error processing {}", logContext, messageSlice, e);
                replyTo.tell(MessageSliceReply.failed(identifier, e, sendTo), ActorRef.noSender());
                removeState(identifier);
            }
        }

        if (reAssembledMessage != null) {
            LOG.debug("{}: Notifying callback of re-assembled message {}", logContext, reAssembledMessage);
            callback.accept(reAssembledMessage, replyTo);
        }
    }

    private static Object reAssembleMessage(final RestrictedObjectStreams objectStreams,
            final AssembledMessageState state) throws MessageSliceException {
        try {
            final var assembledBytes = state.getAssembledBytes();
            try (var in = objectStreams.newObjectInputStream(assembledBytes.openStream())) {
                return in.readObject();
            }
        } catch (IOException | ClassNotFoundException e) {
            throw new MessageSliceException(String.format("Error re-assembling bytes for identifier %s",
                    state.getIdentifier()), e);
        }
    }

    private void onAbortSlicing(final AbortSlicing message) {
        removeState(message.getIdentifier());
    }

    private void removeState(final Identifier identifier) {
        LOG.debug("{}: Removing state for {}", logContext, identifier);
        stateCache.invalidate(identifier);
    }

    private void stateRemoved(final RemovalNotification<Identifier, AssembledMessageState> notification) {
        if (notification.wasEvicted()) {
            LOG.warn("{}: AssembledMessageState for {} was expired from the cache", logContext, notification.getKey());
        } else {
            LOG.debug("{}: AssembledMessageState for {} was removed from the cache due to {}", logContext,
                    notification.getKey(), notification.getCause());
        }

        notification.getValue().close();
    }

    @VisibleForTesting
    boolean hasState(final Identifier forIdentifier) {
        boolean exists = stateCache.getIfPresent(forIdentifier) != null;
        stateCache.cleanUp();
        return exists;
    }

    public static class Builder {
        private FileBackedOutputStreamFactory fileBackedStreamFactory;
        private BiConsumer<Object, ActorRef> assembledMessageCallback;
        private RestrictedObjectStreams objectStreams;
        private @NonNull Duration expireStateAfterInactivity = Duration.ofMinutes(1);
        private @NonNull String logContext = "<no-context>";

        /**
         * Default constructor.
         *
         * @deprecated Use {@link MessageAssembler#builder()} instead.
         */
        @Deprecated(since = "11.0.1", forRemoval = true)
        public Builder() {
            // No-op
        }

        /**
         * Sets the factory for creating {@link FileBackedOutputStream} instances used for streaming messages.
         *
         * @param newFileBackedStreamFactory the factory for creating {@link FileBackedOutputStream} instances
         * @return this Builder
         */
        public Builder fileBackedStreamFactory(final FileBackedOutputStreamFactory newFileBackedStreamFactory) {
            fileBackedStreamFactory = requireNonNull(newFileBackedStreamFactory);
            return this;
        }

        /**
         * Sets the {@link BiConsumer} callback for assembled messages. The callback takes the assembled message and the
         * original sender {@link ActorRef} as arguments.
         *
         * @param newAssembledMessageCallback the {@link BiConsumer} callback
         * @return this Builder
         */
        public Builder assembledMessageCallback(final BiConsumer<Object, ActorRef> newAssembledMessageCallback) {
            assembledMessageCallback = newAssembledMessageCallback;
            return this;
        }

        /**
         * Sets the {@link RestrictedObjectStreams} to use for object de-serialization.
         *
         * @param newObjectStreams the {@link RestrictedObjectStreams} use
         * @return this Builder
         */
        public Builder objectStreams(final RestrictedObjectStreams newObjectStreams) {
            objectStreams = requireNonNull(newObjectStreams);
            return this;
        }

        /**
         * Sets the duration and time unit whereby assembled message state is purged from the cache due to
         * inactivity from the slicing component on the other end. By default, state is purged after 1 minute of
         * inactivity.
         *
         * @param duration the length of time after which a state entry is purged
         * @param unit the unit the duration is expressed in
         * @return this Builder
         */
        public Builder expireStateAfterInactivity(final long duration, final TimeUnit unit) {
            if (duration <= 0) {
                throw new IllegalArgumentException("duration must be > 0");
            }
            expireStateAfterInactivity = Duration.of(duration, unit.toChronoUnit());
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
         * Builds a new MessageAssembler instance.
         *
         * @return a new MessageAssembler
         * @throws NullPointerException if one of the mandatory fields is not set
         */
        public MessageAssembler build() {
            return new MessageAssembler(logContext,
                requireNonNull(objectStreams, "RestrictedObjectStreams cannot be null"),
                requireNonNull(fileBackedStreamFactory, "FiledBackedStreamFactory cannot be null"),
                requireNonNull(assembledMessageCallback, "assembledMessageCallback cannot be null"),
                expireStateAfterInactivity);
        }
    }
}
