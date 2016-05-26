/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.concepts;

import java.io.DataInput;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import org.opendaylight.yangtools.concepts.Identifier;

/**
 * Abstract Externalizable proxy for use with {@link Message} subclasses.
 *
 * @author Robert Varga
 *
 * @param <T> Target identifier type
 * @param <C> Message class
 */
abstract class AbstractMessageProxy<T extends Identifier & WritableObject, C extends Message<T, C>>
        implements Externalizable {
    private static final long serialVersionUID = 1L;
    private T target;
    private long sequence;

    public AbstractMessageProxy() {
        // For Externalizable
    }

    AbstractMessageProxy(final Message<T, C> message) {
        this.target = message.getTarget();
        this.sequence = message.getSequence();
    }

    @Override
    public void writeExternal(final ObjectOutput out) throws IOException {
        target.writeTo(out);
        out.writeLong(sequence);
    }

    @Override
    public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
        target = readTarget(in);
        sequence = in.readLong();
    }

    protected final Object readResolve() {
        return createMessage(target, sequence);
    }

    protected abstract T readTarget(DataInput in) throws IOException;
    abstract Message<T, C> createMessage(T target, long sequence);
}