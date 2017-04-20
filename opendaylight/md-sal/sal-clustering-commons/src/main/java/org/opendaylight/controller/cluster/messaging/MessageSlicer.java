/*
 * Copyright (c) 2017 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.messaging;

import akka.actor.ActorRef;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;
import org.opendaylight.controller.cluster.io.FileBackedOutputStream;
import org.opendaylight.yangtools.concepts.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class slices messages into smaller chunks.
 *
 * @author Thomas Pantelis
 */
public class MessageSlicer {
    private static final Logger LOG = LoggerFactory.getLogger(MessageSlicer.class);
    private static final int MAX_RETRIES = 3;

    private final ConcurrentMap<Identifier, SlicedMessageState<ActorRef>> stateMap = new ConcurrentHashMap<>();
    private final int fileBackedStreamingThreshold;
    private final String tempFileDirectory;
    private final int messageSliceSize;

    /**
     * Constructs an instance.
     *
     * @param messageSliceSize the maximum size (in bytes) for a message slice
     * @param fileBackedStreamingThreshold the threshold in terms of number of bytes when streaming data before it
     *                                     should switch from storing in memory to buffering to a file
     * @param tempFileDirectory the directory in which to create temp files
     */
    public MessageSlicer(final int messageSliceSize, final int fileBackedStreamingThreshold,
            final String tempFileDirectory) {
        this.fileBackedStreamingThreshold = fileBackedStreamingThreshold;
        this.tempFileDirectory = tempFileDirectory;
        this.messageSliceSize = messageSliceSize;
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
    public <T extends Serializable> void slice(Identifier identifier, T message, ActorRef sendTo, ActorRef replyTo,
            Consumer<Throwable> onFailureCallback) {
        LOG.debug("slice: identifier: {}, message: {}", identifier, message);

        MessageSliceIdentifier messageSliceId = new MessageSliceIdentifier(identifier);
        FileBackedOutputStream fileBackedStream =
                new FileBackedOutputStream(fileBackedStreamingThreshold, tempFileDirectory);
        try (ObjectOutputStream out = new ObjectOutputStream(fileBackedStream)) {
            out.writeObject(message);
        } catch (IOException e) {
            LOG.debug("Error serializing message for {}", identifier, e);
            fileBackedStream.cleanup();
            onFailureCallback.accept(e);
        }

        try {
            SlicedMessageState<ActorRef> state = new SlicedMessageState<>(messageSliceId, fileBackedStream,
                    messageSliceSize, MAX_RETRIES, replyTo, onFailureCallback);

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
    public boolean handleMessage(Object message) {
        if (message instanceof MessageSliceReply) {
            LOG.debug("handleMessage: {}", message);
            onMessageSliceReply((MessageSliceReply) message);
            return true;
        }

        return false;
    }

    private void onMessageSliceReply(MessageSliceReply reply) {
        final SlicedMessageState<ActorRef> state = stateMap.get(reply.getIdentifier());
        if (state == null) {
            LOG.warn("SlicedMessageState not found for {}", reply);
            // TODO - send message to abort the other side
            return;
        }

        synchronized (state) {
            try {
                final Optional<MessageSliceException> failure = reply.getFailure();
                if (failure.isPresent()) {
                    LOG.warn("Received failed {}", reply);

                    if (failure.get().isRetriable()) {
                        possiblyRetrySlicing(state, reply.getSendTo());
                    } else {
                        fail(state, failure.get());
                    }

                    return;
                }

                if (state.getCurrentSliceIndex() != reply.getSliceIndex()) {
                    LOG.error("Slice index {} in {} does not match expected index {}", reply.getSliceIndex(), reply,
                            state.getCurrentSliceIndex());
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
                LOG.debug("Error processing {} for {}", reply, state.getIdentifier(), e);
                fail(state, e);
            }
        }
    }

    private void removeAndClose(final SlicedMessageState<ActorRef> state) {
        stateMap.remove(state.getIdentifier());
        state.close();
    }

    private void fail(final SlicedMessageState<ActorRef> state, Throwable failure) {
        removeAndClose(state);
        state.getOnFailureCallback().accept(failure);
    }

    private void possiblyRetrySlicing(SlicedMessageState<ActorRef> state, ActorRef sendTo) throws IOException {
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
}
