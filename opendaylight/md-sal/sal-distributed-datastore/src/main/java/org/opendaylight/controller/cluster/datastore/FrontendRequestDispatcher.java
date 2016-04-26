/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import akka.actor.ActorRef;
import java.util.HashMap;
import java.util.Map;
import org.opendaylight.controller.cluster.access.commands.CreateLocalHistoryRequest;
import org.opendaylight.controller.cluster.access.commands.DestroyLocalHistoryRequest;
import org.opendaylight.controller.cluster.access.concepts.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class FrontendRequestDispatcher implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(FrontendRequestDispatcher.class);
    private final Map<Long, LocalHistoryActor> localHistories = new HashMap<>();
    private final long generation;
    private LocalHistoryActor lastActor;

    FrontendRequestDispatcher(final long generation) {
        this.generation = generation;
    }

    long getGeneration() {
        return generation;
    }

    private void handleCreateRequest(final CreateLocalHistoryRequest request) {
        final long historyId = request.getIdentifier().getHistoryId();
        final LocalHistoryActor actor = localHistories.get(historyId);


        if (actor == null) {
            // We do not have this history. Check if the local history ID in the request is greater than
            // the last known ID. If it is not, the history has been already destroyed.
            if (lastActor == null || Long.compareUnsigned(lastActor.getHistoryId(), historyId) >= 0) {
                // FIXME: add error response
                request.getFrontendRef().tell(null, ActorRef.noSender());
                return;
            }

        }


    }

    private void handleDestroyRequest(final DestroyLocalHistoryRequest request) {
        final LocalHistoryActor actor = localHistories.remove(request.getLocalHistoryIdentifier());
        if (actor != null) {
            // FIXME: destroy actor

            LOG.debug("Destroyed actor {} for local history {}", request.getLocalHistoryIdentifier());

        } else {
            LOG.info("Local history referenced by {} does not exist", request);
        }

        // FIXME: send success
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
