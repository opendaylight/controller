package org.opendaylight.controller.sal.rest.impl;

import org.opendaylight.yangtools.yang.model.api.TypeDefinition;

public final class RestUtil {

    public final static TypeDefinition<?> resolveBaseTypeFrom(TypeDefinition<?> type) {
        TypeDefinition<?> superType = type;
        while (superType.getBaseType() != null) {
            superType = superType.getBaseType();
        }
        return superType;
    }

}
