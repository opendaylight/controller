/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yangjmxgenerator;

import org.opendaylight.yangtools.sal.binding.generator.spi.TypeProvider;
import org.opendaylight.yangtools.sal.binding.model.api.Type;
import org.opendaylight.yangtools.yang.model.api.LeafListSchemaNode;
import org.opendaylight.yangtools.yang.model.api.LeafSchemaNode;
import org.opendaylight.yangtools.yang.model.api.SchemaNode;
import org.opendaylight.yangtools.yang.model.api.TypeDefinition;

public class TypeProviderWrapper {
    private final TypeProvider typeProvider;

    public TypeProviderWrapper(TypeProvider typeProvider) {
        this.typeProvider = typeProvider;
    }

    public Type getType(LeafSchemaNode leaf) {
        TypeDefinition<?> type = leaf.getType();
        return getType(leaf, type);
    }

    public String getDefault(LeafSchemaNode node) {
        return typeProvider.getTypeDefaultConstruction(node);
    }

    public Type getType(SchemaNode leaf, TypeDefinition<?> type) {
        Type javaType;
        try {
            javaType = typeProvider.javaTypeForSchemaDefinitionType(
                    type, leaf);
            if (javaType == null)
                throw new IllegalArgumentException("Unknown type received for "
                        + leaf.toString());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Error while resolving type of "
                    + leaf, e);
        }
        return javaType;
    }

    // there is no getType in common interface
    public Type getType(LeafListSchemaNode leaf) {
        Type javaType;
        try {
            javaType = typeProvider.javaTypeForSchemaDefinitionType(
                    leaf.getType(), leaf);
            if (javaType == null)
                throw new IllegalArgumentException(
                        "Unknown type received for  " + leaf.toString());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Error while resolving type of "
                    + leaf, e);
        }
        return javaType;
    }

    public String getJMXParamForBaseType(TypeDefinition<?> baseType) {
        return typeProvider.getConstructorPropertyName(baseType);
    }

    public String getJMXParamForUnionInnerType(TypeDefinition<?> unionInnerType) {
        return typeProvider.getParamNameFromType(unionInnerType);
    }
}
