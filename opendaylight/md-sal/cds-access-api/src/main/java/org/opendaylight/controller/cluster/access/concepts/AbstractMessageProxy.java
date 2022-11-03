/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.concepts;

import static com.google.common.base.Verify.verifyNotNull;

import java.io.DataInput;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serial;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.yangtools.concepts.WritableIdentifier;
import org.opendaylight.yangtools.concepts.WritableObjects;

/**
 * Abstract Externalizable proxy for use with {@link Message} subclasses.
 *
 * @author Robert Varga
 *
 * @param <T> Target identifier type
 * @param <C> Message class
 */
abstract class AbstractMessageProxy<T extends WritableIdentifier, C extends Message<T, C>> implements Externalizable {
    @Serial
    private static final long serialVersionUID = 1L;

    private T target;
    private long sequence;

    protected AbstractMessageProxy() {
        // For Externalizable
    }

    AbstractMessageProxy(final @NonNull C message) {
        this.target = message.getTarget();
        this.sequence = message.getSequence();
    }

    @Override
    public void writeExternal(final ObjectOutput out) throws IOException {
        target.writeTo(out);
        WritableObjects.writeLong(out, sequence);
    }

    @Override
    public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
        target = verifyNotNull(readTarget(in));
        sequence = WritableObjects.readLong(in);
    }

    @Serial
    protected final Object readResolve() {
        return verifyNotNull(createMessage(target, sequence));
    }

    protected abstract @NonNull T readTarget(@NonNull DataInput in) throws IOException;

    abstract @NonNull C createMessage(@NonNull T msgTarget, long msgSequence);
}
