/*
 * Copyright (c) 2022 PANTHEON.tech, s.r.o. and others.  All rights reserved.
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
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.yangtools.concepts.WritableIdentifier;
import org.opendaylight.yangtools.concepts.WritableObjects;

/**
 * Abstract Externalizable proxy for use with {@link Message} subclasses.
 *
 * @param <T> Target identifier type
 * @param <C> Message class
 */
public interface MessageProxy<T extends WritableIdentifier, C extends Message<T, C>> extends Externalizable {

    @NonNull C message();

    void resolveTo(C newMessage);

    @Override
    default void writeExternal(final ObjectOutput out) throws IOException {
        final var message = message();
        message.getTarget().writeTo(out);
        WritableObjects.writeLong(out, message.getSequence());
        writeExternal(out, message);
    }

    void writeExternal(@NonNull ObjectOutput out, @NonNull C msg) throws IOException;

    @Override
    default void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
        final var target = verifyNotNull(readTarget(in));
        final var sequence = WritableObjects.readLong(in);
        resolveTo(verifyNotNull(readExternal(in, target, sequence)));
    }

    @NonNull C readExternal(@NonNull ObjectInput in, @NonNull T target, long sequence)
        throws IOException, ClassNotFoundException;

    Object readResolve();

    @NonNull T readTarget(@NonNull DataInput in) throws IOException;
}
