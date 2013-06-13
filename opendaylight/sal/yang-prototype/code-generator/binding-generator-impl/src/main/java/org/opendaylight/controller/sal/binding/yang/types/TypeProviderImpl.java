/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.binding.yang.types;

import org.opendaylight.controller.binding.generator.util.ReferencedTypeImpl;
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
import org.opendaylight.controller.yang.model.api.*;
import org.opendaylight.controller.yang.model.api.type.EnumTypeDefinition;
import org.opendaylight.controller.yang.model.api.type.EnumTypeDefinition.EnumPair;
import org.opendaylight.controller.yang.model.api.type.IdentityrefTypeDefinition;
import org.opendaylight.controller.yang.model.api.type.LeafrefTypeDefinition;
import org.opendaylight.controller.yang.model.api.type.UnionTypeDefinition;
import org.opendaylight.controller.yang.model.util.ExtendedType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.opendaylight.controller.binding.generator.util.BindingGeneratorUtil.*;
import static org.opendaylight.controller.yang.model.util.SchemaContextUtil.*;

public final class TypeProviderImpl implements TypeProvider {

    private final SchemaContext schemaContext;
    private Map<String, Map<String, Type>> genTypeDefsContextMap;
    private final Map<SchemaPath, Type> referencedTypes;

    public TypeProviderImpl(final SchemaContext schemaContext) {
        if (schemaContext == null) {
            throw new IllegalArgumentException("Schema Context cannot be null!");
        }

        this.schemaContext = schemaContext;
        this.genTypeDefsContextMap = new HashMap<>();
        this.referencedTypes = new HashMap<>();
        resolveTypeDefsFromContext();
    }

    public void putReferencedType(final SchemaPath refTypePath,
                                  final Type refType) {
        if (refTypePath == null) {
            throw new IllegalArgumentException("Path reference of " +
                    "Enumeration Type Definition cannot be NULL!");
        }

        if (refType == null) {
            throw new IllegalArgumentException("Reference to Enumeration " +
                    "Type cannot be NULL!");
        }
        referencedTypes.put(refTypePath, refType);
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
        if (typeDefinition == null) {
            throw new IllegalArgumentException("Type Definition cannot be " +
                    "NULL!");
        }
        if (typeDefinition.getQName() == null) {
            throw new IllegalArgumentException("Type Definition cannot have " +
                    "non specified QName (QName cannot be NULL!)");
        }
        if (typeDefinition.getQName().getLocalName() == null) {
            throw new IllegalArgumentException("Type Definitions Local Name " +
                    "cannot be NULL!");
        }
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
                final Module module = findParentModuleForTypeDefinition(schemaContext,
                        typeDefinition);
                if (module != null) {
                    final Map<String, Type> genTOs = genTypeDefsContextMap
                            .get(module.getName());
                    if (genTOs != null) {
                        returnType = genTOs.get(typedefName);
                    }
                    if (returnType == null) {
                        returnType = BaseYangTypes.BASE_YANG_TYPES_PROVIDER
                                .javaTypeForSchemaDefinitionType(baseTypeDef);
                    }
                }
            }
        } else {
            if (typeDefinition instanceof LeafrefTypeDefinition) {
                final LeafrefTypeDefinition leafref = (LeafrefTypeDefinition) typeDefinition;
                returnType = provideTypeForLeafref(leafref);
            } else if (typeDefinition instanceof IdentityrefTypeDefinition) {

            } else {
                returnType = BaseYangTypes.BASE_YANG_TYPES_PROVIDER
                        .javaTypeForSchemaDefinitionType(typeDefinition);
            }
        }
        //TODO: add throw exception when we will be able to resolve ALL yang
        // types!
//        if (returnType == null) {
//            throw new IllegalArgumentException("Type Provider can't resolve " +
//                    "type for specified Type Definition " + typedefName);
//        }
        return returnType;
    }

    public Type generatedTypeForExtendedDefinitionType(
            final TypeDefinition<?> typeDefinition) {
        Type returnType = null;
        if (typeDefinition == null) {
            throw new IllegalArgumentException("Type Definition cannot be " +
                    "NULL!");
        }
        if (typeDefinition.getQName() == null) {
            throw new IllegalArgumentException("Type Definition cannot have " +
                    "non specified QName (QName cannot be NULL!)");
        }
        if (typeDefinition.getQName() == null) {
            throw new IllegalArgumentException("Type Definitions Local Name " +
                    "cannot be NULL!");
        }

        final String typedefName = typeDefinition.getQName().getLocalName();
        if (typeDefinition instanceof ExtendedType) {
            final TypeDefinition<?> baseTypeDef = baseTypeDefForExtendedType(typeDefinition);

            if (!(baseTypeDef instanceof LeafrefTypeDefinition)
                    && !(baseTypeDef instanceof IdentityrefTypeDefinition)) {
                final Module module = findParentModuleForTypeDefinition(schemaContext,
                        typeDefinition);

                if (module != null) {
                    final Map<String, Type> genTOs = genTypeDefsContextMap
                            .get(module.getName());
                    if (genTOs != null) {
                        returnType = genTOs.get(typedefName);
                    }
                }
            }
        }
        return returnType;
    }

    private TypeDefinition<?> baseTypeDefForExtendedType(
            final TypeDefinition<?> extendTypeDef) {
        if (extendTypeDef == null) {
            throw new IllegalArgumentException("Type Definiition reference " +
                    "cannot be NULL!");
        }
        final TypeDefinition<?> baseTypeDef = extendTypeDef.getBaseType();
        if (baseTypeDef instanceof ExtendedType) {
            return baseTypeDefForExtendedType(baseTypeDef);
        } else {
            return baseTypeDef;
        }

    }

    public Type provideTypeForLeafref(final LeafrefTypeDefinition leafrefType) {
        Type returnType = null;
        if (leafrefType == null) {
            throw new IllegalArgumentException("Leafref Type Definition " +
                    "reference cannot be NULL!");
        }

        if (leafrefType.getPathStatement() == null) {
            throw new IllegalArgumentException("The Path Statement for " +
                    "Leafref Type Definition cannot be NULL!");
        }

        final RevisionAwareXPath xpath = leafrefType.getPathStatement();
        final String strXPath = xpath.toString();

        if (strXPath != null) {
            if (strXPath.matches(".*//[.* | .*//].*")) {
                returnType = Types.typeForClass(Object.class);
            } else {
                final Module module = findParentModuleForTypeDefinition(schemaContext, leafrefType);
                if (module != null) {
                    final DataSchemaNode dataNode;
                    if (xpath.isAbsolute()) {
                        dataNode = findDataSchemaNode(schemaContext, module,
                                xpath);
                    } else {
                        dataNode = findDataSchemaNodeForRelativeXPath(schemaContext,
                                module, leafrefType, xpath);
                    }

                    if (leafContainsEnumDefinition(dataNode)) {
                        returnType = referencedTypes.get(dataNode.getPath());
                    } else if (leafListContainsEnumDefinition(dataNode)) {
                        returnType = Types.listTypeFor(referencedTypes.get(
                                dataNode.getPath()));
                    } else {
                        returnType = resolveTypeFromDataSchemaNode(dataNode);
                    }
                }
            }
        }
        return returnType;
    }

    private boolean leafContainsEnumDefinition(final DataSchemaNode dataNode) {
        if (dataNode instanceof LeafSchemaNode) {
            final LeafSchemaNode leaf = (LeafSchemaNode) dataNode;
            if (leaf.getType() instanceof EnumTypeDefinition) {
                return true;
            }
        }
        return false;
    }

    private boolean leafListContainsEnumDefinition(
            final DataSchemaNode dataNode) {
        if (dataNode instanceof LeafListSchemaNode) {
            final LeafListSchemaNode leafList = (LeafListSchemaNode) dataNode;
            if (leafList.getType() instanceof EnumTypeDefinition) {
                return true;
            }
        }
        return false;
    }

    private Enumeration resolveEnumFromTypeDefinition(
            final EnumTypeDefinition enumTypeDef, final String enumName) {
        if (enumTypeDef == null) {
            throw new IllegalArgumentException("EnumTypeDefinition reference " +
                    "cannot be NULL!");
        }
        if (enumTypeDef.getValues() == null) {
            throw new IllegalArgumentException("EnumTypeDefinition MUST " +
                    "contain at least ONE value definition!");
        }
        if (enumTypeDef.getQName() == null) {
            throw new IllegalArgumentException("EnumTypeDefinition MUST " +
                    "contain NON-NULL QName!");
        }
        if (enumTypeDef.getQName().getLocalName() == null) {
            throw new IllegalArgumentException("Local Name in " +
                    "EnumTypeDefinition QName cannot be NULL!");
        }

        final String enumerationName = parseToClassName(enumName);

        Module module = findParentModuleForTypeDefinition(schemaContext, enumTypeDef);
        final String basePackageName = moduleNamespaceToPackageName(module);

        final EnumBuilder enumBuilder = new EnumerationBuilderImpl(
                basePackageName, enumerationName);
        updateEnumPairsFromEnumTypeDef(enumTypeDef, enumBuilder);
        return enumBuilder.toInstance(null);
    }

    private EnumBuilder resolveInnerEnumFromTypeDefinition(
            final EnumTypeDefinition enumTypeDef, final String enumName,
            final GeneratedTypeBuilder typeBuilder) {
        if (enumTypeDef == null) {
            throw new IllegalArgumentException("EnumTypeDefinition reference " +
                    "cannot be NULL!");
        }
        if (enumTypeDef.getValues() == null) {
            throw new IllegalArgumentException("EnumTypeDefinition MUST " +
                    "contain at least ONE value definition!");
        }
        if (enumTypeDef.getQName() == null) {
            throw new IllegalArgumentException("EnumTypeDefinition MUST " +
                    "contain NON-NULL QName!");
        }
        if (enumTypeDef.getQName().getLocalName() == null) {
            throw new IllegalArgumentException("Local Name in " +
                    "EnumTypeDefinition QName cannot be NULL!");
        }
        if (typeBuilder == null) {
            throw new IllegalArgumentException("Generated Type Builder " +
                    "reference cannot be NULL!");
        }

        final String enumerationName = parseToClassName(enumName);
        final EnumBuilder enumBuilder = typeBuilder
                .addEnumeration(enumerationName);

        updateEnumPairsFromEnumTypeDef(enumTypeDef, enumBuilder);

        return enumBuilder;
    }

    private void updateEnumPairsFromEnumTypeDef(
            final EnumTypeDefinition enumTypeDef,
            final EnumBuilder enumBuilder) {
        if (enumBuilder != null) {
            final List<EnumPair> enums = enumTypeDef.getValues();
            if (enums != null) {
                int listIndex = 0;
                for (final EnumPair enumPair : enums) {
                    if (enumPair != null) {
                        final String enumPairName = parseToClassName(enumPair
                                .getName());
                        Integer enumPairValue = enumPair.getValue();

                        if (enumPairValue == null) {
                            enumPairValue = listIndex;
                        }
                        enumBuilder.addValue(enumPairName, enumPairValue);
                        listIndex++;
                    }
                }
            }
        }
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

    private void resolveTypeDefsFromContext() {
        final Set<Module> modules = schemaContext.getModules();
        if (modules == null) {
            throw new IllegalArgumentException("Sef of Modules cannot be " +
                    "NULL!");
        }
        for (final Module module : modules) {
            if (module == null) {
                continue;
            }
            final String moduleName = module.getName();
            final String basePackageName = moduleNamespaceToPackageName(module);

            final Set<TypeDefinition<?>> typeDefinitions = module
                    .getTypeDefinitions();

            final Map<String, Type> typeMap = new HashMap<>();
            genTypeDefsContextMap.put(moduleName, typeMap);

            if ((typeDefinitions != null) && (basePackageName != null)) {
                for (final TypeDefinition<?> typedef : typeDefinitions) {
                    typedefToGeneratedType(basePackageName, moduleName, typedef);
                }
                final List<ExtendedType> extUnions = UnionDependencySort
                        .sort(typeDefinitions);
                for (final ExtendedType extUnionType : extUnions) {
                    addUnionGeneratedTypeDefinition(basePackageName, extUnionType);
                }
            }
        }
    }

    private Type typedefToGeneratedType(final String basePackageName,
                                        final String moduleName, final TypeDefinition<?> typedef) {
        if ((basePackageName != null) && (moduleName != null)
                && (typedef != null) && (typedef.getQName() != null)) {


            final String typedefName = typedef.getQName().getLocalName();
            final TypeDefinition<?> baseTypeDefinition = baseTypeDefForExtendedType(typedef);
            if (!(baseTypeDefinition instanceof LeafrefTypeDefinition)
                    && !(baseTypeDefinition instanceof IdentityrefTypeDefinition)) {
                Type returnType;
                if (baseTypeDefinition instanceof EnumTypeDefinition) {
                    final EnumTypeDefinition enumTypeDef = (EnumTypeDefinition) baseTypeDefinition;
                    returnType = resolveEnumFromTypeDefinition(enumTypeDef,
                            typedefName);

                } else {
                    final Type javaType = BaseYangTypes.BASE_YANG_TYPES_PROVIDER
                            .javaTypeForSchemaDefinitionType(baseTypeDefinition);

                    returnType = wrapJavaTypeIntoTO(basePackageName, typedef,
                            javaType);
                }
                if (returnType != null) {
                    final Map<String, Type> typeMap = genTypeDefsContextMap.get
                            (moduleName);
                    if (typeMap != null) {
                        typeMap.put(typedefName, returnType);
                    }
                    return returnType;
                }
            }
        }
        return null;
    }

    private GeneratedTransferObject wrapJavaTypeIntoTO(
            final String basePackageName, final TypeDefinition<?> typedef,
            final Type javaType) {
        if (javaType != null) {
            final String typedefName = typedef.getQName().getLocalName();
            final String propertyName = parseToValidParamName(typedefName);

            final GeneratedTOBuilder genTOBuilder = typedefToTransferObject(
                    basePackageName, typedef);

            final GeneratedPropertyBuilder genPropBuilder = genTOBuilder
                    .addProperty(propertyName);

            genPropBuilder.addReturnType(javaType);
            genTOBuilder.addEqualsIdentity(genPropBuilder);
            genTOBuilder.addHashIdentity(genPropBuilder);
            genTOBuilder.addToStringProperty(genPropBuilder);
            return genTOBuilder.toInstance();
        }
        return null;
    }

    private void addUnionGeneratedTypeDefinition(final String basePackageName,
                                                 final TypeDefinition<?> typedef) {
        if (basePackageName == null) {
            throw new IllegalArgumentException("Base Package Name cannot be " +
                    "NULL!");
        }
        if (typedef == null) {
            throw new IllegalArgumentException("Type Definition cannot be " +
                    "NULL!");
        }
        if (typedef.getQName() == null) {
            throw new IllegalArgumentException("Type Definition cannot have " +
                    "non specified QName (QName cannot be NULL!)");
        }

        final TypeDefinition<?> baseTypeDefinition = typedef.getBaseType();
        if ((baseTypeDefinition != null)
                && (baseTypeDefinition instanceof UnionTypeDefinition)) {
            final UnionTypeDefinition unionTypeDef = (UnionTypeDefinition) baseTypeDefinition;
            final List<TypeDefinition<?>> unionTypes = unionTypeDef
                    .getTypes();
            final Module parentModule = findParentModuleForTypeDefinition(schemaContext,
                    typedef);

            Map<String, Type> genTOsMap = null;
            if (parentModule != null && parentModule.getName() != null) {
                genTOsMap = genTypeDefsContextMap.get(parentModule.getName());
            }

            final GeneratedTOBuilder unionGenTransObject = typedefToTransferObject(
                    basePackageName, typedef);
            if ((unionTypes != null) && (unionGenTransObject != null)) {
                for (final TypeDefinition<?> unionType : unionTypes) {
                    final String typeName = unionType.getQName()
                            .getLocalName();
                    if (unionType instanceof ExtendedType) {
                        final Module unionTypeModule = findParentModuleForTypeDefinition(schemaContext,
                                unionType);
                        if (unionTypeModule != null && unionTypeModule.getName() != null) {
                            final Map<String, Type> innerGenTOs = genTypeDefsContextMap
                                    .get(unionTypeModule.getName());

                            final GeneratedTransferObject genTransferObject =
                                    (GeneratedTransferObject) innerGenTOs.get(typeName);
                            if (genTransferObject != null) {
                                updateUnionTypeAsProperty(unionGenTransObject,
                                        genTransferObject,
                                        genTransferObject.getName());
                            }
                        }
                    } else if (unionType instanceof EnumTypeDefinition) {
                        final EnumBuilder
                                enumBuilder = resolveInnerEnumFromTypeDefinition(
                                (EnumTypeDefinition) unionType, typeName,
                                unionGenTransObject);
                        final Type enumRefType = new ReferencedTypeImpl(
                                enumBuilder.getPackageName(),
                                enumBuilder.getName());
                        updateUnionTypeAsProperty(unionGenTransObject,
                                enumRefType, typeName);
                    } else {
                        final Type javaType = BaseYangTypes.BASE_YANG_TYPES_PROVIDER
                                .javaTypeForSchemaDefinitionType(unionType);
                        if (javaType != null) {
                            updateUnionTypeAsProperty(unionGenTransObject,
                                    javaType, typeName);
                        }
                    }
                }
                genTOsMap.put(typedef.getQName().getLocalName(),
                        unionGenTransObject.toInstance());
            }
        }
    }

    private void updateUnionTypeAsProperty(
            final GeneratedTOBuilder unionGenTransObject, final Type type,
            final String propertyName) {
        if (unionGenTransObject != null && type != null) {
            final GeneratedPropertyBuilder propBuilder =
                    unionGenTransObject.addProperty(parseToValidParamName(
                            propertyName));
            propBuilder.addReturnType(type);
            propBuilder.setReadOnly(false);

            if (!(type instanceof Enumeration)) {
                unionGenTransObject.addEqualsIdentity(propBuilder);
                unionGenTransObject.addHashIdentity(propBuilder);
                unionGenTransObject.addToStringProperty(propBuilder);
            }
        }
    }

    private GeneratedTOBuilder typedefToTransferObject(
            final String basePackageName, final TypeDefinition<?> typedef) {

        final String packageName = packageNameForGeneratedType(basePackageName,
                typedef.getPath());
        final String typeDefTOName = typedef.getQName().getLocalName();

        if ((packageName != null) && (typedef != null)
                && (typeDefTOName != null)) {
            final String genTOName = parseToClassName(typeDefTOName);
            final GeneratedTOBuilder newType = new GeneratedTOBuilderImpl(
                    packageName, genTOName);

            return newType;
        }
        return null;
    }
}
