/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.binding.generator.impl;

import org.opendaylight.controller.binding.generator.util.ReferencedTypeImpl;
import org.opendaylight.controller.binding.generator.util.Types;
import org.opendaylight.controller.binding.generator.util.generated.type.builder.GeneratedTypeBuilderImpl;
import org.opendaylight.controller.sal.binding.generator.api.BindingGenerator;
import org.opendaylight.controller.sal.binding.generator.spi.TypeProvider;
import org.opendaylight.controller.sal.binding.model.api.*;
import org.opendaylight.controller.sal.binding.model.api.type.builder.*;
import org.opendaylight.controller.sal.binding.yang.types.TypeProviderImpl;
import org.opendaylight.controller.yang.common.QName;
import org.opendaylight.controller.yang.model.api.*;
import org.opendaylight.controller.yang.model.api.type.EnumTypeDefinition;
import org.opendaylight.controller.yang.model.api.type.EnumTypeDefinition.EnumPair;
import org.opendaylight.controller.yang.model.util.DataNodeIterator;
import org.opendaylight.controller.yang.model.util.ExtendedType;

import java.util.*;

import static org.opendaylight.controller.binding.generator.util.BindingGeneratorUtil.*;
import static org.opendaylight.controller.yang.model.util.SchemaContextUtil.findDataSchemaNode;
import static org.opendaylight.controller.yang.model.util.SchemaContextUtil.findParentModule;

public final class BindingGeneratorImpl implements BindingGenerator {

    private Map<String, Map<String, GeneratedTypeBuilder>> genTypeBuilders;
    private TypeProvider typeProvider;
    private SchemaContext schemaContext;

    public BindingGeneratorImpl() {
        super();
    }

    @Override
    public List<Type> generateTypes(final SchemaContext context) {
        if (context == null) {
            throw new IllegalArgumentException("Schema Context reference " +
                    "cannot be NULL!");
        }
        if (context.getModules() == null) {
            throw new IllegalStateException("Schema Context does not contain " +
                    "defined modules!");
        }

        final List<Type> generatedTypes = new ArrayList<>();
        schemaContext = context;
        typeProvider = new TypeProviderImpl(context);
        final Set<Module> modules = context.getModules();
        genTypeBuilders = new HashMap<>();
        for (final Module module : modules) {
            generatedTypes.add(moduleToDataType(module));
            generatedTypes.addAll(allTypeDefinitionsToGenTypes(module));
            generatedTypes.addAll(allContainersToGenTypes(module));
            generatedTypes.addAll(allListsToGenTypes(module));
            generatedTypes.addAll(allAugmentsToGenTypes(module));
            generatedTypes.addAll(allRPCMethodsToGenType(module));
            generatedTypes.addAll(allNotifycationsToGenType(module));
        }
        return generatedTypes;
    }

    @Override
    public List<Type> generateTypes(final SchemaContext context,
                                    final Set<Module> modules) {
        if (context == null) {
            throw new IllegalArgumentException("Schema Context reference " +
                    "cannot be NULL!");
        }
        if (context.getModules() == null) {
            throw new IllegalStateException("Schema Context does not contain " +
                    "defined modules!");
        }
        if (modules == null) {
            throw new IllegalArgumentException("Sef of Modules cannot be " +
                    "NULL!");
        }

        final List<Type> filteredGenTypes = new ArrayList<>();
        schemaContext = context;
        typeProvider = new TypeProviderImpl(context);
        final Set<Module> contextModules = context.getModules();
        genTypeBuilders = new HashMap<>();
        for (final Module contextModule : contextModules) {
            final List<Type> generatedTypes = new ArrayList<>();

            generatedTypes.add(moduleToDataType(contextModule));
            generatedTypes.addAll(allTypeDefinitionsToGenTypes(contextModule));
            generatedTypes.addAll(allContainersToGenTypes(contextModule));
            generatedTypes.addAll(allListsToGenTypes(contextModule));
            generatedTypes.addAll(allAugmentsToGenTypes(contextModule));
            generatedTypes.addAll(allRPCMethodsToGenType(contextModule));
            generatedTypes.addAll(allNotifycationsToGenType(contextModule));

            if (modules.contains(contextModule)) {
                filteredGenTypes.addAll(generatedTypes);
            }
        }
        return filteredGenTypes;
    }

    private List<Type> allTypeDefinitionsToGenTypes(final Module module) {
        if (module == null) {
            throw new IllegalArgumentException("Module reference cannot be NULL!");
        }
        if (module.getName() == null) {
            throw new IllegalArgumentException("Module name cannot be NULL!");
        }
        if (module.getTypeDefinitions() == null) {
            throw new IllegalArgumentException("Type Definitions for module " +
                    module.getName() + " cannot be NULL!");
        }

        final Set<TypeDefinition<?>> typeDefinitions = module
                .getTypeDefinitions();
        final List<Type> generatedTypes = new ArrayList<>();
        for (final TypeDefinition<?> typedef : typeDefinitions) {
            if (typedef != null) {
                final Type type = ((TypeProviderImpl)typeProvider)
                        .generatedTypeForExtendedDefinitionType(typedef);
                if ((type != null) && !generatedTypes.contains(type)) {
                    generatedTypes.add(type);
                }
            }
        }
        return generatedTypes;
    }

    private List<Type> allContainersToGenTypes(final Module module) {
        if (module == null) {
            throw new IllegalArgumentException("Module reference cannot be NULL!");
        }

        if (module.getName() == null) {
            throw new IllegalArgumentException("Module name cannot be NULL!");
        }

        if (module.getChildNodes() == null) {
            throw new IllegalArgumentException("Reference to Set of Child " +
                    "Nodes in module " + module.getName() + " cannot be " +
                    "NULL!");
        }

        final List<Type> generatedTypes = new ArrayList<>();
        final DataNodeIterator it = new DataNodeIterator(
                module);
        final List<ContainerSchemaNode> schemaContainers = it.allContainers();
        final String basePackageName = moduleNamespaceToPackageName(module);
        for (final ContainerSchemaNode container : schemaContainers) {
            generatedTypes.add(containerToGenType(basePackageName,
                    container));
        }
        return generatedTypes;
    }

    private List<Type> allListsToGenTypes(final Module module) {
        if (module == null) {
            throw new IllegalArgumentException("Module reference cannot be NULL!");
        }

        if (module.getName() == null) {
            throw new IllegalArgumentException("Module name cannot be NULL!");
        }

        if (module.getChildNodes() == null) {
            throw new IllegalArgumentException("Reference to Set of Child " +
                    "Nodes in module " + module.getName() + " cannot be " +
                    "NULL!");
        }

        final List<Type> generatedTypes = new ArrayList<>();
        final DataNodeIterator it = new DataNodeIterator(
                module);
        final List<ListSchemaNode> schemaLists = it.allLists();
        final String basePackageName = moduleNamespaceToPackageName(module);
        if (schemaLists != null) {
            for (final ListSchemaNode list : schemaLists) {
                generatedTypes.addAll(listToGenType(basePackageName, list));
            }
        }
        return generatedTypes;
    }

    private List<Type> allAugmentsToGenTypes(final Module module) {
        if (module == null) {
            throw new IllegalArgumentException("Module reference cannot be NULL!");
        }

        if (module.getName() == null) {
            throw new IllegalArgumentException("Module name cannot be NULL!");
        }

        if (module.getChildNodes() == null) {
            throw new IllegalArgumentException("Reference to Set of " +
                    "Augmentation Definitions in module " + module.getName()
                    + " cannot be NULL!");
        }

        final List<Type> generatedTypes = new ArrayList<>();
        final String basePackageName = moduleNamespaceToPackageName(module);
        final List<AugmentationSchema> augmentations = resolveAugmentations(module);
        for (final AugmentationSchema augment : augmentations) {
            generatedTypes.addAll(augmentationToGenTypes(basePackageName, augment));
        }
        return generatedTypes;
    }

    private List<AugmentationSchema> resolveAugmentations(final Module module) {
        if (module == null) {
            throw new IllegalArgumentException("Module reference cannot be NULL!");
        }
        if (module.getAugmentations() == null) {
            throw new IllegalStateException("Augmentations Set cannot be NULL!");
        }

        final Set<AugmentationSchema> augmentations = module
                .getAugmentations();
        final List<AugmentationSchema> sortedAugmentations = new ArrayList<>(
                augmentations);
        Collections.sort(sortedAugmentations,
                new Comparator<AugmentationSchema>() {

                    @Override
                    public int compare(
                            AugmentationSchema augSchema1,
                            AugmentationSchema augSchema2) {

                        if (augSchema1.getTargetPath().getPath()
                                .size() > augSchema2
                                .getTargetPath().getPath().size()) {
                            return 1;
                        } else if (augSchema1.getTargetPath()
                                .getPath().size() < augSchema2
                                .getTargetPath().getPath().size()) {
                            return -1;
                        }
                        return 0;

                    }
                });

        return sortedAugmentations;
    }

    private GeneratedType moduleToDataType(final Module module) {
        if (module == null) {
            throw new IllegalArgumentException("Module reference cannot be NULL!");
        }

        final GeneratedTypeBuilder moduleDataTypeBuilder = moduleTypeBuilder(
                module, "Data");

        final String basePackageName = moduleNamespaceToPackageName(module);
        if (moduleDataTypeBuilder != null) {
            final Set<DataSchemaNode> dataNodes = module.getChildNodes();
            resolveDataSchemaNodes(basePackageName, moduleDataTypeBuilder, dataNodes);
        }
        return moduleDataTypeBuilder.toInstance();
    }

    private List<GeneratedType> allRPCMethodsToGenType(final Module module) {
        if (module == null) {
            throw new IllegalArgumentException("Module reference cannot be NULL!");
        }

        if (module.getName() == null) {
            throw new IllegalArgumentException("Module name cannot be NULL!");
        }

        if (module.getChildNodes() == null) {
            throw new IllegalArgumentException("Reference to Set of " +
                    "RPC Method Definitions in module " + module.getName()
                    + " cannot be NULL!");
        }

        final String basePackageName = moduleNamespaceToPackageName(module);
        final Set<RpcDefinition> rpcDefinitions = module.getRpcs();
        final List<GeneratedType> genRPCTypes = new ArrayList<>();

        for (final RpcDefinition rpc : rpcDefinitions) {
            if (rpc != null) {
                final List<DataNodeIterator> rpcInOut = new ArrayList<>();
                rpcInOut.add(new DataNodeIterator(rpc.getInput()));
                rpcInOut.add(new DataNodeIterator(rpc.getOutput()));

                for (DataNodeIterator it : rpcInOut) {
                    List<ContainerSchemaNode> nContainers = it.allContainers();
                    if ((nContainers != null) && !nContainers.isEmpty()) {
                        for (final ContainerSchemaNode container : nContainers) {
                            genRPCTypes.add(containerToGenType(basePackageName, container));
                        }
                    }
                }
            }
        }
        return genRPCTypes;
    }

    private List<Type> allNotifycationsToGenType(final Module module) {
        if (module == null) {
            throw new IllegalArgumentException("Module reference cannot be NULL!");
        }

        if (module.getName() == null) {
            throw new IllegalArgumentException("Module name cannot be NULL!");
        }

        if (module.getChildNodes() == null) {
            throw new IllegalArgumentException("Reference to Set of " +
                    "Notification Definitions in module " + module.getName()
                    + " cannot be NULL!");
        }

        final String basePackageName = moduleNamespaceToPackageName(module);
        final List<Type> genNotifyTypes = new ArrayList<>();
        final Set<NotificationDefinition> notifications = module
                .getNotifications();

        for (final NotificationDefinition notification : notifications) {
            if (notification != null) {
                final List<DataNodeIterator> notifyChildren = new ArrayList<>();

                for (DataSchemaNode childNode : notification.getChildNodes()) {
                    if (childNode instanceof DataNodeContainer) {
                        notifyChildren.add(new DataNodeIterator((DataNodeContainer) childNode));
                    }
                }

                for (DataNodeIterator it : notifyChildren) {
                    List<ContainerSchemaNode> nContainers = it.allContainers();
                    List<ListSchemaNode> nLists = it.allLists();
                    if ((nContainers != null) && !nContainers.isEmpty()) {
                        for (final ContainerSchemaNode container : nContainers) {
                            genNotifyTypes.add(containerToGenType(basePackageName, container));
                        }
                    }
                    if ((nLists != null) && !nLists.isEmpty()) {
                        for (final ListSchemaNode list : nLists) {
                            genNotifyTypes.addAll(listToGenType(basePackageName, list));
                        }
                    }
                }
            }
        }
        return genNotifyTypes;
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

    private EnumBuilder resolveInnerEnumFromTypeDefinition(
            final EnumTypeDefinition enumTypeDef, final String enumName,
            final GeneratedTypeBuilder typeBuilder) {
        if ((enumTypeDef != null) && (typeBuilder != null)
                && (enumTypeDef.getQName() != null)
                && (enumTypeDef.getQName().getLocalName() != null)) {

            final String enumerationName = parseToClassName(enumName);
            final EnumBuilder enumBuilder = typeBuilder
                    .addEnumeration(enumerationName);

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
                return enumBuilder;
            }
        }
        return null;
    }

    private GeneratedTypeBuilder moduleTypeBuilder(final Module module,
                                                   final String postfix) {
        if (module == null) {
            throw new IllegalArgumentException("Module reference cannot be NULL!");
        }
        String packageName = moduleNamespaceToPackageName(module);
        final String moduleName = parseToClassName(module.getName())
                + postfix;

        return new GeneratedTypeBuilderImpl(packageName, moduleName);

    }

    private List<Type> augmentationToGenTypes(final String augmentPackageName,
                                              final AugmentationSchema augSchema) {
        if (augmentPackageName == null) {
            throw new IllegalArgumentException("Package Name cannot be NULL!");
        }
        if (augSchema == null) {
            throw new IllegalArgumentException(
                    "Augmentation Schema cannot be NULL!");
        }
        if (augSchema.getTargetPath() == null) {
            throw new IllegalStateException(
                    "Augmentation Schema does not contain Target Path (Target Path is NULL).");
        }

        final List<Type> genTypes = new ArrayList<>();

        // EVERY augmented interface will extends Augmentation<T> interface
        // and DataObject interface!!!
        final SchemaPath targetPath = augSchema.getTargetPath();
        final DataSchemaNode targetSchemaNode = findDataSchemaNode(schemaContext,
                targetPath);
        if ((targetSchemaNode != null) &&
                (targetSchemaNode.getQName() != null) &&
                (targetSchemaNode.getQName().getLocalName() != null)) {
            final Module targetModule = findParentModule(schemaContext,
                    targetSchemaNode);

            final String targetBasePackage = moduleNamespaceToPackageName(targetModule);
            final String targetPackageName = packageNameForGeneratedType(targetBasePackage,
                    targetSchemaNode.getPath());

            final String targetSchemaNodeName = targetSchemaNode.getQName().getLocalName();
            final Set<DataSchemaNode> augChildNodes = augSchema
                    .getChildNodes();
            final GeneratedTypeBuilder augTypeBuilder = addRawAugmentGenTypeDefinition(
                    augmentPackageName, targetPackageName, targetSchemaNodeName, augSchema);
            if (augTypeBuilder != null) {
                genTypes.add(augTypeBuilder.toInstance());
            }
            genTypes.addAll(augmentationBodyToGenTypes(augmentPackageName,
                    augChildNodes));

        }
        return genTypes;
    }

    private GeneratedTypeBuilder addRawAugmentGenTypeDefinition(
            final String augmentPackageName, final String targetPackageName,
            final String targetSchemaNodeName,
            final AugmentationSchema augSchema) {
        final String targetTypeName = parseToClassName(targetSchemaNodeName);
        Map<String, GeneratedTypeBuilder> augmentBuilders = genTypeBuilders
                .get(augmentPackageName);
        if (augmentBuilders == null) {
            augmentBuilders = new HashMap<>();
            genTypeBuilders.put(augmentPackageName, augmentBuilders);
        }

        final String augTypeName = augGenTypeName(augmentBuilders, targetTypeName);
        final Type targetTypeRef = new ReferencedTypeImpl(targetPackageName, targetTypeName);
        final Set<DataSchemaNode> augChildNodes = augSchema
                .getChildNodes();

        final GeneratedTypeBuilder augTypeBuilder = new GeneratedTypeBuilderImpl(
                augmentPackageName, augTypeName);

        augTypeBuilder.addImplementsType(Types.DATA_OBJECT);
        augTypeBuilder.addImplementsType(Types
                .augmentationTypeFor(targetTypeRef));

        augSchemaNodeToMethods(augmentPackageName, augTypeBuilder, augChildNodes);
        augmentBuilders.put(augTypeName, augTypeBuilder);
        return augTypeBuilder;
    }

    private List<Type> augmentationBodyToGenTypes(final String augBasePackageName,
                                                  final Set<DataSchemaNode> augChildNodes) {
        final List<Type> genTypes = new ArrayList<>();
        final List<DataNodeIterator> augSchemaIts = new ArrayList<>();
        for (final DataSchemaNode childNode : augChildNodes) {
            if (childNode instanceof DataNodeContainer) {
                augSchemaIts.add(new DataNodeIterator(
                        (DataNodeContainer) childNode));

                if (childNode instanceof ContainerSchemaNode) {
                    genTypes.add(containerToGenType(augBasePackageName,
                            (ContainerSchemaNode) childNode));
                } else if (childNode instanceof ListSchemaNode) {
                    genTypes.addAll(listToGenType(augBasePackageName,
                            (ListSchemaNode) childNode));
                }
            }
        }

        for (final DataNodeIterator it : augSchemaIts) {
            final List<ContainerSchemaNode> augContainers = it.allContainers();
            final List<ListSchemaNode> augLists = it.allLists();

            if ((augContainers != null) && !augContainers.isEmpty()) {
                for (final ContainerSchemaNode container : augContainers) {
                    genTypes.add(containerToGenType(augBasePackageName, container));
                }
            }
            if ((augLists != null) && !augLists.isEmpty()) {
                for (final ListSchemaNode list : augLists) {
                    genTypes.addAll(listToGenType(augBasePackageName, list));
                }
            }
        }
        return genTypes;
    }

    private String augGenTypeName(
            final Map<String, GeneratedTypeBuilder> builders,
            final String genTypeName) {
        String augTypeName = genTypeName;

        int index = 1;
        while ((builders != null) && builders.containsKey(genTypeName + index)) {
            index++;
        }
        augTypeName += index;
        return augTypeName;
    }

    private GeneratedType containerToGenType(final String basePackageName,
                                             ContainerSchemaNode containerNode) {
        if (containerNode == null) {
            return null;
        }

        final String packageName = packageNameForGeneratedType(
                basePackageName, containerNode.getPath());
        final Set<DataSchemaNode> schemaNodes = containerNode.getChildNodes();
        final GeneratedTypeBuilder typeBuilder = addRawInterfaceDefinition(
                packageName, containerNode);

        resolveDataSchemaNodes(basePackageName, typeBuilder, schemaNodes);
        return typeBuilder.toInstance();
    }

    private GeneratedTypeBuilder resolveDataSchemaNodes(
            final String basePackageName,
            final GeneratedTypeBuilder typeBuilder,
            final Set<DataSchemaNode> schemaNodes) {

        if ((schemaNodes != null) && (typeBuilder != null)) {
            for (final DataSchemaNode schemaNode : schemaNodes) {
                if (schemaNode.isAugmenting()) {
                    continue;
                }
                addSchemaNodeToBuilderAsMethod(basePackageName, schemaNode, typeBuilder);
            }
        }
        return typeBuilder;
    }

    private GeneratedTypeBuilder augSchemaNodeToMethods(
            final String basePackageName,
            final GeneratedTypeBuilder typeBuilder,
            final Set<DataSchemaNode> schemaNodes) {

        if ((schemaNodes != null) && (typeBuilder != null)) {
            for (final DataSchemaNode schemaNode : schemaNodes) {
                if (schemaNode.isAugmenting()) {
                    addSchemaNodeToBuilderAsMethod(basePackageName, schemaNode, typeBuilder);
                }
            }
        }
        return typeBuilder;
    }

    private void addSchemaNodeToBuilderAsMethod(
            final String basePackageName,
            final DataSchemaNode schemaNode,
            final GeneratedTypeBuilder typeBuilder) {
        if (schemaNode != null && typeBuilder != null) {
            if (schemaNode instanceof LeafSchemaNode) {
                resolveLeafSchemaNodeAsMethod(typeBuilder,
                        (LeafSchemaNode) schemaNode);
            } else if (schemaNode instanceof LeafListSchemaNode) {
                resolveLeafListSchemaNode(typeBuilder,
                        (LeafListSchemaNode) schemaNode);
            } else if (schemaNode instanceof ContainerSchemaNode) {
                resolveContainerSchemaNode(basePackageName, typeBuilder,
                        (ContainerSchemaNode) schemaNode);
            } else if (schemaNode instanceof ListSchemaNode) {
                resolveListSchemaNode(basePackageName, typeBuilder,
                        (ListSchemaNode) schemaNode);
            }
        }
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

                Type returnType = null;
                if (!(typeDef instanceof EnumTypeDefinition)) {
                    returnType = typeProvider
                            .javaTypeForSchemaDefinitionType(typeDef);
                } else {
                    final EnumTypeDefinition enumTypeDef = enumTypeDefFromExtendedType(typeDef);
                    final EnumBuilder enumBuilder = resolveInnerEnumFromTypeDefinition(
                            enumTypeDef, leafName, typeBuilder);

                    if (enumBuilder != null) {
                        returnType = new ReferencedTypeImpl(
                                enumBuilder.getPackageName(),
                                enumBuilder.getName());
                    }
                    ((TypeProviderImpl)typeProvider).putReferencedType(leaf
                            .getPath(), returnType);
                }
                if (returnType != null) {
                    constructGetter(typeBuilder, leafName, leafDesc, returnType);
                    if (!leaf.isConfiguration()) {
                        constructSetter(typeBuilder, leafName, leafDesc, returnType);
                    }
                    return true;
                }
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
                final Type returnType = typeProvider
                        .javaTypeForSchemaDefinitionType(typeDef);

                if (returnType != null) {
                    final GeneratedPropertyBuilder propBuilder = toBuilder
                            .addProperty(parseToClassName(leafName));

                    propBuilder.setReadOnly(isReadOnly);
                    propBuilder.addReturnType(returnType);
                    propBuilder.addComment(leafDesc);

                    toBuilder.addEqualsIdentity(propBuilder);
                    toBuilder.addHashIdentity(propBuilder);
                    toBuilder.addToStringProperty(propBuilder);

                    return true;
                }
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

    private boolean resolveContainerSchemaNode(final String basePackageName,
                                               final GeneratedTypeBuilder typeBuilder,
                                               final ContainerSchemaNode containerNode) {
        if ((containerNode != null) && (typeBuilder != null)) {
            final String nodeName = containerNode.getQName().getLocalName();

            if (nodeName != null) {
                final String packageName = packageNameForGeneratedType(
                        basePackageName, containerNode.getPath());

                final GeneratedTypeBuilder rawGenType = addRawInterfaceDefinition(
                        packageName, containerNode);
                constructGetter(typeBuilder, nodeName, "", rawGenType);

                return true;
            }
        }
        return false;
    }

    private boolean resolveListSchemaNode(final String basePackageName,
                                          final GeneratedTypeBuilder typeBuilder,
                                          final ListSchemaNode schemaNode) {
        if ((schemaNode != null) && (typeBuilder != null)) {
            final String listName = schemaNode.getQName().getLocalName();

            if (listName != null) {
                final String packageName = packageNameForGeneratedType(
                        basePackageName, schemaNode.getPath());
                final GeneratedTypeBuilder rawGenType = addRawInterfaceDefinition(
                        packageName, schemaNode);
                constructGetter(typeBuilder, listName, "",
                        Types.listTypeFor(rawGenType));
                if (!schemaNode.isConfiguration()) {
                    constructSetter(typeBuilder, listName, "",
                            Types.listTypeFor(rawGenType));
                }
                return true;
            }
        }
        return false;
    }

    private GeneratedTypeBuilder addRawInterfaceDefinition(
            final String packageName, final DataSchemaNode schemaNode) {
        if (schemaNode == null) {
            return null;
        }

        final String schemaNodeName = schemaNode.getQName().getLocalName();

        if ((packageName != null) && (schemaNodeName != null)) {
            final String genTypeName = parseToClassName(schemaNodeName);
            final GeneratedTypeBuilder newType = new GeneratedTypeBuilderImpl(
                    packageName, genTypeName);

            newType.addImplementsType(Types.DATA_OBJECT);
            newType.addImplementsType(Types.augmentableTypeFor(newType));

            if (!genTypeBuilders.containsKey(packageName)) {
                final Map<String, GeneratedTypeBuilder> builders = new HashMap<>();
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
        method.append(parseToClassName(methodName));
        return method.toString();
    }

    private String setterMethodName(final String methodName) {
        final StringBuilder method = new StringBuilder();
        method.append("set");
        method.append(parseToClassName(methodName));
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
                parseToValidParamName(schemaNodeName));
        setMethod.addReturnType(Types.voidType());

        return setMethod;
    }


    private List<Type> listToGenType(final String basePackageName,
                                     final ListSchemaNode list) {
        if (basePackageName == null) {
            throw new IllegalArgumentException(
                    "Package Name for Generated Type cannot be NULL!");
        }
        if (list == null) {
            throw new IllegalArgumentException(
                    "List Schema Node cannot be NULL!");
        }

        final String packageName = packageNameForGeneratedType(
                basePackageName, list.getPath());
        final GeneratedTypeBuilder typeBuilder = resolveListTypeBuilder(
                packageName, list);
        final List<String> listKeys = listKeys(list);
        GeneratedTOBuilder genTOBuilder = resolveListKeyTOBuilder(packageName,
                list, listKeys);

        final Set<DataSchemaNode> schemaNodes = list.getChildNodes();

        for (final DataSchemaNode schemaNode : schemaNodes) {
            if (schemaNode.isAugmenting()) {
                continue;
            }
            addSchemaNodeToListBuilders(basePackageName, schemaNode, typeBuilder,
                    genTOBuilder, listKeys);
        }
        return typeBuildersToGenTypes(typeBuilder, genTOBuilder);
    }

    private void addSchemaNodeToListBuilders(final String basePackageName,
                                             final DataSchemaNode schemaNode,
                                             final GeneratedTypeBuilder typeBuilder,
                                             final GeneratedTOBuilder genTOBuilder,
                                             final List<String> listKeys) {
        if (schemaNode == null) {
            throw new IllegalArgumentException(
                    "Data Schema Node cannot be NULL!");
        }

        if (typeBuilder == null) {
            throw new IllegalArgumentException(
                    "Generated Type Builder cannot be NULL!");
        }

        if (schemaNode instanceof LeafSchemaNode) {
            final LeafSchemaNode leaf = (LeafSchemaNode) schemaNode;
            if (!isPartOfListKey(leaf, listKeys)) {
                resolveLeafSchemaNodeAsMethod(typeBuilder, leaf);
            } else {
                resolveLeafSchemaNodeAsProperty(genTOBuilder, leaf, true);
            }
        } else if (schemaNode instanceof LeafListSchemaNode) {
            resolveLeafListSchemaNode(typeBuilder,
                    (LeafListSchemaNode) schemaNode);
        } else if (schemaNode instanceof ContainerSchemaNode) {
            resolveContainerSchemaNode(basePackageName, typeBuilder,
                    (ContainerSchemaNode) schemaNode);
        } else if (schemaNode instanceof ListSchemaNode) {
            resolveListSchemaNode(basePackageName, typeBuilder, (ListSchemaNode) schemaNode);
        }
    }

    private List<Type> typeBuildersToGenTypes(
            final GeneratedTypeBuilder typeBuilder,
            GeneratedTOBuilder genTOBuilder) {
        final List<Type> genTypes = new ArrayList<>();
        if (typeBuilder == null) {
            throw new IllegalArgumentException(
                    "Generated Type Builder cannot be NULL!");
        }

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
    private GeneratedTOBuilder resolveListKey(final String packageName,
                                              final ListSchemaNode list) {
        final String listName = list.getQName().getLocalName() + "Key";
        return schemaNodeToTransferObjectBuilder(packageName, list, listName);
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
        final List<String> listKeys = new ArrayList<>();

        if (list.getKeyDefinition() != null) {
            final List<QName> keyDefinitions = list.getKeyDefinition();

            for (final QName keyDefinition : keyDefinitions) {
                listKeys.add(keyDefinition.getLocalName());
            }
        }
        return listKeys;
    }

    private GeneratedTypeBuilder resolveListTypeBuilder(
            final String packageName, final ListSchemaNode list) {
        if (packageName == null) {
            throw new IllegalArgumentException(
                    "Package Name for Generated Type cannot be NULL!");
        }
        if (list == null) {
            throw new IllegalArgumentException(
                    "List Schema Node cannot be NULL!");
        }

        final String schemaNodeName = list.getQName().getLocalName();
        final String genTypeName = parseToClassName(schemaNodeName);

        GeneratedTypeBuilder typeBuilder = null;
        final Map<String, GeneratedTypeBuilder> builders = genTypeBuilders.get(packageName);
        if (builders != null) {
            typeBuilder = builders.get(genTypeName);
        }
        if (typeBuilder == null) {
            typeBuilder = addRawInterfaceDefinition(packageName, list);
        }
        return typeBuilder;
    }

    private GeneratedTOBuilder resolveListKeyTOBuilder(
            final String packageName, final ListSchemaNode list,
            final List<String> listKeys) {
        GeneratedTOBuilder genTOBuilder = null;
        if (listKeys.size() > 0) {
            genTOBuilder = resolveListKey(packageName, list);
        }
        return genTOBuilder;
    }
}
