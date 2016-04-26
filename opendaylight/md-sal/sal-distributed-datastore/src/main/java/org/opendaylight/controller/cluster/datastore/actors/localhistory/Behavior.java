/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.actors.localhistory;

import akka.actor.ActorRef;
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

abstract class Behavior {
    private static final Logger LOG = LoggerFactory.getLogger(Behavior.class);
    private final LocalHistoryContext context;

    Behavior(final LocalHistoryContext context) {
        this.context = Preconditions.checkNotNull(context);
    }

    final LocalHistoryContext getContext() {
        return context;
    }

    private LocalHistoryIdentifier historyId() {
        return context.getHistoryId();
    }

    final Behavior handleCommand(final Object command) {
        if (command instanceof FrontendRequest) {
            final FrontendRequest<?> request = (FrontendRequest<?>) command;
            if (RequestUtil.checkRequestFrontend(historyId().getFrontendId(), request, getContext().parent())) {
                return this;
            }

            if (request instanceof CreateLocalHistoryRequest) {
                request.getReplyTo().tell(new CreateLocalHistorySuccess(historyId(), context.getSelf()), ActorRef.noSender());
            } else if (request instanceof DestroyLocalHistoryRequest) {
                handleDestroyLocalHistory((DestroyLocalHistoryRequest)request);
            } else if (request instanceof TransactionRequest) {
                return handleTransactionRequest((TransactionRequest) request);
            } else {
                LOG.warn("Ignoring unhandled request {}", request);
            }
        } else {
            LOG.warn("Ignoring command {}", command);
        }

        return this;
    }

    abstract Behavior handleTransactionRequest(TransactionRequest request);


    private void handleDestroyLocalHistory(final DestroyLocalHistoryRequest request) {
        LOG.debug("Closing local history {}", historyId());

        // FIXME: cleanup state

        // TODO: does this really work? Check Shard's DeathWatch handling.
    }

}