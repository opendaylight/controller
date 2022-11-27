/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.concepts;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serial;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.yangtools.concepts.WritableIdentifier;

/**
 * Abstract Externalizable proxy for use with {@link RequestFailure} subclasses.
 *
 * @param <T> Target identifier type
 */
public abstract class AbstractRequestFailureProxy<T extends WritableIdentifier, C extends RequestFailure<T, C>>
        extends AbstractResponseProxy<T, C> {
    @Serial
    private static final long serialVersionUID = 1L;

    protected AbstractRequestFailureProxy() {
        // For Externalizable
    }

    protected AbstractRequestFailureProxy(final @NonNull C failure) {
        super(failure);
    }

    @Override
    public C readExternal(final ObjectInput in, final T target, final long sequence)
            throws IOException, ClassNotFoundException {
        return createFailure(target, sequence, (RequestException) in.readObject());
    }

    @Override
    public final void writeExternal(final ObjectOutput out, final C msg) throws IOException {
        out.writeObject(msg.getCause());
    }

    protected abstract @NonNull C createFailure(@NonNull T target, long sequence,
            @NonNull RequestException failureCause);
}
