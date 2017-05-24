/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.client.messages;

import akka.dispatch.ControlMessage;
import java.io.Serializable;
import org.opendaylight.controller.cluster.raft.base.messages.EmptyExternalizableProxy;

/**
 * Message sent to a raft actor to shutdown gracefully. If it's the leader it will transfer leadership to a
 * follower. As its last act, the actor self-destructs via a PoisonPill. This message should only be used with
 * Patterns.gracefulStop().
 *
 * @author Thomas Pantelis
 */
public final class Shutdown implements Serializable, ControlMessage {
    private static final long serialVersionUID = 1L;
    public static final Shutdown INSTANCE = new Shutdown();

    private Shutdown() {
        // Hidden on purpose
    }

    private Object writeReplace() {
        return new Proxy();
    }

    private static class Proxy extends EmptyExternalizableProxy {
        private static final long serialVersionUID = 1L;

        // checkstyle flags the public modifier as redundant which really doesn't make sense since it clearly isn't
        // redundant. It is explicitly needed for Java serialization to be able to create instances via reflection.
        @SuppressWarnings("checkstyle:RedundantModifier")
        public Proxy() {
            super(INSTANCE);
        }
    }
}
