/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.core.api;

import java.io.Serializable;

import org.opendaylight.yangtools.concepts.Immutable;
import org.opendaylight.yangtools.yang.common.QName;

public class RpcRoutingContext implements Immutable, Serializable {

    /**
     *
     */
    private static final long serialVersionUID = -9079324728075883325L;

    private final QName context;
    private final QName rpc;


    private RpcRoutingContext(QName context, QName rpc) {
        super();
        this.context = context;
        this.rpc = rpc;
    }

    public static final RpcRoutingContext create(QName context, QName rpc) {
        return new RpcRoutingContext(context, rpc);
    }

    public QName getContext() {
        return context;
    }

    public QName getRpc() {
        return rpc;
    }

    @Override
    public String toString() {
        return "RpcRoutingContext [context=" + context + ", rpc=" + rpc + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((context == null) ? 0 : context.hashCode());
        result = prime * result + ((rpc == null) ? 0 : rpc.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        RpcRoutingContext other = (RpcRoutingContext) obj;
        if (context == null) {
            if (other.context != null)
                return false;
        } else if (!context.equals(other.context))
            return false;
        if (rpc == null) {
            if (other.rpc != null)
                return false;
        } else if (!rpc.equals(other.rpc))
            return false;
        return true;
    }
}
