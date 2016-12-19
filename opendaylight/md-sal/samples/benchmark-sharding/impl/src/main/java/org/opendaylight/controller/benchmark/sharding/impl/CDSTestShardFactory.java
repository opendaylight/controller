/*
 * Copyright © 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.benchmark.sharding.impl;

import akka.cluster.Cluster;
import java.util.Collection;
import java.util.stream.Collectors;
import org.opendaylight.controller.cluster.ActorSystemProvider;
import org.opendaylight.controller.cluster.access.concepts.MemberName;
import org.opendaylight.controller.cluster.sharding.DOMDataTreeShardCreationFailedException;
import org.opendaylight.controller.cluster.sharding.DistributedShardFactory;
import org.opendaylight.controller.cluster.sharding.DistributedShardFactory.DistributedShardRegistration;
import org.opendaylight.mdsal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.mdsal.dom.api.DOMDataTreeProducerException;
import org.opendaylight.mdsal.dom.api.DOMDataTreeShardingConflictException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CDSTestShardFactory implements ShardFactory {
    private static final Logger LOG = LoggerFactory.getLogger(CDSTestShardFactory.class);

    private final DistributedShardFactory shardFactoryDelegate;
    private final ActorSystemProvider actorSystemProvider;

    public CDSTestShardFactory(final DistributedShardFactory shardFactoryDelegate, final ActorSystemProvider actorSystemProvider) {
        this.shardFactoryDelegate = shardFactoryDelegate;
        this.actorSystemProvider = actorSystemProvider;
    }

    @Override
    public ShardRegistration createShard(final DOMDataTreeIdentifier prefix)
            throws DOMDataTreeShardingConflictException,
            DOMDataTreeShardCreationFailedException, DOMDataTreeProducerException {
        LOG.debug("Creating CDSShard for prefix {}", prefix);

        Cluster cluster = Cluster.get(actorSystemProvider.getActorSystem());
        Collection<MemberName> replicas =
                cluster.state().getAllRoles().stream().map(MemberName::forName).collect(Collectors.toList());

        final DistributedShardRegistration distributedShardRegistration =
                shardFactoryDelegate.createDistributedShard(prefix, replicas).checkedGet();
        return () -> distributedShardRegistration.close().get();
    }
}
