/*
 * Copyright (c) 2017 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.messaging;

import com.google.common.base.Preconditions;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;
import org.opendaylight.yangtools.concepts.Identifier;

/**
 * Message sent to abort slicing.
 *
 * @author Thomas Pantelis
 */
class AbortSlicing implements Serializable {
    private static final long serialVersionUID = 1L;

    private final Identifier identifier;

    AbortSlicing(final Identifier identifier) {
        this.identifier = Preconditions.checkNotNull(identifier);
    }

    Identifier getIdentifier() {
        return identifier;
    }

    @Override
    public String toString() {
        return "AbortSlicing [identifier=" + identifier + "]";
    }

    private Object writeReplace() {
        return new Proxy(this);
    }

    private static class Proxy implements Externalizable {
        private static final long serialVersionUID = 1L;

        private AbortSlicing abortSlicing;

        // checkstyle flags the public modifier as redundant which really doesn't make sense since it clearly isn't
        // redundant. It is explicitly needed for Java serialization to be able to create instances via reflection.
        @SuppressWarnings("checkstyle:RedundantModifier")
        public Proxy() {
        }

        Proxy(AbortSlicing abortSlicing) {
            this.abortSlicing = abortSlicing;
        }

        @Override
        public void writeExternal(ObjectOutput out) throws IOException {
            out.writeObject(abortSlicing.identifier);
        }

        @Override
        public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
            abortSlicing = new AbortSlicing((Identifier) in.readObject());
        }

        private Object readResolve() {
            return abortSlicing;
        }
    }
}
