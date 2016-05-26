/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.concepts;

import com.google.common.annotations.Beta;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import org.opendaylight.yangtools.concepts.Identifier;

/**
 * Abstract Externalizable proxy for use with {@link RequestFailure} subclasses.
 *
 * @author Robert Varga
 *
 * @param <T> Target identifier type
 */
@Beta
public abstract class AbstractRequestFailureProxy<T extends Identifier & WritableObject> extends
        AbstractResponseProxy<T, RequestFailure<T>> {
    private static final long serialVersionUID = 1L;
    private RequestException cause;

    public AbstractRequestFailureProxy() {
        // For Externalizable
    }

    protected AbstractRequestFailureProxy(final RequestFailure<T> failure) {
        super(failure);
        this.cause = failure.getCause();
    }

    @Override
    public void writeExternal(final ObjectOutput out) throws IOException {
        super.writeExternal(out);
        out.writeObject(cause);
    }

    @Override
    public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
        super.readExternal(in);
        cause = (RequestException) in.readObject();
    }

    @Override
    final RequestFailure<T> createResponse(final T target, final long sequence) {
        return createFailure(target, sequence, cause);
    }

    protected abstract RequestFailure<T> createFailure(T target, long sequence, RequestException cause);
}