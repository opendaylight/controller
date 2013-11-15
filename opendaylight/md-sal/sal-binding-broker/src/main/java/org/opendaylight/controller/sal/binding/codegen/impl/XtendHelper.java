package org.opendaylight.controller.sal.binding.codegen.impl;

import org.opendaylight.yangtools.yang.binding.BaseIdentity;
import org.opendaylight.yangtools.yang.binding.RpcService;

public class XtendHelper {

    public static <C extends BaseIdentity> RpcRoutingTableImpl createRoutingTable(
            Class<C> cls) {
        return new RpcRoutingTableImpl<>(cls);
    }
}
