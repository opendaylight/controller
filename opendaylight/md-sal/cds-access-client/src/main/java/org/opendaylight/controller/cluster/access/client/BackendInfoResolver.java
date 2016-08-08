/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.client;

import akka.actor.ActorRef;
import com.google.common.base.Preconditions;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Caching resolver which resolves a cookie to a leader {@link ActorRef}. This class needs to be specialized by the
 * client. It is used by {@link ClientActorBehavior} for request dispatch. Results are cached until they are invalidated
 * by either the client actor (when a message timeout is detected) and by the specific frontend (on explicit
 * invalidation or when updated information becomes available).
 *
 * @author Robert Varga
 */
@ThreadSafe
public abstract class BackendInfoResolver<T extends BackendInfo> {
    private static final Logger LOG = LoggerFactory.getLogger(BackendInfoResolver.class);
    private final ConcurrentMap<Long, CompletableFuture<T>> backends = new ConcurrentHashMap<>();

    /**
     * Return the currently-resolved backend information, if available. This method is guaranteed not to block, but will
     * initiate resolution of the information if there is none.
     *
     * @param cookie Backend cookie
     * @return Backend information, if available
     */
    public final Optional<T> getFutureBackendInfo(final Long cookie) {
        final Future<T> f = lookupBackend(cookie);
        if (f.isDone()) {
            try {
                return Optional.of(f.get());
            } catch (InterruptedException | ExecutionException e) {
                LOG.debug("Resolution of {} failed", f, e);
            }
        }

        return Optional.empty();
    }

    /**
     * Invalidate a particular instance of {@link BackendInfo}, typically as a response to a request timing out. If
     * the provided information is not the one currently cached this method does nothing.
     *
     * @param cookie Backend cookie
     * @param info Previous information to be invalidated
     */
    public final void invalidateBackend(final long cookie, final @Nonnull CompletionStage<? extends BackendInfo> info) {
        if (backends.remove(cookie, Preconditions.checkNotNull(info))) {
            LOG.trace("Invalidated cache %s -> %s", Long.toUnsignedString(cookie), info);
            invalidateBackendInfo(info);
        }
    }

    /**
     * Request new resolution of a particular backend identified by a cookie. This method is invoked when a client
     * requests information which is not currently cached.
     *
     * @param cookie Backend cookie
     * @return A {@link CompletableFuture} resulting in information about the backend
     */
    protected abstract @Nonnull CompletableFuture<T> resolveBackendInfo(final @Nonnull Long cookie);

    /**
     * Invalidate previously-resolved shard information. This method is invoked when a timeout is detected
     * and the information may need to be refreshed.
     *
     * @param info Previous promise of backend information
     */
    protected abstract void invalidateBackendInfo(@Nonnull CompletionStage<? extends BackendInfo> info);

    // This is what the client needs to start processing. For as long as we do not have this, we should not complete
    // this stage until we have this information
    final CompletionStage<? extends T> getBackendInfo(final Long cookie) {
        return lookupBackend(cookie);
    }

    private CompletableFuture<T> lookupBackend(final Long cookie) {
        return backends.computeIfAbsent(Preconditions.checkNotNull(cookie), this::resolveBackendInfo);
    }
}
