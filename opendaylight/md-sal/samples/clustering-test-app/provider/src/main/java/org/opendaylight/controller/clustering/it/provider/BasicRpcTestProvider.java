/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.clustering.it.provider;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.opendaylight.mdsal.binding.api.RpcProviderService;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonService;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceProvider;
import org.opendaylight.mdsal.singleton.common.api.ServiceGroupIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.controller.basic.rpc.test.rev160120.BasicGlobalInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.controller.basic.rpc.test.rev160120.BasicGlobalOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.controller.basic.rpc.test.rev160120.BasicGlobalOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.controller.basic.rpc.test.rev160120.BasicRpcTestService;
import org.opendaylight.yangtools.concepts.ObjectRegistration;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BasicRpcTestProvider implements ClusterSingletonService, BasicRpcTestService {
    private static final Logger LOG = LoggerFactory.getLogger(BasicRpcTestProvider.class);
    private static final ServiceGroupIdentifier IDENTIFIER = ServiceGroupIdentifier.create("Basic-rpc-test");

    private final RpcProviderService rpcProviderRegistry;
    private final ClusterSingletonServiceProvider singletonService;

    private ObjectRegistration<?> rpcRegistration;

    public BasicRpcTestProvider(final RpcProviderService rpcProviderRegistry,
                                final ClusterSingletonServiceProvider singletonService) {
        this.rpcProviderRegistry = rpcProviderRegistry;
        this.singletonService = singletonService;

        singletonService.registerClusterSingletonService(this);
    }

    @Override
    public void instantiateServiceInstance() {
        LOG.info("Basic testing rpc registered as global");
        rpcRegistration = rpcProviderRegistry.registerRpcImplementation(BasicRpcTestService.class, this);
    }

    @Override
    public ListenableFuture<Void> closeServiceInstance() {
        rpcRegistration.close();
        rpcRegistration = null;

        return Futures.immediateFuture(null);
    }

    @Override
    public ServiceGroupIdentifier getIdentifier() {
        return IDENTIFIER;
    }

    @Override
    public ListenableFuture<RpcResult<BasicGlobalOutput>> basicGlobal(final BasicGlobalInput input) {
        LOG.info("Basic test global rpc invoked");

        return Futures.immediateFuture(RpcResultBuilder.success(new BasicGlobalOutputBuilder().build()).build());
    }
}
