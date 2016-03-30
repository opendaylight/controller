/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import org.opendaylight.controller.cluster.datastore.messages.CommitTransaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract ShardBehavior for functionality shared between behaviors which forward messages to the leader or stash
 * them for later delivery once the leader is known.
 */
abstract class AbstractForwardingShardBehavior extends ShardBehavior {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractForwardingShardBehavior.class);

    AbstractForwardingShardBehavior(Shard shard) {
        super(shard);
    }

    private void forwardOrStash(final ActorRef sender, final Object message, final String comment) {
        final ActorSelection leader = getShard().getLeader();

        if (leader != null) {
            LOG.debug("{}: Forwarding {} to leader {}", persistenceId(), message.getClass().getSimpleName(), leader);
            leader.forward(message, getShard().getContext());
        } else {
            getShard().retryMessage(sender, message, comment);
        }
    }

    @Override
    final void handleCommitTransaction(ActorRef sender, CommitTransaction message) {
        forwardOrStash(sender, message, "Could not commit transaction " + message.getTransactionID());
    }
}
