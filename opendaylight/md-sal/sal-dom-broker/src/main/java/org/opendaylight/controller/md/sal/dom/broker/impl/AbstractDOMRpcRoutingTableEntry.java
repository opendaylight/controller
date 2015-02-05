/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.broker.impl;

import com.google.common.util.concurrent.CheckedFuture;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcException;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcResult;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

abstract class AbstractDOMRpcRoutingTableEntry {
    protected abstract CheckedFuture<DOMRpcResult, DOMRpcException> invokeRpc(final NormalizedNode<?, ?> input);
}
