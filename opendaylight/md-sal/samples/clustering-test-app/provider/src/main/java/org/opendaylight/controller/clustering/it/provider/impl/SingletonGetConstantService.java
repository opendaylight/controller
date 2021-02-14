/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.clustering.it.provider.impl;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.net.URI;
import org.opendaylight.mdsal.dom.api.DOMRpcIdentifier;
import org.opendaylight.mdsal.dom.api.DOMRpcImplementation;
import org.opendaylight.mdsal.dom.api.DOMRpcImplementationRegistration;
import org.opendaylight.mdsal.dom.api.DOMRpcProviderService;
import org.opendaylight.mdsal.dom.api.DOMRpcResult;
import org.opendaylight.mdsal.dom.spi.DefaultDOMRpcResult;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonService;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceProvider;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceRegistration;
import org.opendaylight.mdsal.singleton.common.api.ServiceGroupIdentifier;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.QNameModule;
import org.opendaylight.yangtools.yang.common.Revision;
import org.opendaylight.yangtools.yang.common.YangConstants;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableContainerNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableLeafNodeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SingletonGetConstantService implements DOMRpcImplementation, ClusterSingletonService {

    private static final Logger LOG = LoggerFactory.getLogger(SingletonGetConstantService.class);

    private static final QNameModule MODULE = QNameModule.create(
        URI.create("tag:opendaylight.org,2017:controller:yang:lowlevel:target"), Revision.of("2017-02-15")).intern();
    private static final QName OUTPUT = YangConstants.operationOutputQName(MODULE).intern();
    private static final QName CONSTANT = QName.create(MODULE, "constant").intern();
    private static final QName CONTEXT = QName.create(MODULE, "context").intern();
    private static final QName GET_SINGLETON_CONSTANT = QName.create(MODULE, "get-singleton-constant").intern();

    private static final ServiceGroupIdentifier SERVICE_GROUP_IDENTIFIER =
            ServiceGroupIdentifier.create("get-singleton-constant-service");

    private final DOMRpcProviderService rpcProviderService;
    private final String constant;
    private DOMRpcImplementationRegistration<SingletonGetConstantService> rpcRegistration;

    private SingletonGetConstantService(final DOMRpcProviderService rpcProviderService, final String constant) {
        this.rpcProviderService = rpcProviderService;
        this.constant = constant;
    }

    public static ClusterSingletonServiceRegistration registerNew(
            final ClusterSingletonServiceProvider singletonService, final DOMRpcProviderService rpcProviderService,
            final String constant) {
        LOG.debug("Registering get-singleton-constant into ClusterSingletonService, value {}", constant);

        return singletonService.registerClusterSingletonService(
            new SingletonGetConstantService(rpcProviderService, constant));
    }

    @Override
    public ListenableFuture<DOMRpcResult> invokeRpc(final DOMRpcIdentifier rpc, final NormalizedNode input) {
        LOG.debug("get-singleton-constant invoked, current value: {}", constant);

        return Futures.immediateFuture(new DefaultDOMRpcResult(ImmutableContainerNodeBuilder.create()
            .withNodeIdentifier(new NodeIdentifier(OUTPUT))
            .withChild(ImmutableLeafNodeBuilder.create()
                .withNodeIdentifier(new NodeIdentifier(CONSTANT))
                .withValue(constant)
                .build())
            .build()));
    }

    @Override
    public void instantiateServiceInstance() {
        LOG.debug("Gained ownership of get-singleton-constant, registering service into rpcService");
        final DOMRpcIdentifier id = DOMRpcIdentifier.create(GET_SINGLETON_CONSTANT);

        rpcRegistration = rpcProviderService.registerRpcImplementation(this, id);
    }

    @Override
    public ListenableFuture<Void> closeServiceInstance() {
        LOG.debug("Closing get-singleton-constant instance");
        rpcRegistration.close();
        return Futures.immediateFuture(null);
    }

    @Override
    public ServiceGroupIdentifier getIdentifier() {
        return SERVICE_GROUP_IDENTIFIER;
    }
}
