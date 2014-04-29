/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.connect.api;

import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.model.api.SchemaContextListener;

public interface MessageTransformer<M> extends SchemaContextListener {

    CompositeNode toNotification(M message);

    M toRpcRequest(QName rpc, CompositeNode node);

    RpcResult<CompositeNode> toRpcResult(M message, QName rpc);

}
