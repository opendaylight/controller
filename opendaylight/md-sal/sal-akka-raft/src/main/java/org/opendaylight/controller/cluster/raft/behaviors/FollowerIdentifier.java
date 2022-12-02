/*
 * Copyright (c) 2017 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.behaviors;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import org.opendaylight.yangtools.util.AbstractStringIdentifier;

/**
 * An Identifier for a follower.
 *
 * @author Thomas Pantelis
 */
class FollowerIdentifier extends AbstractStringIdentifier<FollowerIdentifier> {
    @java.io.Serial
    private static final long serialVersionUID = 1L;

    FollowerIdentifier(final String followerId) {
        super(followerId);
    }

    @java.io.Serial
    private Object writeReplace() {
        return new FI(getValue());
    }

    @Deprecated(since = "7.0.0", forRemoval = true)
    private static class Proxy implements Externalizable {
        @java.io.Serial
        private static final long serialVersionUID = 1L;

        private FollowerIdentifier identifier;

        // checkstyle flags the public modifier as redundant which really doesn't make sense since it clearly isn't
        // redundant. It is explicitly needed for Java serialization to be able to create instances via reflection.
        @SuppressWarnings("checkstyle:RedundantModifier")
        public Proxy() {
        }

        Proxy(final FollowerIdentifier identifier) {
            this.identifier = identifier;
        }

        @Override
        public void writeExternal(final ObjectOutput out) throws IOException {
            out.writeObject(identifier.getValue());
        }

        @Override
        public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
            identifier = new FollowerIdentifier((String) in.readObject());
        }

        @java.io.Serial
        private Object readResolve() {
            return identifier;
        }
    }
}
