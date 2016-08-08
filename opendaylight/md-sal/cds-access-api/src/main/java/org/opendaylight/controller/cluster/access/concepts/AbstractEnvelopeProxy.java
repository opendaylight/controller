/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.concepts;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import org.opendaylight.yangtools.concepts.WritableObjects;

abstract class AbstractEnvelopeProxy<T extends Message<?, ?>> implements Externalizable {
    private static final long serialVersionUID = 1L;

    private T message;
    private long sessionId;
    private long txSequence;

    public AbstractEnvelopeProxy() {
        // for Externalizable
    }

    AbstractEnvelopeProxy(final Envelope<T> envelope) {
        message = envelope.getMessage();
        txSequence = envelope.getTxSequence();
        sessionId = envelope.getSessionId();
    }

    @Override
    public final void writeExternal(final ObjectOutput out) throws IOException {
        WritableObjects.writeLongs(out, sessionId, txSequence);
        out.writeObject(message);
    }

    @SuppressWarnings("unchecked")
    @Override
    public final void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
        final byte header = WritableObjects.readLongHeader(in);
        sessionId = WritableObjects.readFirstLong(in, header);
        txSequence = WritableObjects.readSecondLong(in, header);
        message = (T) in.readObject();
    }

    abstract Envelope<T> createEnvelope(T message, long sessionId, long txSequence);

    final Object readResolve() {
        return createEnvelope(message, sessionId, txSequence);
    }
}