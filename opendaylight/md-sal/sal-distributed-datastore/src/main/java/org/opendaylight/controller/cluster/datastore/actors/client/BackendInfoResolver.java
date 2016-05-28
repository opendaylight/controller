/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.actors.client;

import akka.actor.ActorRef;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.annotation.concurrent.ThreadSafe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Caching resolver which resolves a cookie to a leader {@link ActorRef}. This class needs to be specialized by the
 * client. It is used by {@link ClientActorBehavior} for request dispatch. Results are cached until they are invalidated
 * by either the client actor (when a message timeout is detected) and by the specific frontend (on explicit shootdown
 * or when updated information becomes available.
 *
 * @author Robert Varga
 */
@ThreadSafe
public abstract class BackendInfoResolver<T extends BackendInfo> {
    private static final Logger LOG = LoggerFactory.getLogger(BackendInfoResolver.class);
    private final ConcurrentMap<Long, CompletionStage<T>> backends = new ConcurrentHashMap<>();

    protected abstract CompletionStage<T> resolveBackendInfo(final Long cookie);
    protected abstract void invalidateBackendInfo(CompletionStage<? extends BackendInfo> info);

    // This is what the client needs to start processing. For as long as we do not have this, we should not complete
    // this stage until we have this information
    public final CompletionStage<? extends T> getBackendInfo(final long cookie) {
        return backends.computeIfAbsent(cookie, this::resolveBackendInfo);
    }

    // Invalidation callback. Further calls to getBackendInfo should return a new CompletionStage, which will be the
    // fresh information. If the provided info does not match, no action should be taken.
    public final void invalidateBackend(final long cookie, final CompletionStage<? extends BackendInfo> info) {
        if (backends.remove(cookie, info)) {
            LOG.trace("Invalidated cache %s -> %s", Long.toUnsignedString(cookie), info);
            invalidateBackendInfo(info);
        }

    }

}
