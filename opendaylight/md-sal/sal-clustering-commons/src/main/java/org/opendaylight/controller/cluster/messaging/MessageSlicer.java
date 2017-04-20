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
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
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

    private final ConcurrentMap<Identifier, SlicedMessageState<ActorRef>> stateMap = new ConcurrentHashMap<>();
    private final FileBackedOutputStreamFactory filedBackedStreamFactory;
    private final int messageSliceSize;

    /**
     * Constructs an instance.
     *
     * @param messageSliceSize the maximum size (in bytes) for a message slice
     * @param filedBackedStreamFactory
     *                      factory for creating FileBackedOutputStream instances used for streaming messages.
     */
    public MessageSlicer(final int messageSliceSize,
            @Nonnull final FileBackedOutputStreamFactory filedBackedStreamFactory) {
        this.filedBackedStreamFactory = Preconditions.checkNotNull(filedBackedStreamFactory);
        this.messageSliceSize = messageSliceSize;
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

            stateMap.put(messageSliceId, state);
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

    private MessageSlice getNextSliceMessage(SlicedMessageState<ActorRef> state) throws IOException {
        final byte[] firstSliceBytes = state.getNextSlice();
        return new MessageSlice(state.getIdentifier(), firstSliceBytes, state.getCurrentSliceIndex(),
                state.getTotalSlices(), state.getLastSliceHashCode(), state.getReplyTarget());
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

    private void onMessageSliceReply(final MessageSliceReply reply) {
        final SlicedMessageState<ActorRef> state = stateMap.get(reply.getIdentifier());
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
        stateMap.remove(state.getIdentifier());
        state.close();
    }

    private void fail(final SlicedMessageState<ActorRef> state, final Throwable failure) {
        removeAndClose(state);
        state.getOnFailureCallback().accept(failure);
    }

    @VisibleForTesting
    boolean hasState(Identifier forIdentifier) {
        return stateMap.containsKey(forIdentifier);
    }
}
