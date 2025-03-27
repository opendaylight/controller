/*
 * Copyright (c) 2017 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.messaging;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.requireNonNull;

import java.io.Serializable;
import java.util.function.Consumer;
import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.ActorSelection;
import org.opendaylight.raft.spi.FileBackedOutputStream;
import org.opendaylight.raft.spi.FileBackedOutputStreamFactory;
import org.opendaylight.yangtools.concepts.Identifier;

/**
 * Options for slicing a message with {@link MessageSlicer#slice(SliceOptions)}.
 *
 * @author Thomas Pantelis
 */
public final class SliceOptions {
    private final Builder builder;

    private SliceOptions(final Builder builder) {
        this.builder = builder;
    }

    public Identifier getIdentifier() {
        return builder.identifier;
    }

    public FileBackedOutputStream getFileBackedStream() {
        return builder.fileBackedStream;
    }

    public Serializable getMessage() {
        return builder.message;
    }

    public ActorRef getSendToRef() {
        return builder.sendToRef;
    }

    public ActorSelection getSendToSelection() {
        return builder.sendToSelection;
    }

    public ActorRef getReplyTo() {
        return builder.replyTo;
    }

    public Consumer<Throwable> getOnFailureCallback() {
        return builder.onFailureCallback;
    }

    /**
     * Returns a new Builder for creating MessageSlicer instances.
     *
     * @return a Builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Identifier identifier;
        private FileBackedOutputStream fileBackedStream;
        private Serializable message;
        private ActorRef sendToRef;
        private ActorSelection sendToSelection;
        private ActorRef replyTo;
        private Consumer<Throwable> onFailureCallback;
        private boolean sealed;

        /**
         * Sets the identifier of the component to slice.
         *
         * @param newIdentifier the identifier
         * @return this Builder
         */
        public Builder identifier(final Identifier newIdentifier) {
            checkSealed();
            identifier = newIdentifier;
            return this;
        }

        /**
         * Sets the {@link FileBackedOutputStream} containing the message data to slice.
         *
         * @param newFileBackedStream the {@link FileBackedOutputStream}
         * @return this Builder
         */
        public Builder fileBackedOutputStream(final FileBackedOutputStream newFileBackedStream) {
            checkSealed();
            fileBackedStream = newFileBackedStream;
            return this;
        }

        /**
         * Sets the message to slice. The message is first serialized to a {@link FileBackedOutputStream}. If the
         * message doesn't need to be sliced, ie its serialized size is less than the maximum message slice size, then
         * the original message is sent. Otherwise the first message slice is sent.
         *
         * <p><b>Note:</b> a {@link FileBackedOutputStreamFactory} must be set in the {@link MessageSlicer}.
         *
         * @param newMessage the message
         * @param <T> the Serializable message type
         * @return this Builder
         */
        public <T extends Serializable> Builder message(final T newMessage) {
            checkSealed();
            message = newMessage;
            return this;
        }

        /**
         * Sets the reference of the actor to which to send the message slices.
         *
         * @param sendTo the ActorRef
         * @return this Builder
         */
        public Builder sendTo(final ActorRef sendTo) {
            checkSealed();
            sendToRef = sendTo;
            return this;
        }

        /**
         * Sets the ActorSelection to which to send the message slices.
         *
         * @param sendTo the ActorSelection
         * @return this Builder
         */
        public Builder sendTo(final ActorSelection sendTo) {
            checkSealed();
            sendToSelection = sendTo;
            return this;
        }

        /**
         * Sets the reference of the actor to which message slice replies should be sent. The actor should
         * forward the replies to the {@link MessageSlicer#handleMessage(Object)} method.
         *
         * @param newReplyTo the ActorRef
         * @return this Builder
         */
        public Builder replyTo(final ActorRef newReplyTo) {
            checkSealed();
            replyTo = newReplyTo;
            return this;
        }

        /**
         * Sets the callback to be notified of failure.
         *
         * @param newOnFailureCallback the callback
         * @return this Builder
         */
        public Builder onFailureCallback(final Consumer<Throwable> newOnFailureCallback) {
            checkSealed();
            onFailureCallback = newOnFailureCallback;
            return this;
        }

        /**
         * Builds a new SliceOptions instance.
         *
         * @return a new SliceOptions
         */
        public SliceOptions build() {
            sealed = true;

            requireNonNull(identifier, "identifier must be set");
            requireNonNull(replyTo, "replyTo must be set");
            requireNonNull(onFailureCallback, "onFailureCallback must be set");
            checkState(fileBackedStream == null || message == null,
                    "Only one of message and fileBackedStream can be set");
            checkState(!(fileBackedStream == null && message == null),
                    "One of message and fileBackedStream must be set");
            checkState(sendToRef == null || sendToSelection == null,
                    "Only one of sendToRef and sendToSelection can be set");
            checkState(!(sendToRef == null && sendToSelection == null),
                    "One of sendToRef and sendToSelection must be set");

            return new SliceOptions(this);
        }

        protected void checkSealed() {
            checkState(!sealed, "Builder is already sealed - further modifications are not allowed");
        }
    }
}
