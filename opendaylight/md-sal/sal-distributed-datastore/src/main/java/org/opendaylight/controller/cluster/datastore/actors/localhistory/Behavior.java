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
import org.opendaylight.controller.cluster.datastore.actors.messages.PersistTransactionSuccess;
import org.opendaylight.controller.cluster.datastore.actors.messages.PersistTransactionUpdate;
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

    final LocalHistoryIdentifier historyId() {
        return context.getHistoryId();
    }

    final Behavior handleCommand(final Object command) {
        if (command instanceof FrontendRequest) {
            final FrontendRequest<?> request = (FrontendRequest<?>) command;
            if (RequestUtil.checkRequestFrontend(historyId().getFrontendId(), request, getContext().getParent())) {
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
        } else if (command instanceof PersistTransactionUpdate) {
            return handlePersistTransactionUpdate((PersistTransactionUpdate) command);
        } else if (command instanceof PersistTransactionSuccess) {
            return handlePersistTransactionSuccess((PersistTransactionSuccess)command);
        } else {
            LOG.warn("Ignoring unknown command {}", command);
        }

        return this;
    }

    private Behavior handlePersistTransactionUpdate(final PersistTransactionUpdate command) {
        context.updateTransaction(command.getMessage());
        return this;
    }

    Behavior handlePersistTransactionSuccess(final PersistTransactionSuccess command) {
        context.releaseTransaction(command.getIdentifier().getTransactionId().getTransactionId());
        return this;
    }

    abstract Behavior handleDestroyLocalHistory(DestroyLocalHistoryRequest request);
    abstract Behavior handleTransactionRequest(TransactionRequest request);
}
