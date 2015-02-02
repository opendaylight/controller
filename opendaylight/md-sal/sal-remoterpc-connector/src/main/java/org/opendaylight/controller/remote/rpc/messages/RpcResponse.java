/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.remote.rpc.messages;

import java.io.Serializable;

public class RpcResponse implements Serializable {
    private static final long serialVersionUID = -4211279498688989245L;

    private final String resultCompositeNode;

    public RpcResponse(final String resultCompositeNode) {
        this.resultCompositeNode = resultCompositeNode;
    }

    public String getResultCompositeNode() {
        return resultCompositeNode;
    }
}
