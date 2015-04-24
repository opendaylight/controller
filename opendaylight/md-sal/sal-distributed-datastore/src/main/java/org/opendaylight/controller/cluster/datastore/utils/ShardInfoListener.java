/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.utils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opendaylight.controller.cluster.datastore.messages.PrimaryShardInfo;

/**
 * Listener interface used to register for primary shard information changes.
 * Implementations of this interface can be registered with {@link ActorContext}
 * to receive notifications about shard information changes.
 */
public interface ShardInfoListener {
    /**
     * Update {@link PrimaryShardInfo} for a particular shard.
     * @param shardName Shard name
     * @param primaryShardInfo New {@link PrimaryShardInfo}, null if the information
     *                         became unavailable.
     */
    void onShardInfoUpdated(@Nonnull String shardName, @Nullable PrimaryShardInfo primaryShardInfo);
}
