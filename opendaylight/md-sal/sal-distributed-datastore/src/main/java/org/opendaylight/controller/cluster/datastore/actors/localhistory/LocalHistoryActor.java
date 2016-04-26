/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.actors.localhistory;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import com.google.common.base.Preconditions;
import org.opendaylight.controller.cluster.access.commands.CreateLocalHistoryRequest;
import org.opendaylight.controller.cluster.access.commands.CreateLocalHistorySuccess;
import org.opendaylight.controller.cluster.access.commands.DestroyLocalHistoryRequest;
import org.opendaylight.controller.cluster.access.commands.FrontendRequest;
import org.opendaylight.controller.cluster.access.commands.TransactionRequest;
import org.opendaylight.controller.cluster.access.concepts.LocalHistoryIdentifier;
import org.opendaylight.controller.cluster.datastore.utils.RequestUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class LocalHistoryActor extends UntypedActor {
    private static final Logger LOG = LoggerFactory.getLogger(LocalHistoryActor.class);
    private final LocalHistoryIdentifier historyId;
    private Behavior behavior = new IdleBehavior();

    private LocalHistoryActor(final LocalHistoryIdentifier historyId) {
        this.historyId = Preconditions.checkNotNull(historyId);
    }

    public static Props props(final LocalHistoryIdentifier historyId) {
        return Props.create(LocalHistoryActor.class, Preconditions.checkNotNull(historyId));
    }

    @Override
    public void onReceive(final Object command) {
        if (command instanceof FrontendRequest) {
            final FrontendRequest<?> request = (FrontendRequest<?>) command;
            if (RequestUtil.checkRequestFrontend(historyId.getFrontendId(), request, getContext().parent())) {
                return;
            }

            if (request instanceof CreateLocalHistoryRequest) {
                request.getReplyTo().tell(new CreateLocalHistorySuccess(historyId, getSelf()), ActorRef.noSender());
            } else if (request instanceof DestroyLocalHistoryRequest) {
                handleDestroyLocalHistory((DestroyLocalHistoryRequest)request);
            } else if (request instanceof TransactionRequest) {
                behavior = behavior.handleTransactionRequest((TransactionRequest) request);
            } else {
                LOG.warn("Ignoring unhandled request {}", request);
            }
        } else {
            LOG.warn("Ignoring command {}", command);
        }
    }

    private void handleDestroyLocalHistory(final DestroyLocalHistoryRequest request) {
        LOG.debug("Closing local history {}", historyId);

        // FIXME: cleanup state

        // TODO: does this really work? Check Shard's DeathWatch handling.
        getContext().stop(getSelf());
    }
}
