package org.opendaylight.controller.sal.binding.api.rpc;

import org.opendaylight.yangtools.yang.binding.BaseIdentity;
import org.opendaylight.yangtools.yang.binding.RpcService;

public interface RpcRoutingContext<C extends BaseIdentity,S extends RpcService> {

    Class<C> getContextType();
    Class<S> getServiceType();
}
