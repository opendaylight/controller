/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.concepts;

import com.google.common.annotations.Beta;
import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;
import com.google.common.base.Preconditions;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;
import org.opendaylight.yangtools.concepts.Identifiable;
import org.opendaylight.yangtools.concepts.Identifier;

@Beta
public abstract class Message<T extends Identifier, C extends Message<T, C>> implements Identifiable<T>, Serializable {
    protected static abstract class AbstractProxy<T extends Identifier, C extends Message<T, C>> implements Externalizable {
        private static final long serialVersionUID = 1L;
        private T identifier;

        public AbstractProxy() {
            // For Externalizable
        }

        protected AbstractProxy(final T identifier) {
            this.identifier = Preconditions.checkNotNull(identifier);
        }

        protected final T getIdentifier() {
            return identifier;
        }

        @Override
        public void writeExternal(final ObjectOutput out) throws IOException {
            out.writeObject(identifier);
        }

        @SuppressWarnings("unchecked")
        @Override
        public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
            identifier = (T) in.readObject();
        }

        protected abstract Message<?, ?> readResolve();
    }

    private static final long serialVersionUID = 1L;
    private final T identifier;

    Message(final T identifier) {
        // Hidden to force use of subclasses
        this.identifier = Preconditions.checkNotNull(identifier);
    }

    @Override
    public final T getIdentifier() {
        return identifier;
    }

    @Override
    public final String toString() {
        return addToStringAttributes(MoreObjects.toStringHelper(this).omitNullValues()).toString();
    }

    protected ToStringHelper addToStringAttributes(final ToStringHelper toStringHelper) {
        return toStringHelper.add("identifier", getIdentifier());
    }

    protected abstract AbstractProxy<T, C> writeReplace();
}
