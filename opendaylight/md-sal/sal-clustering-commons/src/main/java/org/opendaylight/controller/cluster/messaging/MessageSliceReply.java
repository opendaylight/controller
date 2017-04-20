/*
 * Copyright (c) 2017 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.messaging;

import akka.actor.ActorRef;
import akka.serialization.JavaSerializer;
import akka.serialization.Serialization;
import com.google.common.base.Preconditions;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;
import java.util.Optional;
import org.opendaylight.yangtools.concepts.Identifier;

/**
 * The reply message for {@link MessageSlice}.
 *
 * @author Thomas Pantelis
 */
public class MessageSliceReply implements Serializable {
    private static final long serialVersionUID = 1L;

    private final Identifier identifier;
    private final int sliceIndex;
    private final MessageSliceException failure;
    private final ActorRef sendTo;

    private MessageSliceReply(final Identifier identifier, final int sliceIndex, final MessageSliceException failure,
            final ActorRef sendTo) {
        this.identifier = Preconditions.checkNotNull(identifier);
        this.sliceIndex = sliceIndex;
        this.sendTo = Preconditions.checkNotNull(sendTo);
        this.failure = failure;
    }

    public static MessageSliceReply success(final Identifier identifier, final int sliceIndex, final ActorRef sendTo) {
        return new MessageSliceReply(identifier, sliceIndex, null, sendTo);
    }

    public static MessageSliceReply failed(final Identifier identifier, final MessageSliceException failure,
            final ActorRef sendTo) {
        return new MessageSliceReply(identifier, -1, failure, sendTo);
    }

    public Identifier getIdentifier() {
        return identifier;
    }

    public int getSliceIndex() {
        return sliceIndex;
    }

    public ActorRef getSendTo() {
        return sendTo;
    }

    public Optional<MessageSliceException> getFailure() {
        return Optional.ofNullable(failure);
    }

    @Override
    public String toString() {
        return "MessageSliceReply [identifier=" + identifier + ", sliceIndex=" + sliceIndex + ", failure=" + failure
                + ", sendTo=" + sendTo + "]";
    }

    private Object writeReplace() {
        return new Proxy(this);
    }

    private static class Proxy implements Externalizable {
        private static final long serialVersionUID = 1L;

        private MessageSliceReply messageSliceReply;

        // checkstyle flags the public modifier as redundant which really doesn't make sense since it clearly isn't
        // redundant. It is explicitly needed for Java serialization to be able to create instances via reflection.
        @SuppressWarnings("checkstyle:RedundantModifier")
        public Proxy() {
        }

        Proxy(MessageSliceReply messageSliceReply) {
            this.messageSliceReply = messageSliceReply;
        }

        @Override
        public void writeExternal(ObjectOutput out) throws IOException {
            out.writeObject(messageSliceReply.identifier);
            out.writeInt(messageSliceReply.sliceIndex);
            out.writeObject(messageSliceReply.failure);
            out.writeObject(Serialization.serializedActorPath(messageSliceReply.sendTo));
        }

        @Override
        public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
            final Identifier identifier = (Identifier) in.readObject();
            final int sliceIndex = in.readInt();
            final MessageSliceException failure = (MessageSliceException) in.readObject();
            ActorRef sendTo = JavaSerializer.currentSystem().value().provider()
                    .resolveActorRef((String) in.readObject());

            messageSliceReply = new MessageSliceReply(identifier, sliceIndex, failure, sendTo);
        }

        private Object readResolve() {
            return messageSliceReply;
        }
    }
}
