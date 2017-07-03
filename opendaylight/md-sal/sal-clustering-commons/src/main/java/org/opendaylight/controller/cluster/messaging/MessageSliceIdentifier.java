/*
 * Copyright (c) 2017 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.messaging;

import com.google.common.base.Preconditions;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.concurrent.atomic.AtomicLong;
import org.opendaylight.yangtools.concepts.Identifier;
import org.opendaylight.yangtools.concepts.WritableObjects;

/**
 * Identifier for a message slice that is composed of a client-supplied Identifier and an internal counter value.
 *
 * @author Thomas Pantelis
 */
final class MessageSliceIdentifier implements Identifier {
    private static final long serialVersionUID = 1L;
    private static final AtomicLong ID_COUNTER = new AtomicLong(1);

    private final Identifier clientIdentifier;
    private final long slicerId;
    private final long messageId;

    MessageSliceIdentifier(final Identifier clientIdentifier, final long slicerId) {
        this(clientIdentifier, slicerId, ID_COUNTER.getAndIncrement());
    }

    private MessageSliceIdentifier(final Identifier clientIdentifier, final long slicerId, final long messageId) {
        this.clientIdentifier = Preconditions.checkNotNull(clientIdentifier);
        this.messageId = messageId;
        this.slicerId = slicerId;
    }

    Identifier getClientIdentifier() {
        return clientIdentifier;
    }

    long getSlicerId() {
        return slicerId;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + clientIdentifier.hashCode();
        result = prime * result + (int) (messageId ^ messageId >>> 32);
        result = prime * result + (int) (slicerId ^ slicerId >>> 32);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof MessageSliceIdentifier)) {
            return false;
        }

        MessageSliceIdentifier other = (MessageSliceIdentifier) obj;
        return other.clientIdentifier.equals(clientIdentifier) && other.slicerId == slicerId
                && other.messageId == messageId;
    }

    @Override
    public String toString() {
        return "MessageSliceIdentifier [clientIdentifier=" + clientIdentifier + ", slicerId=" + slicerId
                + ", messageId=" + messageId + "]";
    }

    private Object writeReplace() {
        return new Proxy(this);
    }

    private static class Proxy implements Externalizable {
        private static final long serialVersionUID = 1L;

        private MessageSliceIdentifier messageSliceId;

        // checkstyle flags the public modifier as redundant which really doesn't make sense since it clearly isn't
        // redundant. It is explicitly needed for Java serialization to be able to create instances via reflection.
        @SuppressWarnings("checkstyle:RedundantModifier")
        public Proxy() {
        }

        Proxy(MessageSliceIdentifier messageSliceId) {
            this.messageSliceId = messageSliceId;
        }

        @Override
        public void writeExternal(ObjectOutput out) throws IOException {
            out.writeObject(messageSliceId.clientIdentifier);
            WritableObjects.writeLongs(out, messageSliceId.slicerId, messageSliceId.messageId);
        }

        @Override
        public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
            final Identifier clientIdentifier = (Identifier) in.readObject();
            final byte header = WritableObjects.readLongHeader(in);
            final long slicerId =  WritableObjects.readFirstLong(in, header);
            final long messageId = WritableObjects.readSecondLong(in, header);
            messageSliceId = new MessageSliceIdentifier(clientIdentifier, slicerId, messageId);
        }

        private Object readResolve() {
            return messageSliceId;
        }
    }
}
