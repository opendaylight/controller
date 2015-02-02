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
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

public class InvokeRpc implements Serializable {
    private static final long serialVersionUID = -2813459607858108953L;

    private final QName rpc;
    private final YangInstanceIdentifier identifier;
    private final CompositeNode input;

    public InvokeRpc(final QName rpc, final YangInstanceIdentifier identifier, final CompositeNode input) {
        Preconditions.checkNotNull(rpc, "rpc qname should not be null");
        Preconditions.checkNotNull(input, "rpc input should not be null");

        this.rpc = rpc;
        this.identifier = identifier;
        this.input = input;
    }

    public QName getRpc() {
        return rpc;
    }

    public YangInstanceIdentifier getIdentifier() {
        return identifier;
    }

    public CompositeNode getInput() {
        return input;
    }
}
