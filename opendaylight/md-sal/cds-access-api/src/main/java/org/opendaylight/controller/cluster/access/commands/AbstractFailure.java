/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.commands;

import com.google.common.base.MoreObjects.ToStringHelper;
import com.google.common.base.Preconditions;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import org.opendaylight.controller.cluster.access.concepts.Failure;
import org.opendaylight.yangtools.concepts.Identifier;

abstract class AbstractFailure<T extends Identifier> extends AbstractMessage<T, Failure<T>> implements Failure<T> {
    static abstract class AbstractFailureProxy<T extends Identifier> extends AbstractProxy<T, Failure<T>> {
        private static final long serialVersionUID = 1L;
        private Exception cause;

        public AbstractFailureProxy() {
            // For Externalizable
        }

        AbstractFailureProxy(final T identifier, final Exception cause) {
            super(identifier);
            this.cause = Preconditions.checkNotNull(cause);
        }

        final Exception getCause() {
            return cause;
        }

        @Override
        public void writeExternal(final ObjectOutput out) throws IOException {
            super.writeExternal(out);
            out.writeObject(cause);
        }

        @Override
        public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
            super.readExternal(in);
            cause = (Exception) in.readObject();
        }

        @Override
        abstract AbstractFailure<?> readResolve();
    }

    private static final long serialVersionUID = 1L;
    private final Exception cause;

    AbstractFailure(final T identifier, final Exception cause) {
        super(identifier);
        this.cause = Preconditions.checkNotNull(cause);
    }

    @Override
    public final Exception getCause() {
        return cause;
    }

    @Override
    ToStringHelper addToStringAttributes(final ToStringHelper toStringHelper) {
        return super.addToStringAttributes(toStringHelper).add("cause", cause);
    }

    @Override
    abstract AbstractFailureProxy<T> writeReplace();
}
