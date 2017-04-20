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
 * Represents a sliced message chunk.
 *
 * @author Thomas Pantelis
 */
class MessageSlice implements Serializable {
    private static final long serialVersionUID = 1L;

    private final Identifier identifier;
    private final byte[] data;
    private final int sliceIndex;
    private final int totalSlices;
    private final int lastSliceHashCode;

    MessageSlice(Identifier identifier, byte[] data, int sliceIndex, int totalSlices, int lastSliceHashCode) {
        this.identifier = Preconditions.checkNotNull(identifier);
        this.data = Preconditions.checkNotNull(data);
        this.sliceIndex = sliceIndex;
        this.totalSlices = totalSlices;
        this.lastSliceHashCode = lastSliceHashCode;
    }

    public static long getSerialversionuid() {
        return serialVersionUID;
    }

    public Identifier getIdentifier() {
        return identifier;
    }

    public byte[] getData() {
        return data;
    }

    public int getSliceIndex() {
        return sliceIndex;
    }

    public int getTotalSlices() {
        return totalSlices;
    }

    public int getLastSliceHashCode() {
        return lastSliceHashCode;
    }

    @Override
    public String toString() {
        return "MessageSlice [identifier=" + identifier + ", data length=" + data.length + ", sliceIndex="
                + sliceIndex + ", totalSlices=" + totalSlices + ", lastSliceHashCode=" + lastSliceHashCode + "]";
    }

    private Object writeReplace() {
        return new Proxy(this);
    }

    private static class Proxy implements Externalizable {
        private static final long serialVersionUID = 1L;

        private MessageSlice messageSlice;

        // checkstyle flags the public modifier as redundant which really doesn't make sense since it clearly isn't
        // redundant. It is explicitly needed for Java serialization to be able to create instances via reflection.
        @SuppressWarnings("checkstyle:RedundantModifier")
        public Proxy() {
        }

        Proxy(MessageSlice messageSlice) {
            this.messageSlice = messageSlice;
        }

        @Override
        public void writeExternal(ObjectOutput out) throws IOException {
            out.writeObject(messageSlice.identifier);
            out.writeInt(messageSlice.sliceIndex);
            out.writeInt(messageSlice.totalSlices);
            out.writeInt(messageSlice.lastSliceHashCode);
            out.writeObject(messageSlice.data);
        }

        @Override
        public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
            Identifier identifier = (Identifier) in.readObject();
            int sliceIndex = in.readInt();
            int totalSlices = in.readInt();
            int lastSliceHashCode = in.readInt();
            byte[] data = (byte[])in.readObject();

            messageSlice = new MessageSlice(identifier, data, sliceIndex, totalSlices, lastSliceHashCode);
        }

        private Object readResolve() {
            return messageSlice;
        }
    }
}
