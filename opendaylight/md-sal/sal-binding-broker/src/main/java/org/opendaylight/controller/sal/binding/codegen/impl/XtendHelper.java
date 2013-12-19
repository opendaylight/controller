package org.opendaylight.controller.sal.binding.codegen.impl;

import java.util.List;

import org.opendaylight.yangtools.yang.binding.BaseIdentity;
import org.opendaylight.yangtools.yang.binding.RpcService;
import org.opendaylight.yangtools.yang.model.api.TypeDefinition;
import org.opendaylight.yangtools.yang.model.api.type.UnionTypeDefinition;

public class XtendHelper {

    @SuppressWarnings({"rawtypes","unchecked"})
    public static Iterable<TypeDefinition> getTypes(UnionTypeDefinition definition) {
        return (Iterable<TypeDefinition>) (List) definition.getTypes();
    }
}
