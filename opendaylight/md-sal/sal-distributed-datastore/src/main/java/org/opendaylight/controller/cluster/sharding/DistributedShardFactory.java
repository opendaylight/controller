/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.sharding;

import com.google.common.annotations.Beta;
import java.util.Collection;
import java.util.concurrent.CompletionStage;
import org.opendaylight.controller.cluster.access.concepts.MemberName;
import org.opendaylight.mdsal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.mdsal.dom.api.DOMDataTreeShardingConflictException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.clustering.shard.configuration.rev191128.shard.persistence.Persistence;

/**
 * A factory that handles addition of new clustered shard's based on a prefix. This factory is a QoL class that handles
 * all the boilerplate that comes with registration of a new clustered shard into the system and creating the backend
 * shard/replicas that come along with it.
 */
@Beta
public interface DistributedShardFactory {
    /**
     * Register a new shard that is rooted at the desired prefix with replicas on the provided members.
     * Note to register a shard without replicas you still need to provide at least one Member for the shard.
     *
     * @param prefix         Shard root
     * @param persistence    Shard persistence configuration
     * @param replicaMembers Members that this shard is replicated on, has to have at least one Member even if the shard
     *                       should not be replicated.
     * @return A future that will be completed with a DistributedShardRegistration once the backend and frontend shards
     *         are spawned.
     * @throws DOMDataTreeShardingConflictException If the initial check for a conflict on the local node fails, the
     *         sharding configuration won't be updated if this exception is thrown.
     */
    CompletionStage<DistributedShardRegistration>
        createDistributedShard(DOMDataTreeIdentifier prefix, Persistence persistence,
                               Collection<MemberName> replicaMembers)
            throws DOMDataTreeShardingConflictException;
}