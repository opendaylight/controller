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
import com.google.common.io.ByteSource;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import javax.annotation.Nonnull;
import org.opendaylight.controller.cluster.io.FileBackedOutputStreamFactory;
import org.opendaylight.yangtools.concepts.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class re-assembles messages sliced into smaller chunks by {@link MessageSlicer}.
 *
 * @author Thomas Pantelis
 * @see MessageSlicer
 */
public class MessageAssembler implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(MessageAssembler.class);

    private final Cache<Identifier, AssembledMessageState> stateCache;
    private final FileBackedOutputStreamFactory filedBackedStreamFactory;
    private final BiConsumer<Object, ActorRef> assembledMessageCallback;
    private final String logContext;

    private MessageAssembler(Builder builder) {
        this.filedBackedStreamFactory = Preconditions.checkNotNull(builder.filedBackedStreamFactory,
                "FiledBackedStreamFactory cannot be null");
        this.assembledMessageCallback = Preconditions.checkNotNull(builder.assembledMessageCallback,
                "assembledMessageCallback cannot be null");
        this.logContext = builder.logContext;

        stateCache = CacheBuilder.newBuilder()
                .expireAfterAccess(builder.expireStateAfterInactivityDuration, builder.expireStateAfterInactivityUnit)
                .removalListener((RemovalListener<Identifier, AssembledMessageState>) notification ->
                    stateRemoved(notification)).build();
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
    public static boolean isHandledMessage(Object message) {
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
     * @return true if the message was handled, false otherwise
     */
    public boolean handleMessage(final Object message, final @Nonnull ActorRef sendTo) {
        if (message instanceof MessageSlice) {
            LOG.debug("{}: handleMessage: {}", logContext, message);
            onMessageSlice((MessageSlice) message, sendTo);
            return true;
        } else if (message instanceof AbortSlicing) {
            LOG.debug("{}: handleMessage: {}", logContext, message);
            onAbortSlicing((AbortSlicing) message);
            return true;
        }

        return false;
    }

    private void onMessageSlice(final MessageSlice messageSlice, final ActorRef sendTo) {
        final Identifier identifier = messageSlice.getIdentifier();
        try {
            final AssembledMessageState state = stateCache.get(identifier, () -> createState(messageSlice));
            processMessageSliceForState(messageSlice, state, sendTo);
        } catch (ExecutionException e) {
            final MessageSliceException messageSliceEx;
            final Throwable cause = e.getCause();
            if (cause instanceof MessageSliceException) {
                messageSliceEx = (MessageSliceException) cause;
            } else {
                messageSliceEx = new MessageSliceException(String.format(
                        "Error creating state for identifier %s", identifier), cause);
            }

            messageSlice.getReplyTo().tell(MessageSliceReply.failed(identifier, messageSliceEx, sendTo),
                    ActorRef.noSender());
        }
    }

    private AssembledMessageState createState(final MessageSlice messageSlice) throws MessageSliceException {
        final Identifier identifier = messageSlice.getIdentifier();
        if (messageSlice.getSliceIndex() == SlicedMessageState.FIRST_SLICE_INDEX) {
            LOG.debug("{}: Received first slice for {} - creating AssembledMessageState", logContext, identifier);
            return new AssembledMessageState(identifier, messageSlice.getTotalSlices(),
                    filedBackedStreamFactory, logContext);
        }

        LOG.debug("{}: AssembledMessageState not found for {} - returning failed reply", logContext, identifier);
        throw new MessageSliceException(String.format(
                "No assembled state found for identifier %s and slice index %s", identifier,
                messageSlice.getSliceIndex()), true);
    }

    private void processMessageSliceForState(final MessageSlice messageSlice, AssembledMessageState state,
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

                    reAssembledMessage = reAssembleMessage(state);

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
            assembledMessageCallback.accept(reAssembledMessage, replyTo);
        }
    }

    private Object reAssembleMessage(final AssembledMessageState state) throws MessageSliceException {
        try {
            final ByteSource assembledBytes = state.getAssembledBytes();
            try (ObjectInputStream in = new ObjectInputStream(assembledBytes.openStream())) {
                return in.readObject();
            }

        } catch (IOException | ClassNotFoundException  e) {
            throw new MessageSliceException(String.format("Error re-assembling bytes for identifier %s",
                    state.getIdentifier()), e);
        }
    }

    private void onAbortSlicing(AbortSlicing message) {
        removeState(message.getIdentifier());
    }

    private void removeState(final Identifier identifier) {
        LOG.debug("{}: Removing state for {}", logContext, identifier);
        stateCache.invalidate(identifier);
    }

    private void stateRemoved(RemovalNotification<Identifier, AssembledMessageState> notification) {
        if (notification.wasEvicted()) {
            LOG.warn("{}: AssembledMessageState for {} was expired from the cache", logContext, notification.getKey());
        } else {
            LOG.debug("{}: AssembledMessageState for {} was removed from the cache due to {}", logContext,
                    notification.getKey(), notification.getCause());
        }

        notification.getValue().close();
    }

    @VisibleForTesting
    boolean hasState(Identifier forIdentifier) {
        boolean exists = stateCache.getIfPresent(forIdentifier) != null;
        stateCache.cleanUp();
        return exists;
    }

    public static class Builder {
        private FileBackedOutputStreamFactory filedBackedStreamFactory;
        private BiConsumer<Object, ActorRef> assembledMessageCallback;
        private long expireStateAfterInactivityDuration = 1;
        private TimeUnit expireStateAfterInactivityUnit = TimeUnit.MINUTES;
        private String logContext = "<no-context>";

        /**
         * Sets the factory for creating FileBackedOutputStream instances used for streaming messages.
         *
         * @param newFiledBackedStreamFactory the factory for creating FileBackedOutputStream instances
         * @return this Builder
         */
        public Builder filedBackedStreamFactory(final FileBackedOutputStreamFactory newFiledBackedStreamFactory) {
            this.filedBackedStreamFactory = Preconditions.checkNotNull(newFiledBackedStreamFactory);
            return this;
        }

        /**
         * Sets the Consumer callback for assembled messages. The callback takes the assembled message and the
         * original sender ActorRef as arguments.
         *
         * @param newAssembledMessageCallback the Consumer callback
         * @return this Builder
         */
        public Builder assembledMessageCallback(final BiConsumer<Object, ActorRef> newAssembledMessageCallback) {
            this.assembledMessageCallback = newAssembledMessageCallback;
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
            Preconditions.checkArgument(duration > 0, "duration must be > 0");
            this.expireStateAfterInactivityDuration = duration;
            this.expireStateAfterInactivityUnit = unit;
            return this;
        }

        /**
         * Sets the context for log messages.
         *
         * @param newLogContext the log context
         * @return this Builder
         */
        public Builder logContext(final String newLogContext) {
            this.logContext = newLogContext;
            return this;
        }

        /**
         * Builds a new MessageAssembler instance.
         *
         * @return a new MessageAssembler
         */
        public MessageAssembler build() {
            return new MessageAssembler(this);
        }
    }
}
