/*
 * Copyright (c) 2017 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.message;

import com.google.common.base.Preconditions;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;
import org.opendaylight.yangtools.concepts.Identifier;

/**
 * The reply message for {@link MessageSlice}.
 *
 * @author Thomas Pantelis
 */
class MessageSliceReply implements Serializable {
    private static final long serialVersionUID = 1L;

    private final Identifier identifier;
    private final int sliceIndex;
    private final boolean success;

    MessageSliceReply(Identifier identifier, int sliceIndex, boolean success) {
        this.identifier = Preconditions.checkNotNull(identifier);
        this.sliceIndex = sliceIndex;
        this.success = success;
    }

    Identifier getIdentifier() {
        return identifier;
    }

    int getSliceIndex() {
        return sliceIndex;
    }

    boolean isSuccess() {
        return success;
    }

    @Override
    public String toString() {
        return "MessageSliceReply [identifier=" + identifier + ", sliceIndex=" + sliceIndex + ", success=" + success
                + "]";
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
            out.writeBoolean(messageSliceReply.success);
        }

        @Override
        public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
            messageSliceReply = new MessageSliceReply((Identifier) in.readObject(), in.readInt(), in.readBoolean());
        }

        private Object readResolve() {
            return messageSliceReply;
        }
    }
}
