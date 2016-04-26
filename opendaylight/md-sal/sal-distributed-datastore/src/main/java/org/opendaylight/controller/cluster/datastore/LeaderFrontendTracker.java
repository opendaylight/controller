/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import com.google.common.base.Preconditions;
import java.util.HashMap;
import java.util.Map;
import org.opendaylight.controller.cluster.access.commands.FrontendRequest;
import org.opendaylight.controller.cluster.access.commands.RetiredGenerationException;
import org.opendaylight.controller.cluster.access.concepts.FrontendIdentifier;
import org.opendaylight.controller.cluster.access.concepts.MemberName;
import org.opendaylight.controller.cluster.datastore.utils.RequestUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Dispatching logic for client requests coming from the frontend. Unless noted otherwise all methods may be invoked
 * only from the context of the Shard actor.
 */
final class LeaderFrontendTracker extends AbstractFrontendTracker {
    private static final Logger LOG = LoggerFactory.getLogger(LeaderFrontendTracker.class);
    private final Map<MemberName, FrontendRequestDispatcher> memberGenerations = new HashMap<>();
    private final Shard shard;

    /**
     * @param shard Parent Shard instance. Required only for state tracking purposes.
     *
     * FIXME: introduce ShardState, which holds the state information for current Shard.
     */
    LeaderFrontendTracker(final Shard shard) {
        this.shard = Preconditions.checkNotNull(shard);
    }

    /**
     * Handle a request from the frontend.
     * @param request
     */
    @Override
    void handleRequest(final FrontendRequest<?> request) {
        final FrontendIdentifier frontendId = request.getFrontendIdentifier();
        final FrontendRequestDispatcher existingDispatcher = memberGenerations.get(frontendId.getMemberName());

        if (existingDispatcher != null) {
            final long existingGeneration = existingDispatcher.getGeneration();
            final long requestGeneration = frontendId.getGeneration();

            // Matching generation: process the request
            if (existingGeneration == requestGeneration) {
                existingDispatcher.handleRequest(request);
                return;
            }

            // Request from an older generation: reject it
            if (Long.compareUnsigned(existingGeneration, requestGeneration) > 0) {
                LOG.debug("Rejecting request {}, retired by generation {}", request, existingGeneration);
                RequestUtil.sendFailure(request, new RetiredGenerationException(existingGeneration));
                return;
            }

            // Request from a newer generation: shutdown the dispatcher and remove it, we will treat the request
            // as a new generation.
            LOG.debug("Retiring dispatcher {} due to next-generation request {}", existingDispatcher, request);
            existingDispatcher.close();
            memberGenerations.remove(frontendId.getMemberName());
        }

        // We have either killed the dispatcher or we have not had one.
        final FrontendRequestDispatcher dispatcher = new FrontendRequestDispatcher(shard.getContext(),
            frontendId.getGeneration(), shard.getDataStore().getDataTree());
        memberGenerations.put(frontendId.getMemberName(), dispatcher);
        LOG.debug("Instantiated dispatcher {} for frontend {}", dispatcher, frontendId);
        dispatcher.handleRequest(request);
    }
}
