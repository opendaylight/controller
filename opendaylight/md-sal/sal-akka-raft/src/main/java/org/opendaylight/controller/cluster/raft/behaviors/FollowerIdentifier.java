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
import org.opendaylight.controller.cluster.messaging.StringIdentifier;
import org.opendaylight.yangtools.util.AbstractStringIdentifier;

/**
 * An Identifier for a follower.
 *
 * @author Thomas Pantelis
 */
class FollowerIdentifier extends AbstractStringIdentifier<StringIdentifier> {
    private static final long serialVersionUID = 1L;

    FollowerIdentifier(String followerId) {
        super(followerId);
    }

    private Object writeReplace() {
        return new Proxy(this);
    }

    private static class Proxy implements Externalizable {
        private static final long serialVersionUID = 1L;

        private FollowerIdentifier identifier;

        // checkstyle flags the public modifier as redundant which really doesn't make sense since it clearly isn't
        // redundant. It is explicitly needed for Java serialization to be able to create instances via reflection.
        @SuppressWarnings("checkstyle:RedundantModifier")
        public Proxy() {
        }

        Proxy(FollowerIdentifier identifier) {
            this.identifier = identifier;
        }

        @Override
        public void writeExternal(ObjectOutput out) throws IOException {
            out.writeObject(identifier.getValue());
        }

        @Override
        public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
            identifier = new FollowerIdentifier((String) in.readObject());
        }

        private Object readResolve() {
            return identifier;
        }
    }
}
