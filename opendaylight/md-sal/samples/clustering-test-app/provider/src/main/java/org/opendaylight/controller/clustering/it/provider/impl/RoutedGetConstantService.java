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
import org.opendaylight.mdsal.binding.dom.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.mdsal.dom.api.DOMRpcIdentifier;
import org.opendaylight.mdsal.dom.api.DOMRpcImplementation;
import org.opendaylight.mdsal.dom.api.DOMRpcProviderService;
import org.opendaylight.mdsal.dom.api.DOMRpcResult;
import org.opendaylight.mdsal.dom.spi.DefaultDOMRpcResult;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.QNameModule;
import org.opendaylight.yangtools.yang.common.YangConstants;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.spi.node.ImmutableNodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class RoutedGetConstantService implements DOMRpcImplementation {
    private static final Logger LOG = LoggerFactory.getLogger(RoutedGetConstantService.class);

    private static final QNameModule MODULE =
        QNameModule.ofRevision("tag:opendaylight.org,2017:controller:yang:lowlevel:target", "2017-02-15").intern();
    private static final QName OUTPUT = YangConstants.operationOutputQName(MODULE).intern();
    private static final QName CONSTANT = QName.create(MODULE, "constant").intern();
    private static final QName GET_CONTEXTED_CONSTANT = QName.create(MODULE, "get-contexted-constant").intern();

    private final String constant;

    private RoutedGetConstantService(final String constant) {
        this.constant = constant;
    }

    public static Registration registerNew(final BindingNormalizedNodeSerializer codec,
            final DOMRpcProviderService rpcProviderService, final String constant,
            final InstanceIdentifier<?> context) {

        LOG.debug("Registering get-contexted-constant on context: {}, with value: {}", context, constant);

        final var yid = codec.toYangInstanceIdentifier(context);
        final var id = DOMRpcIdentifier.create(GET_CONTEXTED_CONSTANT, yid);

        return rpcProviderService.registerRpcImplementation(new RoutedGetConstantService(constant), id);
    }

    @Override
    public ListenableFuture<DOMRpcResult> invokeRpc(final DOMRpcIdentifier rpc, final ContainerNode input) {
        LOG.debug("get-contexted-constant invoked, current value: {}", constant);

        return Futures.immediateFuture(new DefaultDOMRpcResult(ImmutableNodes.newContainerBuilder()
            .withNodeIdentifier(new NodeIdentifier(OUTPUT))
            .withChild(ImmutableNodes.leafNode(CONSTANT, constant))
            .build()));
    }
}
