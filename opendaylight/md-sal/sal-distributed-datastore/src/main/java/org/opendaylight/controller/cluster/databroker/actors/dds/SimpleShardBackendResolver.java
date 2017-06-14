/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker.actors.dds;

import com.google.common.base.Preconditions;
import java.util.concurrent.CompletionStage;
import javax.annotation.concurrent.ThreadSafe;
import org.opendaylight.controller.cluster.access.client.BackendInfoResolver;
import org.opendaylight.controller.cluster.access.concepts.ClientIdentifier;
import org.opendaylight.controller.cluster.datastore.utils.ActorContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link BackendInfoResolver} implementation for static shard configuration based on ShardManager. Unlike the full
 * {@link ModuleShardBackendResolver}, this resolver is used in situations where the client corresponds exactly to one
 * backend shard, e.g. there is only one fixed cookie assigned and the operation path is not consulted at all.
 *
 * @author Robert Varga
 */
@ThreadSafe
final class SimpleShardBackendResolver extends AbstractShardBackendResolver {
    private static final Logger LOG = LoggerFactory.getLogger(SimpleShardBackendResolver.class);

    private final String shardName;

    private volatile ShardState state;

    // FIXME: we really need just ActorContext.findPrimaryShardAsync()
    SimpleShardBackendResolver(final ClientIdentifier clientId, final ActorContext actorContext,
            final String shardName) {
        super(clientId, actorContext);
        this.shardName = Preconditions.checkNotNull(shardName);
    }

    private CompletionStage<ShardBackendInfo> getBackendInfo(final long cookie) {
        Preconditions.checkArgument(cookie == 0);

        final ShardState existing = state;
        if (existing != null) {
            return existing.getStage();
        }

        synchronized (this) {
            final ShardState recheck = state;
            if (recheck != null) {
                return recheck.getStage();
            }

            final ShardState newState = resolveBackendInfo(shardName, 0);
            state = newState;

            final CompletionStage<ShardBackendInfo> stage = newState.getStage();
            stage.whenComplete((info, failure) -> {
                if (failure != null) {
                    synchronized (SimpleShardBackendResolver.this) {
                        if (state == newState) {
                            state = null;
                        }
                    }
                }
            });

            return stage;
        }
    }

    @Override
    public CompletionStage<ShardBackendInfo> getBackendInfo(final Long cookie) {
        return getBackendInfo(cookie.longValue());
    }

    @Override
    public CompletionStage<? extends ShardBackendInfo> refreshBackendInfo(final Long cookie,
            final ShardBackendInfo staleInfo) {

        final ShardState existing = state;
        if (existing != null) {
            if (!staleInfo.equals(existing.getResult())) {
                return existing.getStage();
            }

            synchronized (this) {
                LOG.debug("Invalidating backend information {}", staleInfo);
                flushCache(shardName);
                LOG.trace("Invalidated cache %s", staleInfo);
                state = null;
            }
        }

        return getBackendInfo(cookie);
    }
}
