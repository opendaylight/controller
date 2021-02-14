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

public final class GetConstantService implements DOMRpcImplementation {
    private static final Logger LOG = LoggerFactory.getLogger(GetConstantService.class);

    private static final QNameModule MODULE = QNameModule.create(
        URI.create("tag:opendaylight.org,2017:controller:yang:lowlevel:target"), Revision.of("2017-02-15")).intern();

    private static final QName OUTPUT = YangConstants.operationOutputQName(MODULE).intern();
    private static final QName CONSTANT = QName.create(MODULE, "constant").intern();
    private static final QName GET_CONSTANT = QName.create(MODULE, "get-constant").intern();

    private final String constant;

    private GetConstantService(final String constant) {
        this.constant = constant;
    }

    public static DOMRpcImplementationRegistration<GetConstantService> registerNew(
            final DOMRpcProviderService rpcProviderService, final String constant) {
        LOG.debug("Registering get-constant service, constant value: {}", constant);
        return rpcProviderService.registerRpcImplementation(new GetConstantService(constant),
            DOMRpcIdentifier.create(GET_CONSTANT));
    }

    @Override
    public ListenableFuture<DOMRpcResult> invokeRpc(final DOMRpcIdentifier rpc, final NormalizedNode input) {
        LOG.debug("get-constant invoked, current value: {}", constant);

        return Futures.immediateFuture(new DefaultDOMRpcResult(ImmutableContainerNodeBuilder.create()
            .withNodeIdentifier(new NodeIdentifier(OUTPUT))
            .withChild(ImmutableLeafNodeBuilder.create()
                .withNodeIdentifier(new NodeIdentifier(CONSTANT))
                .withValue(constant)
                .build())
            .build()));
    }
}
