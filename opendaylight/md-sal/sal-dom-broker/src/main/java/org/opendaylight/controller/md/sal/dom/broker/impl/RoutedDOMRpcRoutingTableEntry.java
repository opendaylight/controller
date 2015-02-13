/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.broker.impl;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.Futures;
import java.util.List;
import java.util.Map;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcException;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcIdentifier;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcImplementation;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcImplementationNotAvailableException;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcResult;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNodes;
import org.opendaylight.yangtools.yang.model.api.RpcDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class RoutedDOMRpcRoutingTableEntry extends AbstractDOMRpcRoutingTableEntry {
    private static final Logger LOG = LoggerFactory.getLogger(RoutedDOMRpcRoutingTableEntry.class);
    private final DOMRpcIdentifier globalRpcId;
    private final YangInstanceIdentifier keyId;

    private RoutedDOMRpcRoutingTableEntry(final DOMRpcIdentifier globalRpcId, final YangInstanceIdentifier keyId, final Map<YangInstanceIdentifier, List<DOMRpcImplementation>> impls) {
        super(globalRpcId.getType(), impls);
        this.keyId = Preconditions.checkNotNull(keyId);
        this.globalRpcId = Preconditions.checkNotNull(globalRpcId);
    }

    RoutedDOMRpcRoutingTableEntry(final RpcDefinition def, final YangInstanceIdentifier keyId, final Map<YangInstanceIdentifier, List<DOMRpcImplementation>> impls) {
        super(def.getPath(), impls);
        this.keyId = Preconditions.checkNotNull(keyId);
        this.globalRpcId = DOMRpcIdentifier.create(def.getPath());
    }

    @Override
    protected CheckedFuture<DOMRpcResult, DOMRpcException> invokeRpc(final NormalizedNode<?, ?> input) {
        final Optional<NormalizedNode<?, ?>> maybeKey = NormalizedNodes.findNode(input, keyId);

        // Routing key is present, attempt to deliver as a routed RPC
        if (maybeKey.isPresent()) {
            final NormalizedNode<?, ?> key = maybeKey.get();
            final Object value = key.getValue();
            if (value instanceof YangInstanceIdentifier) {
                final YangInstanceIdentifier iid = (YangInstanceIdentifier) value;
                final List<DOMRpcImplementation> impls = getImplementations(iid);
                if (impls != null) {
                    return impls.get(0).invokeRpc(DOMRpcIdentifier.create(getSchemaPath(), iid), input);
                }
                LOG.debug("No implementation for context {} found", iid);
            } else {
                LOG.warn("Ignoring wrong context value {}", value);
            }
        }

        final List<DOMRpcImplementation> impls = getImplementations(null);
        if (impls != null) {
            return impls.get(0).invokeRpc(globalRpcId, input);
        } else {
            return Futures.<DOMRpcResult, DOMRpcException>immediateFailedCheckedFuture(new DOMRpcImplementationNotAvailableException("No implementation of RPC %s available", getSchemaPath()));
        }
    }

    @Override
    protected RoutedDOMRpcRoutingTableEntry newInstance(final Map<YangInstanceIdentifier, List<DOMRpcImplementation>> impls) {
        return new RoutedDOMRpcRoutingTableEntry(globalRpcId, keyId, impls);
    }
}