/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.actors.localhistory;

import akka.actor.ActorRef;
import java.util.Collection;
import java.util.HashSet;
import org.opendaylight.controller.cluster.access.commands.DeadHistoryException;
import org.opendaylight.controller.cluster.access.commands.DestroyLocalHistoryRequest;
import org.opendaylight.controller.cluster.access.commands.DestroyLocalHistoryResponse;
import org.opendaylight.controller.cluster.access.commands.TransactionRequest;
import org.opendaylight.controller.cluster.access.concepts.LocalHistoryIdentifier;
import org.opendaylight.controller.cluster.datastore.utils.RequestUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class ClosedBehavior extends IdleBehavior {
    private static final Logger LOG = LoggerFactory.getLogger(ClosedBehavior.class);
    private final Collection<ActorRef> notifyActors = new HashSet<>(2);

    ClosedBehavior(final LocalHistoryContext context) {
        super(context);
    }

    @Override
    Behavior createTransaction(final TransactionRequest request) {
        final LocalHistoryIdentifier history = getContext().getHistoryId();
        LOG.warn("Attempted to create transaction {} on closed history {}", request, history);
        RequestUtil.sendFailure(request, new DeadHistoryException(history.getHistoryId()));
        return this;
    }

    @Override
    Behavior handleDestroyLocalHistory(final DestroyLocalHistoryRequest request) {
        notifyActors.add(request.getReplyTo());
        return checkShutdown();
    }

    @Override
    Behavior handleTransactionCommitResponse(final TransactionCommitSuccess command) {
        super.handleTransactionCommitResponse(command);
        return checkShutdown();
    }

    private Behavior checkShutdown() {
        if (getContext().isEmpty()) {
            final DestroyLocalHistoryResponse message = new DestroyLocalHistoryResponse(getContext().getHistoryId());
            for (ActorRef actor : notifyActors) {
                actor.tell(message, ActorRef.noSender());
            }
            return null;
        } else {
            return this;
        }
    }
}
