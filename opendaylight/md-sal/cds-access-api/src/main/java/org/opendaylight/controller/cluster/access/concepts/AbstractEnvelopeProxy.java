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
    private long sequence;
    private long retry;

    public AbstractEnvelopeProxy() {
        // for Externalizable
    }

    AbstractEnvelopeProxy(final Envelope<T> envelope) {
        message = envelope.getMessage();
        sequence = envelope.getSequence();
        retry = envelope.getRetry();
    }

    @Override
    public final void writeExternal(final ObjectOutput out) throws IOException {
        WritableObjects.writeLongs(out, sequence, retry);
        out.writeObject(message);
    }

    @SuppressWarnings("unchecked")
    @Override
    public final void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
        final byte header = WritableObjects.readLongHeader(in);
        sequence = WritableObjects.readFirstLong(in, header);
        retry = WritableObjects.readSecondLong(in, header);
        message = (T) in.readObject();
    }

    abstract Envelope<T> createEnvelope(T message, long sequence, long retry);

    final Object readResolve() {
        return createEnvelope(message, sequence, retry);
    }
}