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
import java.util.function.Consumer;
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
public class MessageAssembler {
    private static final Logger LOG = LoggerFactory.getLogger(MessageAssembler.class);

    private final Cache<Identifier, AssembledMessageState> stateCache;
    private final FileBackedOutputStreamFactory filedBackedStreamFactory;
    private final Consumer<Object> assembledMessageCallback;

    private MessageAssembler(Builder builder) {
        this.filedBackedStreamFactory = Preconditions.checkNotNull(builder.filedBackedStreamFactory,
                "FiledBackedStreamFactory cannot be null");
        this.assembledMessageCallback = Preconditions.checkNotNull(builder.assembledMessageCallback,
                "Consumer<Object> cannot be null");

        stateCache = CacheBuilder.newBuilder()
                .expireAfterAccess(builder.expireStateAfterInactivityDuration, builder.expireStateAfterInactivityUnit)
                .removalListener((RemovalListener<Identifier, AssembledMessageState>) notification ->
                    stateRemoved(notification)).build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static boolean isHandledMessage(Object message) {
        return message instanceof MessageSlice || message instanceof AbortSlicing;
    }

    /**
     * Invoked by the client to handle messages pertaining to this class.
     *
     * @param message the message
     * @param sendTo the reference of the actor to which subsequent messages should be sent
     * @return true if the message was handled, false otherwise
     */
    public boolean handleMessage(final Object message, final @Nonnull ActorRef sendTo) {
        if (message instanceof MessageSlice) {
            LOG.debug("handleMessage: {}", message);
            onMessageSlice((MessageSlice) message, sendTo);
            return true;
        } else if (message instanceof AbortSlicing) {
            LOG.debug("handleMessage: {}", message);
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
            LOG.debug("Received first slice for {} - creating AssembledMessageState", identifier);
            return new AssembledMessageState(identifier, messageSlice.getTotalSlices(),
                    filedBackedStreamFactory);
        }

        LOG.debug("AssembledMessageState not found for {} - returning failed reply", identifier);
        throw new MessageSliceException(String.format(
                "No assembled state found for identifier %s and slice index %s", identifier,
                messageSlice.getSliceIndex()), true);
    }

    private void processMessageSliceForState(final MessageSlice messageSlice, AssembledMessageState state,
            final ActorRef sendTo) {
        final Identifier identifier = messageSlice.getIdentifier();
        Object reAssembledMessage = null;
        synchronized (state) {
            final int sliceIndex = messageSlice.getSliceIndex();
            final ActorRef replyTo = messageSlice.getReplyTo();
            try {
                final MessageSliceReply successReply = MessageSliceReply.success(identifier, sliceIndex, sendTo);
                if (state.addSlice(sliceIndex, messageSlice.getData(), messageSlice.getLastSliceHashCode())) {
                    LOG.debug("Received last slice for {}", identifier);

                    reAssembledMessage = reAssembleMessage(state);

                    removeAndClose(state);
                    replyTo.tell(successReply, ActorRef.noSender());
                } else {
                    LOG.debug("Added slice for {} - expecting more", identifier);
                    replyTo.tell(successReply, ActorRef.noSender());
                }
            } catch (MessageSliceException e) {
                LOG.warn("Error processing {}", messageSlice, e);
                removeAndClose(state);
                replyTo.tell(MessageSliceReply.failed(identifier, e, sendTo), ActorRef.noSender());
            }
        }

        if (reAssembledMessage != null) {
            LOG.debug("Notifying callback of re-assembled message {}", reAssembledMessage);
            assembledMessageCallback.accept(reAssembledMessage);
        }
    }

    private Object reAssembleMessage(final AssembledMessageState state) throws MessageSliceException {
        try {
            final ByteSource assembledBytes = state.getAssembledBytes();
            try (ObjectInputStream in = new ObjectInputStream(assembledBytes.openStream())) {
                return in.readObject();
            }

        } catch (IOException | ClassNotFoundException  e) {
            throw new MessageSliceException("Error re-assembling bytes", e);
        }
    }

    private void onAbortSlicing(AbortSlicing message) {
        AssembledMessageState state = stateCache.getIfPresent(message.getIdentifier());
        if (state != null) {
            synchronized (state) {
                removeAndClose(state);
            }
        }
    }

    private void removeAndClose(final AssembledMessageState state) {
        LOG.debug("Removing and closing state for {}", state.getIdentifier());
        stateCache.invalidate(state.getIdentifier());
    }

    private void stateRemoved(RemovalNotification<Identifier, AssembledMessageState> notification) {
        if (notification.wasEvicted()) {
            LOG.warn("AssembledMessageState for {} was expired from the cache", notification.getKey());
        } else {
            LOG.debug("AssembledMessageState for {} was removed from the cache due to {}", notification.getKey(),
                    notification.getCause());
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
        private Consumer<Object> assembledMessageCallback;
        private int expireStateAfterInactivityDuration = 1;
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
         * Sets the Consumer callback for assembled messages.
         *
         * @param newAssembledMessageCallback the Consumer callback
         * @return this Builder
         */
        public Builder assembledMessageCallback(final Consumer<Object> newAssembledMessageCallback) {
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
        public Builder expireStateAfterInactivity(final int duration, final TimeUnit unit) {
            Preconditions.checkArgument(duration > 0, "duration must be > 0");
            this.expireStateAfterInactivityDuration = duration;
            this.expireStateAfterInactivityUnit = unit;
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
