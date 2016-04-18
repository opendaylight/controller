/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.concepts;

import com.google.common.base.MoreObjects.ToStringHelper;
import com.google.common.base.Preconditions;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import org.opendaylight.yangtools.concepts.Identifier;

public final class RequestFailure<T extends Identifier> extends Response<T, RequestFailure<T>> {
    private static final class Proxy<T extends Identifier> extends AbstractProxy<T, RequestFailure<T>> {
        private static final long serialVersionUID = 1L;
        private RequestException cause;

        public Proxy() {
            // For Externalizable
        }

        Proxy(final T identifier, final RequestException cause) {
            super(identifier);
            this.cause = Preconditions.checkNotNull(cause);
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
        protected RequestFailure<T> readResolve() {
            return new RequestFailure<>(getIdentifier(), cause);
        }
    }

    private static final long serialVersionUID = 1L;
    private final RequestException cause;

    public RequestFailure(final T identifier, final RequestException cause) {
        super(identifier);
        this.cause = Preconditions.checkNotNull(cause);
    }

    public RequestException getCause() {
        return cause;
    }

    @Override
    protected ToStringHelper addToStringAttributes(final ToStringHelper toStringHelper) {
        return super.addToStringAttributes(toStringHelper).add("cause", cause);
    }

    @Override
    protected Proxy<T> writeReplace() {
        return new Proxy<>(getIdentifier(), cause);
    }
}
