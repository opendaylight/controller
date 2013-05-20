/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.binding.yang.types;

import static org.opendaylight.controller.yang.model.util.SchemaContextUtil.findDataSchemaNode;
import static org.opendaylight.controller.yang.model.util.SchemaContextUtil.findDataSchemaNodeForRelativeXPath;
import static org.opendaylight.controller.yang.model.util.SchemaContextUtil.resolveModuleFromSchemaPath;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.opendaylight.controller.binding.generator.util.BindingGeneratorUtil;
import org.opendaylight.controller.binding.generator.util.Types;
import org.opendaylight.controller.binding.generator.util.generated.type.builder.EnumerationBuilderImpl;
import org.opendaylight.controller.binding.generator.util.generated.type.builder.GeneratedTOBuilderImpl;
import org.opendaylight.controller.sal.binding.generator.spi.TypeProvider;
import org.opendaylight.controller.sal.binding.model.api.Enumeration;
import org.opendaylight.controller.sal.binding.model.api.GeneratedTransferObject;
import org.opendaylight.controller.sal.binding.model.api.Type;
import org.opendaylight.controller.sal.binding.model.api.type.builder.EnumBuilder;
import org.opendaylight.controller.sal.binding.model.api.type.builder.GeneratedPropertyBuilder;
import org.opendaylight.controller.sal.binding.model.api.type.builder.GeneratedTOBuilder;
import org.opendaylight.controller.sal.binding.model.api.type.builder.GeneratedTypeBuilder;
import org.opendaylight.controller.yang.model.api.DataSchemaNode;
import org.opendaylight.controller.yang.model.api.LeafListSchemaNode;
import org.opendaylight.controller.yang.model.api.LeafSchemaNode;
import org.opendaylight.controller.yang.model.api.Module;
import org.opendaylight.controller.yang.model.api.RevisionAwareXPath;
import org.opendaylight.controller.yang.model.api.SchemaContext;
import org.opendaylight.controller.yang.model.api.TypeDefinition;
import org.opendaylight.controller.yang.model.api.type.EnumTypeDefinition;
import org.opendaylight.controller.yang.model.api.type.EnumTypeDefinition.EnumPair;
import org.opendaylight.controller.yang.model.api.type.IdentityrefTypeDefinition;
import org.opendaylight.controller.yang.model.api.type.LeafrefTypeDefinition;
import org.opendaylight.controller.yang.model.api.type.UnionTypeDefinition;
import org.opendaylight.controller.yang.model.util.ExtendedType;

public class TypeProviderImpl implements TypeProvider {

    private final SchemaContext schemaContext;
    private Map<String, Map<String, GeneratedTransferObject>> genTypeDefsContextMap;
    private final List<GeneratedTransferObject> allTypeDefinitions;

    public TypeProviderImpl(final SchemaContext schemaContext) {
        if (schemaContext == null) {
            throw new IllegalArgumentException("Schema Context cannot be null!");
        }

        this.schemaContext = schemaContext;
        this.genTypeDefsContextMap = new HashMap<String, Map<String, GeneratedTransferObject>>();
        allTypeDefinitions = resolveTypeDefsFromContext();
    }

    public List<GeneratedTransferObject> getGeneratedTypeDefs() {
        return allTypeDefinitions;
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
            final String typedefName = typeDefinition.getQName().getLocalName();
            if (typeDefinition instanceof ExtendedType) {
                final TypeDefinition<?> baseTypeDef = baseTypeDefForExtendedType(typeDefinition);

                if (baseTypeDef instanceof LeafrefTypeDefinition) {
                    final LeafrefTypeDefinition leafref = (LeafrefTypeDefinition) baseTypeDef;
                    returnType = provideTypeForLeafref(leafref);
                } else if (baseTypeDef instanceof IdentityrefTypeDefinition) {

                } else if (baseTypeDef instanceof EnumTypeDefinition) {
                    final EnumTypeDefinition enumTypeDef = (EnumTypeDefinition) baseTypeDef;
                    returnType = resolveEnumFromTypeDefinition(enumTypeDef,
                            typedefName);
                } else {
                    final Module module = resolveModuleFromSchemaPath(schemaContext, typeDefinition
                                    .getPath());

                    if (module != null) {
                        final Map<String, GeneratedTransferObject> genTOs = genTypeDefsContextMap
                                .get(module.getName());
                        if (genTOs != null) {
                            returnType = genTOs.get(typedefName);
                        }
                    }
                }
            } else {
                if (typeDefinition instanceof LeafrefTypeDefinition) {
                    final LeafrefTypeDefinition leafref = (LeafrefTypeDefinition) typeDefinition;
                    returnType = provideTypeForLeafref(leafref);
                } else if (typeDefinition instanceof EnumTypeDefinition) {
                    final EnumTypeDefinition enumTypeDef = (EnumTypeDefinition) typeDefinition;
                    returnType = resolveEnumFromTypeDefinition(enumTypeDef,
                            typedefName);
                } else if (typeDefinition instanceof IdentityrefTypeDefinition) {

                } else {
                    returnType = BaseYangTypes.BASE_YANG_TYPES_PROVIDER
                            .javaTypeForSchemaDefinitionType(typeDefinition);
                }
            }
        }
        return returnType;
    }

    private TypeDefinition<?> baseTypeDefForExtendedType(
            final TypeDefinition<?> extendTypeDef) {
        if (extendTypeDef != null) {
            final TypeDefinition<?> baseTypeDef = extendTypeDef.getBaseType();
            if (baseTypeDef instanceof ExtendedType) {
                baseTypeDefForExtendedType(baseTypeDef);
            } else {
                return baseTypeDef;
            }
        }
        return null;
    }

    public Type baseTypeForExtendedType(final TypeDefinition<?> typeDefinition) {
        Type returnType = null;
        if (typeDefinition != null) {
            final TypeDefinition<?> baseTypeDefinition = baseTypeDefForExtendedType(typeDefinition);

            if (baseTypeDefinition instanceof EnumTypeDefinition) {
                final EnumTypeDefinition enumTypeDef = (EnumTypeDefinition) typeDefinition;
                final String enumName = enumTypeDef.getQName().getLocalName();
                return resolveEnumFromTypeDefinition(enumTypeDef, enumName);
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
                    final Module module = resolveModuleFromSchemaPath(schemaContext, leafrefType.getPath());
                    if (module != null) {
                        final DataSchemaNode dataNode;
                        if (xpath.isAbsolute()) {
                            dataNode = findDataSchemaNode(schemaContext, 
                                    module, xpath);
                        } else {
                            dataNode = findDataSchemaNodeForRelativeXPath(schemaContext, module,
                                            leafrefType, xpath);
                        }
                        returnType = resolveTypeFromDataSchemaNode(dataNode);
                    }
                }
            }
        }
        return returnType;
    }

    private EnumBuilder resolveEnumFromTypeDefinition(
            final EnumTypeDefinition enumTypeDef, final String enumName,
            final GeneratedTypeBuilder typeBuilder) {
        if ((enumTypeDef != null) && (typeBuilder != null)
                && (enumTypeDef.getQName() != null)
                && (enumTypeDef.getQName().getLocalName() != null)) {

            final String enumerationName = BindingGeneratorUtil
                    .parseToClassName(enumName);
            final EnumBuilder enumBuilder = typeBuilder
                    .addEnumeration(enumerationName);

            if (enumBuilder != null) {
                final List<EnumPair> enums = enumTypeDef.getValues();
                if (enums != null) {
                    int listIndex = 0;
                    for (final EnumPair enumPair : enums) {
                        if (enumPair != null) {
                            final String enumPairName = BindingGeneratorUtil
                                    .parseToClassName(enumPair.getName());
                            Integer enumPairValue = enumPair.getValue();

                            if (enumPairValue == null) {
                                enumPairValue = listIndex;
                            }
                            enumBuilder.addValue(enumPairName, enumPairValue);
                            listIndex++;
                        }
                    }
                }
                return enumBuilder;
            }
        }
        return null;
    }

    private Enumeration resolveEnumFromTypeDefinition(
            final EnumTypeDefinition enumTypeDef, final String enumName) {
        if ((enumTypeDef != null) && (enumTypeDef.getQName() != null)
                && (enumTypeDef.getQName().getLocalName() != null)) {

            final String enumerationName = BindingGeneratorUtil
                    .parseToClassName(enumName);

            Module module = resolveModuleFromSchemaPath(schemaContext, enumTypeDef.getPath());
            final String basePackageName = BindingGeneratorUtil
                    .moduleNamespaceToPackageName(module);
            final String packageName = BindingGeneratorUtil
                    .packageNameForGeneratedType(basePackageName,
                            enumTypeDef.getPath());

            final EnumBuilder enumBuilder = new EnumerationBuilderImpl(
                    packageName, enumerationName);

            if (enumBuilder != null) {
                final List<EnumPair> enums = enumTypeDef.getValues();
                if (enums != null) {
                    int listIndex = 0;
                    for (final EnumPair enumPair : enums) {
                        if (enumPair != null) {
                            final String enumPairName = BindingGeneratorUtil
                                    .parseToClassName(enumPair.getName());
                            Integer enumPairValue = enumPair.getValue();

                            if (enumPairValue == null) {
                                enumPairValue = listIndex;
                            }
                            enumBuilder.addValue(enumPairName, enumPairValue);
                            listIndex++;
                        }
                    }
                }
                return enumBuilder.toInstance(null);
            }
        }
        return null;
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

    private List<GeneratedTransferObject> resolveTypeDefsFromContext() {
        final List<GeneratedTransferObject> genTypeDefs = new ArrayList<GeneratedTransferObject>();
        final Set<Module> modules = schemaContext.getModules();
        if (modules != null) {
            for (final Module module : modules) {
                if (module != null) {
                    final String moduleName = module.getName();
                    final String basePackageName = BindingGeneratorUtil
                            .moduleNamespaceToPackageName(module);

                    final Set<TypeDefinition<?>> typeDefinitions = module
                            .getTypeDefinitions();

                    if ((typeDefinitions != null) && (basePackageName != null)) {
                        for (final TypeDefinition<?> typedef : typeDefinitions) {
                            final GeneratedTransferObject genTransObj = toGeneratedTransferObject(
                                    basePackageName, moduleName, typedef);
                            if (genTransObj != null) {
                                genTypeDefs.add(genTransObj);
                            }
                        }
                        // for (final TypeDefinition<?> typedef :
                        // typeDefinitions) {
                        // addUnionGeneratedTypeDefinition(basePackageName,
                        // module.getName(), typedef);
                        // }
                    }
                }
            }
        }
        return genTypeDefs;
    }

    private GeneratedTransferObject toGeneratedTransferObject(
            final String basePackageName, final String moduleName,
            final TypeDefinition<?> typedef) {
        if ((basePackageName != null) && (moduleName != null)
                && (typedef != null) && (typedef.getQName() != null)) {
            final GeneratedTOBuilder genTOBuilder = typedefToTransferObject(
                    basePackageName, typedef);

            final String typedefName = typedef.getQName().getLocalName();
            final String propertyName = BindingGeneratorUtil
                    .parseToValidParamName(typedefName);

            final TypeDefinition<?> baseTypeDefinition = baseTypeDefForExtendedType(typedef);
            if (!(baseTypeDefinition instanceof LeafrefTypeDefinition)
                    && !(baseTypeDefinition instanceof IdentityrefTypeDefinition)) {
                Type returnType = null;
                if (baseTypeDefinition instanceof EnumTypeDefinition) {
                    final EnumTypeDefinition enumTypeDef = (EnumTypeDefinition) baseTypeDefinition;
                    returnType = resolveEnumFromTypeDefinition(enumTypeDef,
                            typedefName);
                } else {
                    returnType = BaseYangTypes.BASE_YANG_TYPES_PROVIDER
                            .javaTypeForSchemaDefinitionType(baseTypeDefinition);
                }

                if (returnType != null) {
                    final GeneratedPropertyBuilder genPropBuilder = genTOBuilder
                            .addProperty(propertyName);

                    genPropBuilder.addReturnType(returnType);
                    genTOBuilder.addEqualsIdentity(genPropBuilder);
                    genTOBuilder.addHashIdentity(genPropBuilder);
                    genTOBuilder.addToStringProperty(genPropBuilder);

                    Map<String, GeneratedTransferObject> transferObjectsMap = genTypeDefsContextMap
                            .get(moduleName);
                    if (transferObjectsMap == null) {
                        transferObjectsMap = new HashMap<String, GeneratedTransferObject>();
                        genTypeDefsContextMap.put(moduleName,
                                transferObjectsMap);
                    }

                    final GeneratedTransferObject transferObject = genTOBuilder
                            .toInstance();
                    if (transferObject != null) {
                        transferObjectsMap.put(typedefName, transferObject);
                        return transferObject;
                    }
                }
            }
        }
        return null;
    }

    private void addUnionGeneratedTypeDefinition(final String basePackageName,
            final String moduleName, final TypeDefinition<?> typedef) {
        if ((basePackageName != null) && (moduleName != null)
                && (typedef != null) && (typedef.getQName() != null)) {
            final TypeDefinition<?> baseTypeDefinition = baseTypeDefForExtendedType(typedef);

            if ((baseTypeDefinition != null)
                    && (baseTypeDefinition instanceof UnionTypeDefinition)) {
                final UnionTypeDefinition unionTypeDef = (UnionTypeDefinition) baseTypeDefinition;

                final List<TypeDefinition<?>> unionTypes = unionTypeDef
                        .getTypes();
                final Map<String, GeneratedTransferObject> genTOsMap = genTypeDefsContextMap
                        .get(moduleName);
                final GeneratedTOBuilder unionGenTransObject = typedefToTransferObject(
                        basePackageName, typedef);
                if ((unionTypes != null) && (genTOsMap != null)
                        && (unionGenTransObject != null)) {
                    for (final TypeDefinition<?> unionType : unionTypes) {
                        final String typeName = unionType.getQName()
                                .getLocalName();
                        final GeneratedTransferObject genTransferObject = genTOsMap
                                .get(typeName);

                        if (genTransferObject != null) {
                            unionGenTransObject
                                    .addProperty(
                                            BindingGeneratorUtil
                                                    .parseToValidParamName(genTransferObject
                                                            .getName()))
                                    .addReturnType(genTransferObject);
                        }
                    }
                    genTOsMap.put(unionTypeDef.getQName().getLocalName(),
                            unionGenTransObject.toInstance());
                }
            }
        }
    }

    private GeneratedTOBuilder typedefToTransferObject(
            final String basePackageName, final TypeDefinition<?> typedef) {

        final String packageName = BindingGeneratorUtil
                .packageNameForGeneratedType(basePackageName, typedef.getPath());
        final String typeDefTOName = typedef.getQName().getLocalName();

        if ((packageName != null) && (typedef != null)
                && (typeDefTOName != null)) {
            final String genTOName = BindingGeneratorUtil
                    .parseToClassName(typeDefTOName);
            final GeneratedTOBuilder newType = new GeneratedTOBuilderImpl(
                    packageName, genTOName);

            return newType;
        }
        return null;
    }
}
