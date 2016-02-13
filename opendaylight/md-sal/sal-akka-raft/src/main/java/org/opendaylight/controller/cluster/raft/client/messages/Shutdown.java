/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.client.messages;

import java.io.Serializable;

/**
 * Message sent to a raft actor to shutdown gracefully. If it's the leader it will transfer leadership to a
 * follower. As its last act, the actor self-destructs via a PoisonPill.
 *
 * @author Thomas Pantelis
 */
public final class Shutdown implements Serializable {
    private static final long serialVersionUID = 1L;
    public static final Shutdown INSTANCE = new Shutdown();

    private Shutdown() {
        // Hidden on purpose
    }

    @SuppressWarnings({ "static-method", "unused" })
    private Shutdown readResolve() {
        return INSTANCE;
    }
}
