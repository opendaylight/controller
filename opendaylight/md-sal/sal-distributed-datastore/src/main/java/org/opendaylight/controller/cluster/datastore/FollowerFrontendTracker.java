/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import akka.actor.ActorRef;
import com.google.common.base.Preconditions;
import org.opendaylight.controller.cluster.access.commands.FrontendRequest;
import org.opendaylight.controller.cluster.access.commands.NotLeaderException;
import org.opendaylight.controller.cluster.access.concepts.RequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class FollowerFrontendTracker extends AbstractFrontendTracker {
    private static final Logger LOG = LoggerFactory.getLogger(FollowerFrontendTracker.class);

    private final RequestException cause;
    private final String shardName;

    FollowerFrontendTracker(final ActorRef self, final String shardName) {
        this.shardName = Preconditions.checkNotNull(shardName);
        cause = new NotLeaderException(self + " is not the leader for shard " + shardName);
    }

    @Override
    void handleRequest(final FrontendRequest<?> request) {
        LOG.debug("Not a leader for shard {}, rejecting request {}", shardName, request);
        RequestUtil.sendFailure(request, cause);
    }
}
