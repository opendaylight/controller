/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.config;

import akka.cluster.ddata.ReplicatedData;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import org.opendaylight.controller.cluster.access.concepts.MemberName;
import org.opendaylight.mdsal.dom.api.DOMDataTreeIdentifier;

/**
 * Configuration for prefix based shards.
 */
public class PrefixShardConfiguration implements ReplicatedData, Serializable {
    private static final long serialVersionUID = 1L;

    private final DOMDataTreeIdentifier prefix;
    private final String shardStrategyName;
    private final Collection<MemberName> shardMemberNames;

    public PrefixShardConfiguration(final DOMDataTreeIdentifier prefix,
                                    final String shardStrategyName,
                                    final Collection<MemberName> shardMemberNames) {
        this.prefix = Preconditions.checkNotNull(prefix);
        this.shardStrategyName = Preconditions.checkNotNull(shardStrategyName);
        this.shardMemberNames = ImmutableSet.copyOf(shardMemberNames);
    }

    public DOMDataTreeIdentifier getPrefix() {
        return prefix;
    }

    public String getShardStrategyName() {
        return shardStrategyName;
    }

    public Collection<MemberName> getShardMemberNames() {
        return shardMemberNames;
    }

    @Override
    public String toString() {
        return "PrefixShardConfiguration{"
                + "prefix=" + prefix
                + ", shardStrategyName='"
                + shardStrategyName + '\''
                + ", shardMemberNames=" + shardMemberNames
                + '}';
    }

    public String toDataMapKey() {
        return "prefix=" + prefix;
    }

    @Override
    public ReplicatedData merge(final ReplicatedData replicatedData) {
        if (!(replicatedData instanceof PrefixShardConfiguration)) {
            throw new IllegalStateException("replicatedData expected to be instance of PrefixShardConfiguration");
        }
        final PrefixShardConfiguration entry = (PrefixShardConfiguration) replicatedData;
        if (!entry.getPrefix().equals(prefix)) {
            // this should never happen since the key is the prefix
            // if it does just return current?
            return this;
        }
        final HashSet<MemberName> members = new HashSet<>(shardMemberNames);
        members.addAll(entry.getShardMemberNames());
        return new PrefixShardConfiguration(prefix, shardStrategyName, members);
    }
}
