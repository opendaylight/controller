package org.opendaylight.controller.sal.binding.codegen.impl;

import java.util.List;

import org.opendaylight.yangtools.yang.binding.BaseIdentity;
import org.opendaylight.yangtools.yang.binding.RpcService;
import org.opendaylight.yangtools.yang.model.api.TypeDefinition;
import org.opendaylight.yangtools.yang.model.api.type.UnionTypeDefinition;

public class XtendHelper {

    public static <C extends BaseIdentity> RpcRoutingTableImpl createRoutingTable(
            Class<C> cls) {
        return new RpcRoutingTableImpl<>(cls);
    }

    @SuppressWarnings({"rawtypes","unchecked"})
    public static Iterable<TypeDefinition> getTypes(UnionTypeDefinition definition) {
        return (Iterable<TypeDefinition>) (List) definition.getTypes();
    }
}
