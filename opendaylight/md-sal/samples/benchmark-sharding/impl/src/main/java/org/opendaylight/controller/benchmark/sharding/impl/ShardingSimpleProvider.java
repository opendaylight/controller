/*
 * Copyright © 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.benchmark.sharding.impl;

import org.opendaylight.controller.benchmark.sharding.impl.tests.ShardTestFactory;
import org.opendaylight.controller.cluster.ActorSystemProvider;
import org.opendaylight.controller.cluster.sharding.DistributedShardFactory;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.controller.sal.core.api.model.SchemaService;
import org.opendaylight.mdsal.dom.api.DOMDataTreeService;
import org.opendaylight.mdsal.dom.api.DOMDataTreeShardingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Interfaces the shardingsimple application with the Blueprint.
 * On initialization creates all other shardingsimple infra used throughout
 * the run of the app (ShardHelper, ShardTestFactory, etc.)
 *
 * @author jmedved
 */
public class ShardingSimpleProvider {

    private static final Logger LOG = LoggerFactory.getLogger(ShardingSimpleProvider.class);

    private final ShardHelper shardHelper;
    private final ShardingsimpleServiceImpl rpcServiceImpl;
    private final ShardTestFactory testFactory;

    /**
     * Public constructor - references to MD-SAL services injected through here.
     *
     * @param rpcRegistry reference to MD-SAL RPC Registry
     * @param dataTreeShardingService reference to MD-SAL Data Tree Sharding Service
     * @param dataTreeService reference to MD-SAL Data Tree  Service
     * @param schemaService reference to MD-SAL Schema Service
     */
    public ShardingSimpleProvider(final RpcProviderRegistry rpcRegistry,
                                  final DOMDataTreeShardingService dataTreeShardingService,
                                  final DOMDataTreeService dataTreeService,
                                  final DistributedShardFactory shardFactory,
                                  final ActorSystemProvider actorSystemProvider,
                                  final SchemaService schemaService) {
        this.shardHelper = new ShardHelper(dataTreeShardingService, dataTreeService, schemaService);
        this.testFactory = new ShardTestFactory(shardHelper,
                dataTreeService, shardFactory, dataTreeShardingService, actorSystemProvider, schemaService);
        this.rpcServiceImpl = new ShardingsimpleServiceImpl(rpcRegistry, testFactory);

        LOG.info("ShardingSimpleProvider Constructor finished");
    }

    /**
     * Method called when the blueprint container is created.
     */
    public void init() {
        LOG.info("ShardingSimpleProvider Session Initiated");
        rpcServiceImpl.init();
    }

    /**
     * Method called when the blueprint container is destroyed.
     */
    public void close() {
        LOG.info("ShardingSimpleProvider Closed");
        rpcServiceImpl.close();
        shardHelper.close();

    }
}