/*
 * Copyright (c) 2015 Dell Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.shardmanager;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import org.opendaylight.controller.cluster.datastore.config.PrefixShardConfiguration;
import org.opendaylight.mdsal.dom.api.DOMDataTreeIdentifier;

/**
 * Persisted data of the ShardManager.
 *
 * @deprecated Use {@link org.opendaylight.controller.cluster.datastore.persisted.ShardManagerSnapshot} instead.
 */
@Deprecated
public final class ShardManagerSnapshot implements Serializable {
    private static final long serialVersionUID = 1L;
    private final List<String> shardList;

    private final Map<DOMDataTreeIdentifier, PrefixShardConfiguration> prefixShardConfiguration;

    ShardManagerSnapshot(final @Nonnull List<String> shardList,
                         final Map<DOMDataTreeIdentifier, PrefixShardConfiguration> prefixShardConfiguration) {
        this.shardList = ImmutableList.copyOf(shardList);
        this.prefixShardConfiguration = ImmutableMap.copyOf(prefixShardConfiguration);
    }

    public List<String> getShardList() {
        return this.shardList;
    }

    /**
     * Creates a ShardManagerSnapshot.
     *
     * @deprecated This method is for migration only and should me removed once
     *             org.opendaylight.controller.cluster.datastore.ShardManagerSnapshot is removed.
     */
    @Deprecated
    public static ShardManagerSnapshot forShardList(final @Nonnull List<String> shardList) {
        return new ShardManagerSnapshot(shardList, Collections.emptyMap());
    }

    public Map<DOMDataTreeIdentifier, PrefixShardConfiguration> getPrefixShardConfiguration() {
        return prefixShardConfiguration;
    }

    private Object readResolve() {
        return new org.opendaylight.controller.cluster.datastore.persisted.ShardManagerSnapshot(shardList,
                prefixShardConfiguration);
    }

    @Override
    public String toString() {
        return "ShardManagerSnapshot [ShardList = " + shardList + " ]";
    }
}
