/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.persisted;

import com.google.common.collect.ImmutableList;
import java.util.List;
import org.eclipse.jdt.annotation.NonNull;

/**
 * Represents the persisted snapshot state for the ShardManager.
 *
 * @author Thomas Pantelis
 */
public final class ShardManagerSnapshot {
    private final List<String> shardList;

    public ShardManagerSnapshot(final @NonNull List<String> shardList) {
        this.shardList = ImmutableList.copyOf(shardList);
    }

    public List<String> getShardList() {
        return shardList;
    }

    @Override
    public String toString() {
        return "ShardManagerSnapshot [ShardList = " + shardList + " ]";
    }
}
