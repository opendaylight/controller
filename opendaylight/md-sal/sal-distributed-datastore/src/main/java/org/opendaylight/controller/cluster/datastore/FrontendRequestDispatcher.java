/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import akka.actor.ActorContext;
import akka.actor.ActorRef;
import com.google.common.base.Preconditions;
import java.util.HashMap;
import java.util.Map;
import org.opendaylight.controller.cluster.access.commands.CreateLocalHistoryRequest;
import org.opendaylight.controller.cluster.access.commands.CreateLocalHistorySuccess;
import org.opendaylight.controller.cluster.access.commands.DeadHistoryException;
import org.opendaylight.controller.cluster.access.commands.DestroyLocalHistoryRequest;
import org.opendaylight.controller.cluster.access.commands.DestroyLocalHistoryResponse;
import org.opendaylight.controller.cluster.access.concepts.Request;
import org.opendaylight.controller.cluster.datastore.actors.localhistory.LocalHistoryActor;
import org.opendaylight.controller.cluster.datastore.utils.RequestUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class FrontendRequestDispatcher implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(FrontendRequestDispatcher.class);
    private final Map<Long, ActorRef> localHistories = new HashMap<>();
    private final ActorContext actorContext;
    private final long generation;
    private Long lastHistoryId;

    FrontendRequestDispatcher(final ActorContext actorContext, final long generation) {
        this.actorContext = Preconditions.checkNotNull(actorContext);
        this.generation = generation;
    }

    long getGeneration() {
        return generation;
    }

    private static void sendSuccess(final CreateLocalHistoryRequest request, final ActorRef actor) {
        request.getReplyTo().tell(new CreateLocalHistorySuccess(request.getLocalHistoryIdentifier(), actor),
            ActorRef.noSender());
    }

    private void handleCreateRequest(final CreateLocalHistoryRequest request) {
        final long historyId = request.getIdentifier().getHistoryId();
        final ActorRef existingActor = localHistories.get(historyId);
        if (existingActor != null) {
            // FIXME: persist a birth marker
            sendSuccess(request, existingActor);
            return;
        }

        // We do not have this history. Check if the local history ID in the request is greater than
        // the last known ID. If it is not, the history has been already destroyed.
        if (lastHistoryId != null && Long.compareUnsigned(lastHistoryId, historyId) >= 0) {
            RequestUtil.sendFailure(request, new DeadHistoryException(historyId));
            return;
        }

        final ActorRef actor = actorContext.watch(actorContext.actorOf(
            LocalHistoryActor.props(request.getLocalHistoryIdentifier())));
        sendSuccess(request, actor);
    }

    private void handleDestroyRequest(final DestroyLocalHistoryRequest request) {
        final ActorRef actor = localHistories.remove(request.getLocalHistoryIdentifier());
        if (actor != null) {
            actorContext.stop(actor);
            // FIXME: persist a tombstone
            LOG.debug("Destroyed actor {} for local history {}", request.getLocalHistoryIdentifier());
        } else {
            LOG.info("Local history referenced by {} does not exist", request);
        }

        request.getReplyTo().tell(new DestroyLocalHistoryResponse(request.getIdentifier()), ActorRef.noSender());
    }

    void handleRequest(final Request<?> request) {
        if (request instanceof CreateLocalHistoryRequest) {
            handleCreateRequest((CreateLocalHistoryRequest) request);
        } else if (request instanceof DestroyLocalHistoryRequest) {
            handleDestroyRequest((DestroyLocalHistoryRequest) request);
        } else {
            LOG.warn("Rejecting unhandled request {}", request);
        }
    }

    @Override
    public void close() {
        // TODO Auto-generated method stub

    }
}
