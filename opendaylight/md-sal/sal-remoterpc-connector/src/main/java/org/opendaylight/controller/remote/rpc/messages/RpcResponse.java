/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.remote.rpc.messages;

import java.io.Serializable;
import org.opendaylight.controller.protobuff.messages.common.NormalizedNodeMessages;

public class RpcResponse implements Serializable {
    private static final long serialVersionUID = -4211279498688989245L;

    private final NormalizedNodeMessages.Node resultNormalizedNode;

    public RpcResponse(final NormalizedNodeMessages.Node inputNormalizedNode) {
        resultNormalizedNode = inputNormalizedNode;
    }

    public NormalizedNodeMessages.Node getResultNormalizedNode() {
        return resultNormalizedNode;
    }
}
