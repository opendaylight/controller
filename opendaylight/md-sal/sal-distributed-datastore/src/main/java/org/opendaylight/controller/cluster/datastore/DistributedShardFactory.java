/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore;

import java.util.Collection;
import org.opendaylight.controller.cluster.access.concepts.MemberName;
import org.opendaylight.mdsal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.mdsal.dom.api.DOMDataTreeProducerException;
import org.opendaylight.mdsal.dom.api.DOMDataTreeShardingConflictException;

public interface DistributedShardFactory {

    /**
     * Register a new shard that is rooted at the desired prefix with replicas on the provided members.
     * Note to register a shard without replicas you still need to provide atleast one Member for the shard.
     *
     * @param prefix         Shard root
     * @param replicaMembers Members that this shard is replicated on, has to have atleast one Member even if the shard
     *                       should not be replicated.
     * @return ShardRegistration that should be closed if the shard should be destroyed
     * @throws DOMDataTreeShardingConflictException
     * @throws DOMDataTreeProducerException         in case there is a problem closing the initial producer that is used to
     *                                              register the shard into the ShardingService
     */
    DistributedShardRegistration createDistributedShard(DOMDataTreeIdentifier prefix, Collection<MemberName> replicaMembers)
            throws DOMDataTreeShardingConflictException, DOMDataTreeProducerException;

    interface DistributedShardRegistration {
        void close();
    }
}