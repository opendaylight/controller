/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.broker.impl;

import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.Futures;
import java.util.List;
import java.util.Map;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcException;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcImplementation;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcImplementationNotAvailableException;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcResult;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;

final class UnknownDOMRpcRoutingTableEntry extends AbstractDOMRpcRoutingTableEntry {
    private final CheckedFuture<DOMRpcResult, DOMRpcException> unknownRpc;

    UnknownDOMRpcRoutingTableEntry(final SchemaPath schemaPath, final Map<YangInstanceIdentifier, List<DOMRpcImplementation>> impls) {
        super(schemaPath, impls);
        unknownRpc = Futures.<DOMRpcResult, DOMRpcException>immediateFailedCheckedFuture(
            new DOMRpcImplementationNotAvailableException("SchemaPath %s is not resolved to an RPC", schemaPath));
    }

    @Override
    protected CheckedFuture<DOMRpcResult, DOMRpcException> invokeRpc(final NormalizedNode<?, ?> input) {
        return unknownRpc;
    }

    @Override
    protected UnknownDOMRpcRoutingTableEntry newInstance(final Map<YangInstanceIdentifier, List<DOMRpcImplementation>> impls) {
        return new UnknownDOMRpcRoutingTableEntry(getSchemaPath(), impls);
    }
}