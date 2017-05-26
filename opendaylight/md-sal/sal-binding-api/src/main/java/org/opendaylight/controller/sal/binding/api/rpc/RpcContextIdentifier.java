/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.binding.api.rpc;

import org.opendaylight.yangtools.concepts.Immutable;
import org.opendaylight.yangtools.yang.binding.BaseIdentity;
import org.opendaylight.yangtools.yang.binding.RpcService;

public final  class RpcContextIdentifier implements Immutable{

    public final Class<? extends RpcService> rpcService;
    public final Class<? extends BaseIdentity> routingContext;

    private RpcContextIdentifier(Class<? extends RpcService> rpcService, Class<? extends BaseIdentity> routingContext) {
        super();
        this.rpcService = rpcService;
        this.routingContext = routingContext;
    }

    public Class<? extends RpcService> getRpcService() {
        return rpcService;
    }

    public Class<? extends BaseIdentity> getRoutingContext() {
        return routingContext;
    }

    public static final RpcContextIdentifier contextForGlobalRpc(Class<? extends RpcService> serviceType) {
        return new RpcContextIdentifier(serviceType, null);
    }

    public static final RpcContextIdentifier contextFor(Class<? extends RpcService> serviceType,Class<? extends BaseIdentity> routingContext) {
        return new RpcContextIdentifier(serviceType, routingContext);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((routingContext == null) ? 0 : routingContext.hashCode());
        result = prime * result + ((rpcService == null) ? 0 : rpcService.hashCode());
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
        RpcContextIdentifier other = (RpcContextIdentifier) obj;
        if (routingContext == null) {
            if (other.routingContext != null)
                return false;
        } else if (!routingContext.equals(other.routingContext))
            return false;
        if (rpcService == null) {
            if (other.rpcService != null)
                return false;
        } else if (!rpcService.equals(other.rpcService))
            return false;
        return true;
    }

}
