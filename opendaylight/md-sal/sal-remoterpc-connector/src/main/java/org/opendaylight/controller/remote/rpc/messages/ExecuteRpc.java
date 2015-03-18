/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.remote.rpc.messages;


import com.google.common.base.Preconditions;
import java.io.Serializable;
import org.opendaylight.controller.protobuff.messages.common.NormalizedNodeMessages;
import org.opendaylight.yangtools.yang.common.QName;

public class ExecuteRpc implements Serializable {
    private static final long serialVersionUID = 1128904894827335676L;

    private final NormalizedNodeMessages.Node inputNormalizedNode;
    private final QName rpc;

    public ExecuteRpc(final NormalizedNodeMessages.Node inputNormalizedNode, final QName rpc) {
        Preconditions.checkNotNull(inputNormalizedNode, "Normalized Node input string should be present");
        Preconditions.checkNotNull(rpc, "rpc Qname should not be null");

        this.inputNormalizedNode = inputNormalizedNode;
        this.rpc = rpc;
    }

    public NormalizedNodeMessages.Node getInputNormalizedNode() {
        return inputNormalizedNode;
    }

    public QName getRpc() {
        return rpc;
    }
}
