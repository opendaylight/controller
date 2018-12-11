/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.client;

import akka.actor.ActorRef;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;
import javax.annotation.Nonnull;
import org.opendaylight.yangtools.concepts.Registration;

/**
 * Caching resolver which resolves a cookie to a leader {@link ActorRef}. This class needs to be specialized by the
 * client. It is used by {@link ClientActorBehavior} for request dispatch. Results are cached until they are invalidated
 * by either the client actor (when a message timeout is detected) and by the specific frontend (on explicit
 * invalidation or when updated information becomes available).
 *
 * <p>
 * If the completion stage returned by this interface's methods fails with a
 * {@link org.opendaylight.controller.cluster.access.concepts.RequestException}, it will be forwarded to all
 * outstanding requests towards the leader. If it fails with a {@link java.util.concurrent.TimeoutException},
 * resolution process will be retried. If it fails with any other cause, it will we wrapped as a
 * {@link org.opendaylight.controller.cluster.access.concepts.RuntimeRequestException} wrapping that cause.
 *
 * @author Robert Varga
 */
public abstract class BackendInfoResolver<T extends BackendInfo> implements AutoCloseable {
    /**
     * Request resolution of a particular backend identified by a cookie. This request can be satisfied from the cache.
     *
     * @param cookie Backend cookie
     * @return A {@link CompletionStage} resulting in information about the backend
     */
    @Nonnull
    public abstract CompletionStage<? extends T> getBackendInfo(@Nonnull Long cookie);

    /**
     * Request re-resolution of a particular backend identified by a cookie, indicating a particular information as
     * being stale. If the implementation's cache holds the stale information, it should be purged.
     *
     * @param cookie Backend cookie
     * @param staleInfo Stale backend information
     * @return A {@link CompletionStage} resulting in information about the backend
     */
    @Nonnull
    public abstract CompletionStage<? extends T> refreshBackendInfo(@Nonnull Long cookie, @Nonnull T staleInfo);

    /**
     * Registers a callback to be notified when BackendInfo that may have been previously obtained is now stale and
     * should be refreshed.
     *
     * @param callback the callback that takes the backend cookie whose BackendInfo is now stale.
     * @return a Registration
     */
    @Nonnull
    public abstract Registration notifyWhenBackendInfoIsStale(Consumer<Long> callback);

    @Nonnull
    public abstract String resolveCookieName(Long cookie);

    @Override
    public void close() {
    }
}
