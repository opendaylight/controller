/*
 * Copyright © 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.benchmark.sharding.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.RpcRegistration;
import org.opendaylight.controller.benchmark.sharding.impl.tests.AbstractShardTest;
import org.opendaylight.controller.benchmark.sharding.impl.tests.ShardTestFactory;
import org.opendaylight.controller.benchmark.sharding.impl.tests.ShardTestStats;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.clustering.sharding.simple.rev160802.ShardTestInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.clustering.sharding.simple.rev160802.ShardTestOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.clustering.sharding.simple.rev160802.ShardTestOutput.Status;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.clustering.sharding.simple.rev160802.ShardTestOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.clustering.sharding.simple.rev160802.ShardingsimpleService;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements the shardingsimple RPC API.
 *
 * @author jmedved
 */
public class ShardingsimpleServiceImpl implements ShardingsimpleService, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(ShardingsimpleServiceImpl.class);

    private final RpcProviderRegistry rpcRegistry;
    private final ShardTestFactory testFactory;
    private RpcRegistration<ShardingsimpleService> rpcReg;

    /**
     * Constructor.
     *
     * @param rpcRegistry reference to MD-SAL RPC Registry
     * @param testFactory reference to the Shard Test Factory
     */
    public ShardingsimpleServiceImpl(final RpcProviderRegistry rpcRegistry, final ShardTestFactory testFactory) {
        LOG.info("Creating Shardingsimple RPC service");
        this.rpcRegistry = rpcRegistry;
        this.testFactory = testFactory;
    }

    /**
     * Initialization - called when Blueprint container is coming up.
     */
    public void init() {
        LOG.info("Registering Shardingsimple RPC service");
        rpcReg = rpcRegistry.addRpcImplementation(ShardingsimpleService.class, this);
    }

    /**
     * cleaning-up everything.
     */
    @Override
    public void close() {
        rpcReg.close();
    }

    @Override
    @SuppressWarnings("IllegalCatch")
    public Future<RpcResult<ShardTestOutput>> shardTest(final ShardTestInput input) {
        LOG.info("Input: {}", input);

        try {
            final AbstractShardTest shardTest = testFactory.createTest(input);
            final ShardTestStats stats = shardTest.runTest();
            if (input.isValidateData()) {
                shardTest.registerValidationListener();
            }

            final List<Long> shardExecTime = new ArrayList<>();
            shardExecTime.add((long)1);
            shardExecTime.add((long)2);
            shardExecTime.add((long)3);

            final ShardTestOutput output = new ShardTestOutputBuilder()
                    .setStatus(Status.OK)
                    .setTotalExecTime(stats.getExecTime())
                    .setShardExecTime(shardExecTime)
                    .setTxError(stats.getTxError())
                    .setTxOk(stats.getTxOk())
                    .setTxSubmitted(stats.getTxSubmitted())
                    .setListenerEventsOk(stats.getListenerEventsOk())
                    .setListenerEventsFail(stats.getListenerEventsFail())
                    .build();
            shardTest.close();
            return RpcResultBuilder.success(output).buildFuture();
        } catch (Exception e) {
            LOG.error("Failed to create/execute Shard Test, {}", e);
            return RpcResultBuilder.success(new ShardTestOutputBuilder()
                                                    .setStatus(Status.FAILED)
                                                    .build()).buildFuture();
        }
    }
}
