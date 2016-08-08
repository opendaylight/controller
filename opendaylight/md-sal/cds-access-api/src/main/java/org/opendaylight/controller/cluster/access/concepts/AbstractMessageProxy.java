/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.concepts;

import com.google.common.base.Verify;
import java.io.DataInput;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import javax.annotation.Nonnull;
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
    private static final long serialVersionUID = 1L;
    private T target;
    private long sequence;

    protected AbstractMessageProxy() {
        // For Externalizable
    }

    AbstractMessageProxy(final @Nonnull C message) {
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
        target = Verify.verifyNotNull(readTarget(in));
        sequence = WritableObjects.readLong(in);
    }

    protected final Object readResolve() {
        return Verify.verifyNotNull(createMessage(target, sequence));
    }

    protected abstract @Nonnull T readTarget(@Nonnull DataInput in) throws IOException;
    abstract @Nonnull C createMessage(@Nonnull T target, long sequence);
}