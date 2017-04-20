/*
 * Copyright (c) 2017 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.message;

import com.google.common.base.Preconditions;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
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

    private final ConcurrentMap<Identifier, SlicedMessageState> stateMap = new ConcurrentHashMap<>();
    private final int fileBackedStreamingThreshold;
    private final String tempFileDirectory;
    private final int messageSliceSize;
    private final Consumer<Object> messageCallback;

    /**
     * Constructs an instance.
     *
     * @param messageSliceSize the maximum size (in bytes) for a message slice
     * @param fileBackedStreamingThreshold the threshold in terms of number of bytes when streaming data before it
     *                                     should switch from storing in memory to buffering to a file
     * @param tempFileDirectory the directory in which to create temp files
     * @param messageCallback callback invoked to send messages emitted by this instance
     */
    public MessageSlicer(final int messageSliceSize, final int fileBackedStreamingThreshold,
            final String tempFileDirectory, final Consumer<Object> messageCallback) {
        this.fileBackedStreamingThreshold = fileBackedStreamingThreshold;
        this.tempFileDirectory = tempFileDirectory;
        this.messageSliceSize = messageSliceSize;
        this.messageCallback = Preconditions.checkNotNull(messageCallback);
    }

    /**
     * Slices the given message and sends the first message slice.
     *
     * @param identifier the identifier of the message
     * @param message the message to slice
     * @throws IOException if an error occurs when slicing the message
     */
    public void slice(Identifier identifier, Serializable message) throws IOException {
        LOG.debug("slice: identifier: {}, message: {}", identifier, message);

        FileBackedOutputStream fileBackedStream =
                new FileBackedOutputStream(fileBackedStreamingThreshold, tempFileDirectory);
        try (ObjectOutputStream out = new ObjectOutputStream(fileBackedStream)) {
            out.writeObject(message);
        } catch (IOException e) {
            fileBackedStream.cleanup();
            throw e;
        }

        MessageSliceIdentifier messageSliceId = new MessageSliceIdentifier(identifier);
        SlicedMessageState state = new SlicedMessageState(messageSliceId, fileBackedStream, messageSliceSize);

        final MessageSlice firstSlice = getNextSliceMessage(state);

        LOG.debug("Sending first slice: {}", firstSlice);

        stateMap.put(messageSliceId, state);
        messageCallback.accept(firstSlice);
    }

    private MessageSlice getNextSliceMessage(SlicedMessageState state) throws IOException {
        final byte[] firstSliceBytes = state.getNextSlice();
        return new MessageSlice(state.getIdentifier(), firstSliceBytes, state.getCurrentSliceIndex(),
                state.getTotalSlices(), state.getLastSliceHashCode());
    }

    /**
     * Invoked by the client to handle messages pertaining to this class.
     *
     * @param message the message
     * @return true if the message was handled, false otherwise
     * @throws IOException if an error occurs when processing the message
     */
    public boolean handleMessage(Object message) throws IOException {
        if (message instanceof MessageSliceReply) {
            LOG.debug("handleMessage: {}", message);
            onMessageSliceReply((MessageSliceReply) message);
            return true;
        }

        return false;
    }

    private void onMessageSliceReply(MessageSliceReply reply) throws IOException {
        final SlicedMessageState state = stateMap.get(reply.getIdentifier());
        if (state == null) {
            LOG.warn("SlicedMessageState not found for {}", reply);
            return;
        }

        synchronized (state) {
            if (state.getCurrentSliceIndex() == reply.getSliceIndex()) {
                if (reply.isSuccess()) {
                    if (state.isLastSlice(reply.getSliceIndex())) {
                        LOG.debug("Received last slice reply for {}", reply.getIdentifier());
                        stateMap.remove(reply.getIdentifier());
                    } else {
                        state.setLastReplyStatus(true);

                        final MessageSlice nextSlice = getNextSliceMessage(state);
                        LOG.debug("Sending next slice: {}", nextSlice);
                        messageCallback.accept(nextSlice);
                    }
                } else {
                    LOG.warn("Received failed MessageSliceReply - will retry: {}", reply);

                    state.setLastReplyStatus(false);
                    // TODO - should we try to resend in this case?
                }
            } else {
                LOG.error("Slice index {} in {} does not match expected index {}", reply.getSliceIndex(), reply,
                        state.getCurrentSliceIndex());

                if (reply.getSliceIndex() == SlicedMessageState.INVALID_SLICE_INDEX) {
                    // Since the recipient did not find this index to be valid we should reset the state so that
                    // slicing can resume from the beginning.
                    state.reset();
                    // TODO - resend
                }
            }
        }
    }
}
