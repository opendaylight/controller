/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.binding.yang.types;

import org.opendaylight.controller.binding.generator.util.Types;
import org.opendaylight.controller.sal.binding.generator.spi.TypeProvider;
import org.opendaylight.controller.sal.binding.model.api.Type;
import org.opendaylight.controller.yang.model.api.DataSchemaNode;
import org.opendaylight.controller.yang.model.api.LeafListSchemaNode;
import org.opendaylight.controller.yang.model.api.LeafSchemaNode;
import org.opendaylight.controller.yang.model.api.Module;
import org.opendaylight.controller.yang.model.api.RevisionAwareXPath;
import org.opendaylight.controller.yang.model.api.SchemaContext;
import org.opendaylight.controller.yang.model.api.TypeDefinition;
import org.opendaylight.controller.yang.model.api.type.IdentityrefTypeDefinition;
import org.opendaylight.controller.yang.model.api.type.LeafrefTypeDefinition;
import org.opendaylight.controller.yang.model.util.ExtendedType;
import org.opendaylight.controller.yang.model.util.Leafref;
import org.opendaylight.controller.yang.model.util.SchemaContextUtil;

public class TypeProviderImpl implements TypeProvider {

    private SchemaContextUtil schemaContextUtil;

    public TypeProviderImpl(SchemaContext schemaContext) {
        schemaContextUtil = new SchemaContextUtil(schemaContext);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.opendaylight.controller.yang.model.type.provider.TypeProvider#
     * javaTypeForYangType(java.lang.String)
     */
    @Override
    public Type javaTypeForYangType(String type) {
        Type t = BaseYangTypes.BASE_YANG_TYPES_PROVIDER
                .javaTypeForYangType(type);
        return t;
    }

    @Override
    public Type javaTypeForSchemaDefinitionType(
            final TypeDefinition<?> typeDefinition) {
        Type returnType = null;
        if (typeDefinition != null) {
            if (typeDefinition instanceof Leafref) {
                final LeafrefTypeDefinition leafref = (LeafrefTypeDefinition) typeDefinition;
                returnType = provideTypeForLeafref(leafref);
            } else if (typeDefinition instanceof IdentityrefTypeDefinition) {

            } else if (typeDefinition instanceof ExtendedType) {
                final TypeDefinition<?> baseType = typeDefinition.getBaseType();
                return javaTypeForSchemaDefinitionType(baseType);
            } else {
                returnType = baseTypeForExtendedType(typeDefinition);
            }
        }
        return returnType;
    }

    public Type baseTypeForExtendedType(final TypeDefinition<?> typeDefinition) {
        Type returnType = null;
        if (typeDefinition != null) {
            if (typeDefinition instanceof ExtendedType) {
                final TypeDefinition<?> extType = typeDefinition.getBaseType();
                return baseTypeForExtendedType(extType);
            } else {
                returnType = BaseYangTypes.BASE_YANG_TYPES_PROVIDER
                        .javaTypeForSchemaDefinitionType(typeDefinition);
            }
        }
        return returnType;
    }

    public Type provideTypeForLeafref(final LeafrefTypeDefinition leafrefType) {
        Type returnType = null;
        if ((leafrefType != null) && (leafrefType.getPathStatement() != null)
                && (leafrefType.getPath() != null)) {

            final RevisionAwareXPath xpath = leafrefType.getPathStatement();
            final String strXPath = xpath.toString();

            if (strXPath != null) {
                if (strXPath.matches(".*//[.* | .*//].*")) {
                    returnType = Types.typeForClass(Object.class);
                } else {
                    final Module module = schemaContextUtil
                            .resolveModuleFromSchemaPath(leafrefType.getPath());
                    if (module != null) {
                        final DataSchemaNode dataNode;
                        if (xpath.isAbsolute()) {
                            dataNode = schemaContextUtil.findDataSchemaNode(
                                    module, xpath);
                        } else {
                            dataNode = schemaContextUtil
                                    .findDataSchemaNodeForRelativeXPath(module,
                                            leafrefType, xpath);
                        }
                        returnType = resolveTypeFromDataSchemaNode(dataNode);
                    }
                }
            }
        }
        return returnType;
    }

    private Type resolveTypeFromDataSchemaNode(final DataSchemaNode dataNode) {
        Type returnType = null;
        if (dataNode != null) {
            if (dataNode instanceof LeafSchemaNode) {
                final LeafSchemaNode leaf = (LeafSchemaNode) dataNode;
                returnType = javaTypeForSchemaDefinitionType(leaf.getType());
            } else if (dataNode instanceof LeafListSchemaNode) {
                final LeafListSchemaNode leafList = (LeafListSchemaNode) dataNode;
                returnType = javaTypeForSchemaDefinitionType(leafList.getType());
            }
        }
        return returnType;
    }
}
