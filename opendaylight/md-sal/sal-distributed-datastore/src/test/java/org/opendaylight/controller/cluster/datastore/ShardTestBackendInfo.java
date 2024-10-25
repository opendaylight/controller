/*
 * Copyright (c) 2024 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import org.apache.pekko.testkit.TestActorRef;
import org.opendaylight.controller.cluster.access.ABIVersion;
import org.opendaylight.controller.cluster.access.client.BackendInfo;

final class ShardTestBackendInfo extends BackendInfo {
    // Maximum number of messages in the queue before it starts to delay
    private static final int TEST_MAX_MESSAGES = 30;

    private final Shard shard;

    ShardTestBackendInfo(final TestActorRef<Shard> shard) {
        // Note:
        super(shard, shard.toString(), 0, ABIVersion.current(), TEST_MAX_MESSAGES);
        this.shard = shard.underlyingActor();
    }

    Shard shard() {
        return shard;
    }
}
