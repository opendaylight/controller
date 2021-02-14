/*
 * Copyright (c) 2020 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.sharding;

import com.google.common.collect.ClassToInstanceMap;
import java.util.Collection;
import java.util.concurrent.CompletionStage;
import org.opendaylight.controller.cluster.ActorSystemProvider;
import org.opendaylight.controller.cluster.access.concepts.MemberName;
import org.opendaylight.controller.cluster.datastore.DistributedDataStoreInterface;
import org.opendaylight.mdsal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.mdsal.dom.api.DOMDataTreeListener;
import org.opendaylight.mdsal.dom.api.DOMDataTreeLoopException;
import org.opendaylight.mdsal.dom.api.DOMDataTreeProducer;
import org.opendaylight.mdsal.dom.api.DOMDataTreeService;
import org.opendaylight.mdsal.dom.api.DOMDataTreeServiceExtension;
import org.opendaylight.mdsal.dom.api.DOMDataTreeShard;
import org.opendaylight.mdsal.dom.api.DOMDataTreeShardingConflictException;
import org.opendaylight.mdsal.dom.api.DOMDataTreeShardingService;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(immediate = true, property = "type=default")
@Deprecated(forRemoval = true)
public final class OSGiDistributedShardedDOMDataTree
        implements DOMDataTreeService, DOMDataTreeShardingService, DistributedShardFactory {
    private static final Logger LOG = LoggerFactory.getLogger(OSGiDistributedShardedDOMDataTree.class);

    @Reference
    ActorSystemProvider actorSystemProvider = null;
    @Reference(target = "(type=distributed-config)")
    DistributedDataStoreInterface configDatastore = null;
    @Reference(target = "(type=distributed-operational)")
    DistributedDataStoreInterface operDatastore = null;

    private DistributedShardedDOMDataTree delegate;

    @Override
    public DOMDataTreeProducer createProducer(final Collection<DOMDataTreeIdentifier> subtrees) {
        return delegate.createProducer(subtrees);
    }

    @Override
    public ClassToInstanceMap<DOMDataTreeServiceExtension> getExtensions() {
        return delegate.getExtensions();
    }

    @Override
    public CompletionStage<DistributedShardRegistration> createDistributedShard(final DOMDataTreeIdentifier prefix,
            final Collection<MemberName> replicaMembers) throws DOMDataTreeShardingConflictException {
        return delegate.createDistributedShard(prefix, replicaMembers);
    }

    @Override
    public <T extends DOMDataTreeShard> ListenerRegistration<T> registerDataTreeShard(
            final DOMDataTreeIdentifier prefix, final T shard, final DOMDataTreeProducer producer)
            throws DOMDataTreeShardingConflictException {
        return delegate.registerDataTreeShard(prefix, shard, producer);
    }

    @Override
    public <T extends DOMDataTreeListener> ListenerRegistration<T> registerListener(final T listener,
            final Collection<DOMDataTreeIdentifier> subtrees, final boolean allowRxMerges,
            final Collection<DOMDataTreeProducer> producers) throws DOMDataTreeLoopException {
        return delegate.registerListener(listener, subtrees, allowRxMerges, producers);
    }

    @Activate
    void activate() {
        LOG.info("Distributed DOM Data Tree Service starting");
        delegate = new DistributedShardedDOMDataTree(actorSystemProvider, operDatastore, configDatastore);
        delegate.init();
        LOG.info("Distributed DOM Data Tree Service started");
    }

    @Deactivate
    void deactivate() {
        LOG.info("Distributed DOM Data Tree Service stopping");
        // TODO: this needs a shutdown hook, I think
        delegate = null;
        LOG.info("Distributed DOM Data Tree Service stopped");
    }
}
