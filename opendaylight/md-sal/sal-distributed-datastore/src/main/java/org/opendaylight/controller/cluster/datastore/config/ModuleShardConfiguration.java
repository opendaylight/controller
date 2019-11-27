/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.config;

import static java.util.Objects.requireNonNull;

import java.net.URI;
import java.util.Collection;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.cluster.access.concepts.MemberName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.clustering.shard.configuration.rev191128.shard.persistence.Persistence;

/**
 * Encapsulates information for adding a new module shard configuration.
 *
 * @author Thomas Pantelis
 */
public class ModuleShardConfiguration {
    private final URI namespace;
    private final String moduleName;
    private final String shardName;
    private final String shardStrategyName;
    private final Collection<MemberName> shardMemberNames;
    private final Persistence persistence;

    /**
     * Constructs a new instance.
     *
     * @param namespace the name space of the module.
     * @param moduleName the name of the module.
     * @param shardName the name of the shard.
     * @param persistence the persistence of the shard. If not specified the default Data-store persistence is used
     * @param shardStrategyName the name of the sharding strategy (eg "module"). If null the default strategy
     *                          is used.
     * @param shardMemberNames the names of the shard's member replicas.
     */
    public ModuleShardConfiguration(@NonNull URI namespace, @NonNull String moduleName, @NonNull String shardName,
                                    @Nullable Persistence persistence, @Nullable String shardStrategyName,
                                    @NonNull Collection<MemberName> shardMemberNames) {
        this.namespace = requireNonNull(namespace, "nameSpace should not be null");
        this.moduleName = requireNonNull(moduleName, "moduleName should not be null");
        this.shardName = requireNonNull(shardName, "shardName should not be null");
        this.persistence = persistence;
        this.shardStrategyName = shardStrategyName;
        this.shardMemberNames = requireNonNull(shardMemberNames, "shardMemberNames");
    }

    public URI getNamespace() {
        return namespace;
    }

    public String getModuleName() {
        return moduleName;
    }

    public String getShardName() {
        return shardName;
    }

    public Persistence getPersistence() {
        return persistence;
    }

    public String getShardStrategyName() {
        return shardStrategyName;
    }

    public Collection<MemberName> getShardMemberNames() {
        return shardMemberNames;
    }

    @Override
    public String toString() {
        return "ModuleShardConfiguration [namespace=" + namespace + ", moduleName=" + moduleName + ", shardName="
                + shardName + ", shardMemberNames=" + shardMemberNames + ", shardStrategyName=" + shardStrategyName
                + "]";
    }
}
