/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.base.messages;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;

/**
 * Message sent to a follower to force an immediate election time out.
 *
 * @author Thomas Pantelis
 */
public final class TimeoutNow implements Serializable {
    private static final long serialVersionUID = 1L;
    public static final TimeoutNow INSTANCE = new TimeoutNow();

    private TimeoutNow() {
        // Hidden on purpose
    }

    private Object writeReplace() {
        return new Proxy();
    }

    private static class Proxy implements Externalizable {
        private static final long serialVersionUID = 1L;

        // checkstyle flags the public modifier as redundant which really doesn't make sense since it clearly isn't
        // redundant. It is explicitly needed for Java serialization to be able to create instances via reflection.
        @SuppressWarnings("checkstyle:RedundantModifier")
        public Proxy() {
        }

        @Override
        public void writeExternal(ObjectOutput out) throws IOException {
        }

        @Override
        public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        }

        private Object readResolve() {
            return INSTANCE;
        }
    }
}
