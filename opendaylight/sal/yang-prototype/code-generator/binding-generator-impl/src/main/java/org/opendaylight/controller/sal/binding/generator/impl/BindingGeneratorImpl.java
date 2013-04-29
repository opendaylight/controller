/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.binding.generator.impl;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.opendaylight.controller.binding.generator.util.BindingGeneratorUtil;
import org.opendaylight.controller.binding.generator.util.Types;
import org.opendaylight.controller.binding.generator.util.generated.type.builder.GeneratedTypeBuilderImpl;
import org.opendaylight.controller.sal.binding.generator.api.BindingGenerator;
import org.opendaylight.controller.sal.binding.generator.spi.TypeProvider;
import org.opendaylight.controller.sal.binding.model.api.GeneratedTransferObject;
import org.opendaylight.controller.sal.binding.model.api.GeneratedType;
import org.opendaylight.controller.sal.binding.model.api.Type;
import org.opendaylight.controller.sal.binding.model.api.type.builder.EnumBuilder;
import org.opendaylight.controller.sal.binding.model.api.type.builder.GeneratedPropertyBuilder;
import org.opendaylight.controller.sal.binding.model.api.type.builder.GeneratedTOBuilder;
import org.opendaylight.controller.sal.binding.model.api.type.builder.GeneratedTypeBuilder;
import org.opendaylight.controller.sal.binding.model.api.type.builder.MethodSignatureBuilder;
import org.opendaylight.controller.sal.binding.yang.types.TypeProviderImpl;
import org.opendaylight.controller.yang.common.QName;
import org.opendaylight.controller.yang.model.api.ContainerSchemaNode;
import org.opendaylight.controller.yang.model.api.DataSchemaNode;
import org.opendaylight.controller.yang.model.api.LeafListSchemaNode;
import org.opendaylight.controller.yang.model.api.LeafSchemaNode;
import org.opendaylight.controller.yang.model.api.ListSchemaNode;
import org.opendaylight.controller.yang.model.api.Module;
import org.opendaylight.controller.yang.model.api.NotificationDefinition;
import org.opendaylight.controller.yang.model.api.RpcDefinition;
import org.opendaylight.controller.yang.model.api.SchemaContext;
import org.opendaylight.controller.yang.model.api.SchemaPath;
import org.opendaylight.controller.yang.model.api.TypeDefinition;
import org.opendaylight.controller.yang.model.api.type.EnumTypeDefinition;
import org.opendaylight.controller.yang.model.api.type.EnumTypeDefinition.EnumPair;
import org.opendaylight.controller.yang.model.util.DataNodeIterator;
import org.opendaylight.controller.yang.model.util.ExtendedType;

public class BindingGeneratorImpl implements BindingGenerator {

    private static final String[] SET_VALUES = new String[] { "abstract",
            "assert", "boolean", "break", "byte", "case", "catch", "char",
            "class", "const", "continue", "default", "double", "do", "else",
            "enum", "extends", "false", "final", "finally", "float", "for",
            "goto", "if", "implements", "import", "instanceof", "int",
            "interface", "long", "native", "new", "null", "package", "private",
            "protected", "public", "return", "short", "static", "strictfp",
            "super", "switch", "synchronized", "this", "throw", "throws",
            "transient", "true", "try", "void", "volatile", "while" };

    public static final Set<String> JAVA_RESERVED_WORDS = new HashSet<String>(
            Arrays.asList(SET_VALUES));

    private static Calendar calendar = new GregorianCalendar();
    private Map<String, Map<String, GeneratedTypeBuilder>> genTypeBuilders;
    private TypeProvider typeProvider;
    private String basePackageName;

    public BindingGeneratorImpl() {
        super();
    }

    private static String validatePackage(final String packageName) {
        if (packageName != null) {
            final String[] packNameParts = packageName.split("\\.");
            if (packNameParts != null) {
                final StringBuilder builder = new StringBuilder();
                for (int i = 0; i < packNameParts.length; ++i) {
                    if (JAVA_RESERVED_WORDS.contains(packNameParts[i])) {
                        packNameParts[i] = "_" + packNameParts[i];
                    }
                    if (i > 0) {
                        builder.append(".");
                    }
                    builder.append(packNameParts[i]);
                }
                return builder.toString();
            }
        }
        return packageName;
    }

    @Override
    public List<Type> generateTypes(final SchemaContext context) {
        final List<Type> genTypes = new ArrayList<Type>();

        if (context != null) {
            typeProvider = new TypeProviderImpl(context);
            final Set<Module> modules = context.getModules();
            
            if (modules != null) {
                for (final Module module : modules) {
                    final DataNodeIterator moduleIterator = new DataNodeIterator(
                            module);

                    genTypeBuilders = new HashMap<String, Map<String, GeneratedTypeBuilder>>();
                    final List<ContainerSchemaNode> schemaContainers = moduleIterator
                            .allContainers();
                    final List<ListSchemaNode> schemaLists = moduleIterator
                            .allLists();

                    basePackageName = BindingGeneratorUtil
                            .moduleNamespaceToPackageName(
                                    module.getNamespace(),
                                    module.getYangVersion());

                    if (schemaContainers.size() > 0) {
                        for (final ContainerSchemaNode container : schemaContainers) {
                            genTypes.add(containerToGenType(container));
                        }
                    }
                    if (schemaLists.size() > 0) {
                        for (final ListSchemaNode list : schemaLists) {
                            genTypes.addAll(listToGenType(list));
                        }
                    }

                    final GeneratedType genDataType = moduleToDataType(module);
                    final GeneratedType genRpcType = rpcMethodsToGenType(module);
                    final GeneratedType genNotifyType = notifycationsToGenType(module);

                    if (genDataType != null) {
                        genTypes.add(genDataType);
                    }
                    if (genRpcType != null) {
                        genTypes.add(genRpcType);
                    }
                    if (genNotifyType != null) {
                        genTypes.add(genNotifyType);
                    }
                }
                
                //FIXME this is quick add of typedefs to generated types from type provider
                genTypes.addAll(((TypeProviderImpl)typeProvider).getGeneratedTypeDefs());
            }
        }
        return genTypes;
    }

    private GeneratedType moduleToDataType(final Module module) {
        if (module != null) {
            final Set<TypeDefinition<?>> typeDefinitions = module
                    .getTypeDefinitions();
            final GeneratedTypeBuilder moduleDataTypeBuilder = moduleTypeBuilder(
                    module, "Data");

            if (moduleDataTypeBuilder != null) {
                if (typeDefinitions != null) {
                    for (final TypeDefinition<?> typedef : typeDefinitions) {
                        if (isDerivedFromEnumerationType(typedef)) {
                            final EnumTypeDefinition enumBaseType = enumTypeDefFromExtendedType(typedef);
                            resolveEnumFromTypeDefinition(enumBaseType, typedef
                                    .getQName().getLocalName(),
                                    moduleDataTypeBuilder);
                        }
                    }
                }

                final Set<DataSchemaNode> dataNodes = module.getChildNodes();
                resolveTypesFromDataSchemaNode(moduleDataTypeBuilder, dataNodes);
                return moduleDataTypeBuilder.toInstance();
            }
        }
        return null;
    }

    private boolean isDerivedFromEnumerationType(
            final TypeDefinition<?> typeDefinition) {
        if (typeDefinition != null) {
            if (typeDefinition.getBaseType() instanceof EnumTypeDefinition) {
                return true;
            } else if (typeDefinition.getBaseType() instanceof ExtendedType) {
                return isDerivedFromEnumerationType(typeDefinition
                        .getBaseType());
            }
        }
        return false;
    }

    private EnumTypeDefinition enumTypeDefFromExtendedType(
            final TypeDefinition<?> typeDefinition) {
        if (typeDefinition != null) {
            if (typeDefinition.getBaseType() instanceof EnumTypeDefinition) {
                return (EnumTypeDefinition) typeDefinition.getBaseType();
            } else if (typeDefinition.getBaseType() instanceof ExtendedType) {
                return enumTypeDefFromExtendedType(typeDefinition.getBaseType());
            }
        }
        return null;
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

    private GeneratedTypeBuilder moduleTypeBuilder(final Module module,
            final String postfix) {
        if (module != null) {
            String packageName = resolveBasePackageName(module.getNamespace(),
                    module.getYangVersion());
            final String moduleName = BindingGeneratorUtil
                    .parseToClassName(module.getName()) + postfix;

            if (packageName != null) {
                packageName = validatePackage(packageName);
                return new GeneratedTypeBuilderImpl(packageName, moduleName);
            }
        }
        return null;
    }

    private GeneratedType rpcMethodsToGenType(final Module module) {
        if (module != null) {
            final Set<RpcDefinition> rpcDefinitions = module.getRpcs();
            //TODO: add implementation
            if ((rpcDefinitions != null) && !rpcDefinitions.isEmpty()) {
                final GeneratedTypeBuilder rpcTypeBuilder = moduleTypeBuilder(
                        module, "Rpc");

                for (final RpcDefinition rpc : rpcDefinitions) {
                    if (rpc != null) {

                    }
                }
            }
        }
        return null;
    }

    private GeneratedType notifycationsToGenType(final Module module) {
        if (module != null) {
            final Set<NotificationDefinition> notifications = module
                    .getNotifications();
            //TODO: add implementation
            if ((notifications != null) && !notifications.isEmpty()) {
                final GeneratedTypeBuilder notifyTypeBuilder = moduleTypeBuilder(
                        module, "Notification");

                for (final NotificationDefinition notification : notifications) {
                    if (notification != null) {

                    }
                }
            }
        }
        return null;
    }

    // private String resolveGeneratedTypePackageName(final SchemaPath
    // schemaPath) {
    // final StringBuilder builder = new StringBuilder();
    // builder.append(basePackageName);
    // if ((schemaPath != null) && (schemaPath.getPath() != null)) {
    // final List<QName> pathToNode = schemaPath.getPath();
    // final int traversalSteps = (pathToNode.size() - 1);
    // for (int i = 0; i < traversalSteps; ++i) {
    // builder.append(".");
    // String nodeLocalName = pathToNode.get(i).getLocalName();
    //
    // // TODO: refactor with use of BindingGeneratorUtil class
    // nodeLocalName = nodeLocalName.replace(":", ".");
    // nodeLocalName = nodeLocalName.replace("-", ".");
    // builder.append(nodeLocalName);
    // }
    // return validatePackage(builder.toString());
    // }
    // return null;
    // }

    private GeneratedType containerToGenType(ContainerSchemaNode container) {
        if (container == null) {
            return null;
        }
        final Set<DataSchemaNode> schemaNodes = container.getChildNodes();
        final GeneratedTypeBuilder typeBuilder = addRawInterfaceDefinition(container);

        resolveTypesFromDataSchemaNode(typeBuilder, schemaNodes);
        return typeBuilder.toInstance();
    }

    private GeneratedTypeBuilder resolveTypesFromDataSchemaNode(
            final GeneratedTypeBuilder typeBuilder,
            final Set<DataSchemaNode> schemaNodes) {

        if ((schemaNodes != null) && (typeBuilder != null)) {
            for (final DataSchemaNode node : schemaNodes) {
                if (node instanceof LeafSchemaNode) {
                    resolveLeafSchemaNodeAsMethod(typeBuilder,
                            (LeafSchemaNode) node);
                } else if (node instanceof LeafListSchemaNode) {
                    resolveLeafListSchemaNode(typeBuilder,
                            (LeafListSchemaNode) node);

                } else if (node instanceof ContainerSchemaNode) {
                    resolveContainerSchemaNode(typeBuilder,
                            (ContainerSchemaNode) node);
                } else if (node instanceof ListSchemaNode) {
                    resolveListSchemaNode(typeBuilder, (ListSchemaNode) node);
                }
            }
        }
        return typeBuilder;
    }

    private boolean resolveLeafSchemaNodeAsMethod(
            final GeneratedTypeBuilder typeBuilder, final LeafSchemaNode leaf) {
        if ((leaf != null) && (typeBuilder != null)) {
            final String leafName = leaf.getQName().getLocalName();
            String leafDesc = leaf.getDescription();
            if (leafDesc == null) {
                leafDesc = "";
            }

            if (leafName != null) {
                final TypeDefinition<?> typeDef = leaf.getType();

                Type type = null;
                if (!(typeDef instanceof EnumTypeDefinition)
                        && !isDerivedFromEnumerationType(typeDef)) {
                    type = typeProvider
                            .javaTypeForSchemaDefinitionType(typeDef);
                } else {
                    if (isImported(leaf.getPath(), typeDef.getPath())) {
                        // TODO: resolving of imported enums as references to
                        // GeneratedTypeData interface
                    } else {
                        final EnumTypeDefinition enumTypeDef = enumTypeDefFromExtendedType(typeDef);
                        final EnumBuilder enumBuilder = resolveEnumFromTypeDefinition(
                                enumTypeDef, leafName, typeBuilder);

                        if (enumBuilder != null) {
                            type = new ReferencedTypeImpl(
                                    enumBuilder.getPackageName(),
                                    enumBuilder.getName());
                        }
                    }
                }

                constructGetter(typeBuilder, leafName, leafDesc, type);
                if (!leaf.isConfiguration()) {
                    constructSetter(typeBuilder, leafName, leafDesc, type);
                }
                return true;
            }
        }
        return false;
    }

    private boolean isImported(final SchemaPath leafPath,
            final SchemaPath typeDefPath) {
        if ((leafPath != null) && (leafPath.getPath() != null)
                && (typeDefPath != null) && (typeDefPath.getPath() != null)) {

            final QName leafPathQName = leafPath.getPath().get(0);
            final QName typePathQName = typeDefPath.getPath().get(0);

            if ((leafPathQName != null)
                    && (leafPathQName.getNamespace() != null)
                    && (typePathQName != null)
                    && (typePathQName.getNamespace() != null)) {

                return !leafPathQName.getNamespace().equals(
                        typePathQName.getNamespace());
            }
        }
        return false;
    }

    private boolean resolveLeafSchemaNodeAsProperty(
            final GeneratedTOBuilder toBuilder, final LeafSchemaNode leaf,
            boolean isReadOnly) {
        if ((leaf != null) && (toBuilder != null)) {
            final String leafName = leaf.getQName().getLocalName();
            String leafDesc = leaf.getDescription();
            if (leafDesc == null) {
                leafDesc = "";
            }

            if (leafName != null) {
                final TypeDefinition<?> typeDef = leaf.getType();

                // TODO: properly resolve enum types
                final Type javaType = typeProvider
                        .javaTypeForSchemaDefinitionType(typeDef);

                final GeneratedPropertyBuilder propBuilder = toBuilder
                        .addProperty(BindingGeneratorUtil
                                .parseToClassName(leafName));

                propBuilder.setReadOnly(isReadOnly);
                propBuilder.addReturnType(javaType);
                propBuilder.addComment(leafDesc);

                toBuilder.addEqualsIdentity(propBuilder);
                toBuilder.addHashIdentity(propBuilder);
                toBuilder.addToStringProperty(propBuilder);

                return true;
            }
        }
        return false;
    }

    private boolean resolveLeafListSchemaNode(
            final GeneratedTypeBuilder typeBuilder,
            final LeafListSchemaNode node) {
        if ((node != null) && (typeBuilder != null)) {
            final String nodeName = node.getQName().getLocalName();
            String nodeDesc = node.getDescription();
            if (nodeDesc == null) {
                nodeDesc = "";
            }

            if (nodeName != null) {
                final TypeDefinition<?> type = node.getType();
                final Type listType = Types.listTypeFor(typeProvider
                        .javaTypeForSchemaDefinitionType(type));

                constructGetter(typeBuilder, nodeName, nodeDesc, listType);
                if (!node.isConfiguration()) {
                    constructSetter(typeBuilder, nodeName, nodeDesc, listType);
                }
                return true;
            }
        }
        return false;
    }

    private boolean resolveContainerSchemaNode(
            final GeneratedTypeBuilder typeBuilder,
            final ContainerSchemaNode node) {
        if ((node != null) && (typeBuilder != null)) {
            final String nodeName = node.getQName().getLocalName();

            if (nodeName != null) {
                final GeneratedTypeBuilder rawGenType = addRawInterfaceDefinition(node);
                constructGetter(typeBuilder, nodeName, "", rawGenType);

                return true;
            }
        }
        return false;
    }

    private boolean resolveListSchemaNode(
            final GeneratedTypeBuilder typeBuilder, final ListSchemaNode node) {
        if ((node != null) && (typeBuilder != null)) {
            final String nodeName = node.getQName().getLocalName();

            if (nodeName != null) {
                final GeneratedTypeBuilder rawGenType = addRawInterfaceDefinition(node);
                constructGetter(typeBuilder, nodeName, "",
                        Types.listTypeFor(rawGenType));
                if (!node.isConfiguration()) {
                    constructSetter(typeBuilder, nodeName, "",
                            Types.listTypeFor(rawGenType));
                }
                return true;
            }
        }
        return false;
    }

    private GeneratedTypeBuilder addRawInterfaceDefinition(
            final DataSchemaNode schemaNode) {
        if (schemaNode == null) {
            return null;
        }

        final String packageName = BindingGeneratorUtil
                .packageNameForGeneratedType(basePackageName,
                        schemaNode.getPath());
        final String schemaNodeName = schemaNode.getQName().getLocalName();

        if ((packageName != null) && (schemaNode != null)
                && (schemaNodeName != null)) {
            final String genTypeName = BindingGeneratorUtil
                    .parseToClassName(schemaNodeName);
            final GeneratedTypeBuilder newType = new GeneratedTypeBuilderImpl(
                    packageName, genTypeName);

            if (!genTypeBuilders.containsKey(packageName)) {
                final Map<String, GeneratedTypeBuilder> builders = new HashMap<String, GeneratedTypeBuilder>();
                builders.put(genTypeName, newType);
                genTypeBuilders.put(packageName, builders);
            } else {
                final Map<String, GeneratedTypeBuilder> builders = genTypeBuilders
                        .get(packageName);
                if (!builders.containsKey(genTypeName)) {
                    builders.put(genTypeName, newType);
                }
            }
            return newType;
        }
        return null;
    }

    private String getterMethodName(final String methodName) {
        final StringBuilder method = new StringBuilder();
        method.append("get");
        method.append(BindingGeneratorUtil.parseToClassName(methodName));
        return method.toString();
    }

    private String setterMethodName(final String methodName) {
        final StringBuilder method = new StringBuilder();
        method.append("set");
        method.append(BindingGeneratorUtil.parseToClassName(methodName));
        return method.toString();
    }

    private MethodSignatureBuilder constructGetter(
            final GeneratedTypeBuilder interfaceBuilder,
            final String schemaNodeName, final String comment,
            final Type returnType) {
        final MethodSignatureBuilder getMethod = interfaceBuilder
                .addMethod(getterMethodName(schemaNodeName));

        getMethod.addComment(comment);
        getMethod.addReturnType(returnType);

        return getMethod;
    }

    private MethodSignatureBuilder constructSetter(
            final GeneratedTypeBuilder interfaceBuilder,
            final String schemaNodeName, final String comment,
            final Type parameterType) {
        final MethodSignatureBuilder setMethod = interfaceBuilder
                .addMethod(setterMethodName(schemaNodeName));

        setMethod.addComment(comment);
        setMethod.addParameter(parameterType,
                BindingGeneratorUtil.parseToValidParamName(schemaNodeName));
        setMethod.addReturnType(Types.voidType());

        return setMethod;
    }

    private String resolveBasePackageName(final URI moduleNamespace,
            final String yangVersion) {
        final StringBuilder packageNameBuilder = new StringBuilder();

        packageNameBuilder.append("org.opendaylight.yang.gen.v");
        packageNameBuilder.append(yangVersion);
        packageNameBuilder.append(".rev");
        packageNameBuilder.append(calendar.get(Calendar.YEAR));
        packageNameBuilder.append((calendar.get(Calendar.MONTH) + 1));
        packageNameBuilder.append(calendar.get(Calendar.DAY_OF_MONTH));
        packageNameBuilder.append(".");

        String namespace = moduleNamespace.toString();
        namespace = namespace.replace(":", ".");
        namespace = namespace.replace("-", ".");

        packageNameBuilder.append(namespace);

        return packageNameBuilder.toString();
    }

    private List<Type> listToGenType(final ListSchemaNode list) {
        if (list == null) {
            return null;
        }
        final GeneratedTypeBuilder typeBuilder = resolveListTypeBuilder(list);
        final List<String> listKeys = listKeys(list);
        GeneratedTOBuilder genTOBuilder = null;
        if (listKeys.size() > 0) {
            genTOBuilder = resolveListKey(list);
        }

        final Set<DataSchemaNode> schemaNodes = list.getChildNodes();
        for (final DataSchemaNode node : schemaNodes) {

            if (node instanceof LeafSchemaNode) {
                final LeafSchemaNode leaf = (LeafSchemaNode) node;
                if (!isPartOfListKey(leaf, listKeys)) {
                    resolveLeafSchemaNodeAsMethod(typeBuilder, leaf);
                } else {
                    resolveLeafSchemaNodeAsProperty(genTOBuilder, leaf, true);
                }
            } else if (node instanceof LeafListSchemaNode) {
                resolveLeafListSchemaNode(typeBuilder,
                        (LeafListSchemaNode) node);
            } else if (node instanceof ContainerSchemaNode) {
                resolveContainerSchemaNode(typeBuilder,
                        (ContainerSchemaNode) node);
            } else if (node instanceof ListSchemaNode) {
                resolveListSchemaNode(typeBuilder, (ListSchemaNode) node);
            }
        }

        final List<Type> genTypes = new ArrayList<Type>();
        if (genTOBuilder != null) {
            final GeneratedTransferObject genTO = genTOBuilder.toInstance();
            constructGetter(typeBuilder, genTO.getName(),
                    "Returns Primary Key of Yang List Type", genTO);
            genTypes.add(genTO);
        }
        genTypes.add(typeBuilder.toInstance());
        return genTypes;
    }

    /**
     * @param list
     * @return
     */
    private GeneratedTOBuilder resolveListKey(final ListSchemaNode list) {
        final String packageName = BindingGeneratorUtil
                .packageNameForGeneratedType(basePackageName, list.getPath());
        final String listName = list.getQName().getLocalName() + "Key";

        return BindingGeneratorUtil.schemaNodeToTransferObjectBuilder(
                packageName, list, listName);
    }

    private boolean isPartOfListKey(final LeafSchemaNode leaf,
            final List<String> keys) {
        if ((leaf != null) && (keys != null) && (leaf.getQName() != null)) {
            final String leafName = leaf.getQName().getLocalName();
            if (keys.contains(leafName)) {
                return true;
            }
        }
        return false;
    }

    private List<String> listKeys(final ListSchemaNode list) {
        final List<String> listKeys = new ArrayList<String>();

        if (list.getKeyDefinition() != null) {
            final List<QName> keyDefinitions = list.getKeyDefinition();

            for (final QName keyDefinition : keyDefinitions) {
                listKeys.add(keyDefinition.getLocalName());
            }
        }
        return listKeys;
    }

    private GeneratedTypeBuilder resolveListTypeBuilder(
            final ListSchemaNode list) {
        final String packageName = BindingGeneratorUtil
                .packageNameForGeneratedType(basePackageName,
                        list.getPath());
        final String schemaNodeName = list.getQName().getLocalName();
        final String genTypeName = BindingGeneratorUtil
                .parseToClassName(schemaNodeName);

        GeneratedTypeBuilder typeBuilder = null;
        if (genTypeBuilders.containsKey(packageName)) {
            final Map<String, GeneratedTypeBuilder> builders = new HashMap<String, GeneratedTypeBuilder>();
            typeBuilder = builders.get(genTypeName);

            if (null == typeBuilder) {
                typeBuilder = addRawInterfaceDefinition(list);
            }
        }
        return typeBuilder;
    }
}
