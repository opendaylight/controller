/*
 * Copyright (c) 2017 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.messaging;

import akka.actor.ActorRef;
import com.google.common.base.Preconditions;
import com.google.common.io.ByteSource;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
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

    private final ConcurrentMap<Identifier, AssembledMessageState> stateMap = new ConcurrentHashMap<>();
    private final FileBackedOutputStreamFactory filedBackedStreamFactory;
    private final Consumer<Object> assembledMessageCallback;

    /**
     * Constructor.
     *
     * @param filedBackedStreamFactory
     *                     factory for creating FileBackedOutputStream instances used for streaming messages.
     */
    public MessageAssembler(@Nonnull final FileBackedOutputStreamFactory filedBackedStreamFactory,
            @Nonnull final Consumer<Object> assembledMessageCallback) {
        this.filedBackedStreamFactory = Preconditions.checkNotNull(filedBackedStreamFactory);
        this.assembledMessageCallback = Preconditions.checkNotNull(assembledMessageCallback);
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
        }

        return false;
    }

    private void onMessageSlice(final MessageSlice messageSlice, final ActorRef sendTo) {
        final Identifier identifier = messageSlice.getIdentifier();
        AssembledMessageState state = stateMap.get(identifier);
        if (state == null) {
            state = new AssembledMessageState(identifier,  messageSlice.getTotalSlices(), filedBackedStreamFactory);
            final AssembledMessageState previous = stateMap.putIfAbsent(identifier, state);
            if (previous != null) {
                state = previous;
            }
        }

        synchronized (state) {
            final int sliceIndex = messageSlice.getSliceIndex();
            final ActorRef replyTo = messageSlice.getReplyTo();
            try {
                final MessageSliceReply successReply = MessageSliceReply.success(identifier, sliceIndex, sendTo);
                if (state.addSlice(sliceIndex, messageSlice.getData(), messageSlice.getLastSliceHashCode())) {
                    LOG.debug("Received last slice for {}", identifier);

                    assembleBytesAndNotify(state, successReply, replyTo);
                } else {
                    LOG.debug("Added slice for {} - expecting more", identifier);
                    replyTo.tell(successReply, ActorRef.noSender());
                }
            } catch (MessageSliceException e) {
                LOG.warn("Error processing {}", messageSlice, e);
                removeAndClose(state);
                replyTo.tell(MessageSliceReply.failed(identifier, sendTo, e), ActorRef.noSender());
            }
        }
    }

    private void assembleBytesAndNotify(final AssembledMessageState state, final MessageSliceReply successReply,
            final ActorRef replyTo) throws MessageSliceException {
        try {
            final ByteSource assembledBytes = state.getAssembledBytes();
            final Object originalMessage;
            try (ObjectInputStream in = new ObjectInputStream(assembledBytes.openStream())) {
                originalMessage = in.readObject();
            }
            removeAndClose(state);
            replyTo.tell(successReply, ActorRef.noSender());

            assembledMessageCallback.accept(originalMessage);
        } catch (IOException | ClassNotFoundException  e) {
            throw new MessageSliceException("Error re-assembling bytes", e);
        }
    }

    private void removeAndClose(final AssembledMessageState state) {
        stateMap.remove(state.getIdentifier());
        state.close();
    }
}
