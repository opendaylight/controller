/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.clustering.it.provider.impl;

import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcException;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcIdentifier;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcImplementation;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcImplementationRegistration;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcProviderService;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcResult;
import org.opendaylight.controller.md.sal.dom.spi.DefaultDOMRpcResult;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonService;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceProvider;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceRegistration;
import org.opendaylight.mdsal.singleton.common.api.ServiceGroupIdentifier;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.LeafNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableContainerNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableLeafNodeBuilder;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SingletonGetConstantService implements DOMRpcImplementation, ClusterSingletonService {

    private static final Logger LOG = LoggerFactory.getLogger(SingletonGetConstantService.class);

    private static final QName OUTPUT =
            QName.create("tag:opendaylight.org,2017:controller:yang:lowlevel:target","2017-02-15", "output");
    private static final QName CONSTANT =
            QName.create("tag:opendaylight.org,2017:controller:yang:lowlevel:target","2017-02-15", "constant");
    private static final QName CONTEXT =
            QName.create("tag:opendaylight.org,2017:controller:yang:lowlevel:target","2017-02-15", "context");
    private static final QName GET_SINGLETON_CONSTANT =
            QName.create("tag:opendaylight.org,2017:controller:yang:lowlevel:target","2017-02-15",
                    "get-singleton-constant");

    private static final ServiceGroupIdentifier SERVICE_GROUP_IDENTIFIER =
            ServiceGroupIdentifier.create("get-singleton-constant-service");

    private final DOMRpcProviderService rpcProviderService;
    private final String constant;
    private DOMRpcImplementationRegistration<SingletonGetConstantService> rpcRegistration;

    private SingletonGetConstantService(final DOMRpcProviderService rpcProviderService,
                                        final String constant) {


        this.rpcProviderService = rpcProviderService;
        this.constant = constant;
    }

    public static ClusterSingletonServiceRegistration registerNew(
            final ClusterSingletonServiceProvider singletonService, final DOMRpcProviderService rpcProviderService,
            final String constant) {
        LOG.debug("Registering get-singleton-constant into ClusterSingletonService, value {}", constant);

        return singletonService
                .registerClusterSingletonService(new SingletonGetConstantService(rpcProviderService, constant));
    }

    @Nonnull
    @Override
    public CheckedFuture<DOMRpcResult, DOMRpcException> invokeRpc(@Nonnull DOMRpcIdentifier rpc,
            @Nullable NormalizedNode<?, ?> input) {
        LOG.debug("get-singleton-constant invoked, current value: {}", constant);

        final LeafNode<Object> value = ImmutableLeafNodeBuilder.create()
                .withNodeIdentifier(new NodeIdentifier(CONSTANT))
                .withValue(constant)
                .build();

        final ContainerNode result = ImmutableContainerNodeBuilder.create()
                .withNodeIdentifier(new NodeIdentifier(OUTPUT))
                .withChild(value)
                .build();

        return Futures.immediateCheckedFuture(new DefaultDOMRpcResult(result));
    }

    @Override
    public void instantiateServiceInstance() {
        LOG.debug("Gained ownership of get-singleton-constant, registering service into rpcService");
        final DOMRpcIdentifier id = DOMRpcIdentifier.create(SchemaPath.create(true, GET_SINGLETON_CONSTANT));

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
