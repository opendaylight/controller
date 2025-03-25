/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.client.messages;

import java.io.Serializable;
import org.apache.pekko.dispatch.ControlMessage;

/**
 * Message sent to a raft actor to shutdown gracefully. If it's the leader it will transfer leadership to a
 * follower. As its last act, the actor self-destructs via a PoisonPill. This message should only be used with
 * Patterns.gracefulStop().
 *
 * @author Thomas Pantelis
 */
public final class Shutdown implements Serializable, ControlMessage {
    @java.io.Serial
    private static final long serialVersionUID = 1L;

    public static final Shutdown INSTANCE = new Shutdown();

    private Shutdown() {
        // Hidden on purpose
    }

    @java.io.Serial
    @SuppressWarnings("static-method")
    private Object readResolve() {
        return INSTANCE;
    }
}
