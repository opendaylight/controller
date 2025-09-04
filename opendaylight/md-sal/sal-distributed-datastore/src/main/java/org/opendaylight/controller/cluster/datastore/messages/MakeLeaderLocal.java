/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.messages;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Message sent to local shard to try to gain shard leadership. Sender of this message will be notified about result of
 * leadership transfer with {@link org.apache.pekko.actor.Status.Success}, if leadership is successfully transferred
 * to local shard. Otherwise {@link org.apache.pekko.actor.Status.Failure} with
 * {@link org.opendaylight.controller.cluster.raft.LeadershipTransferFailedException} will be sent to sender of this
 * message.
 */
// FIXME: with replyTo
@NonNullByDefault
public final class MakeLeaderLocal {
    public static final MakeLeaderLocal INSTANCE = new MakeLeaderLocal();

    private MakeLeaderLocal() {
        // Hidden on purpose
    }
}
