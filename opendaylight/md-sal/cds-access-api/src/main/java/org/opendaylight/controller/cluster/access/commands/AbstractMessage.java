/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.commands;

import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;
import com.google.common.base.Preconditions;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import org.opendaylight.controller.cluster.access.concepts.Message;
import org.opendaylight.yangtools.concepts.Identifier;

abstract class AbstractMessage<T extends Identifier, C extends Message<T, C>> implements Message<T, C> {
    static abstract class AbstractProxy<T extends Identifier, C extends Message<T, C>> implements Externalizable {
        private static final long serialVersionUID = 1L;
        private T identifier;

        public AbstractProxy() {
            // For Externalizable
        }

        AbstractProxy(final T historyId) {
            this.identifier = Preconditions.checkNotNull(historyId);
        }

        final T getHistoryId() {
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

        abstract AbstractMessage<?, ?> readResolve();
    }

    private static final long serialVersionUID = 1L;
    private final T identifier;

    AbstractMessage(final T identifier) {
        this.identifier = Preconditions.checkNotNull(identifier);
    }

    @Override
    public final T getIdentifier() {
        return identifier;
    }

    @Override
    public final String toString() {
        return addToStringAttributes(MoreObjects.toStringHelper(this)).toString();
    }

    ToStringHelper addToStringAttributes(final ToStringHelper toStringHelper) {
        return toStringHelper.add("identifier", getIdentifier());
    }

    abstract AbstractProxy<T, C> writeReplace();
}
