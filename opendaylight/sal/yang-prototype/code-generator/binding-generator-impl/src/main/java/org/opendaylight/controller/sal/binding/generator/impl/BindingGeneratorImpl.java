/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.binding.generator.impl;

import static org.opendaylight.controller.binding.generator.util.BindingGeneratorUtil.*;
import static org.opendaylight.controller.yang.model.util.SchemaContextUtil.findDataSchemaNode;
import static org.opendaylight.controller.yang.model.util.SchemaContextUtil.findParentModule;

import java.util.*;
import java.util.concurrent.Future;

import org.opendaylight.controller.binding.generator.util.ReferencedTypeImpl;
import org.opendaylight.controller.binding.generator.util.Types;
import org.opendaylight.controller.binding.generator.util.generated.type.builder.GeneratedTOBuilderImpl;
import org.opendaylight.controller.binding.generator.util.generated.type.builder.GeneratedTypeBuilderImpl;
import org.opendaylight.controller.sal.binding.generator.api.BindingGenerator;
import org.opendaylight.controller.sal.binding.generator.spi.TypeProvider;
import org.opendaylight.controller.sal.binding.model.api.GeneratedTransferObject;
import org.opendaylight.controller.sal.binding.model.api.GeneratedType;
import org.opendaylight.controller.sal.binding.model.api.Type;
import org.opendaylight.controller.sal.binding.model.api.type.builder.*;
import org.opendaylight.controller.sal.binding.yang.types.TypeProviderImpl;
import org.opendaylight.controller.yang.binding.Notification;
import org.opendaylight.controller.yang.common.QName;
import org.opendaylight.controller.yang.common.RpcResult;
import org.opendaylight.controller.yang.model.api.*;
import org.opendaylight.controller.yang.model.api.type.EnumTypeDefinition;
import org.opendaylight.controller.yang.model.api.type.EnumTypeDefinition.EnumPair;
import org.opendaylight.controller.yang.model.util.DataNodeIterator;
import org.opendaylight.controller.yang.model.util.ExtendedType;
import org.opendaylight.controller.yang.model.util.SchemaContextUtil;

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
            throw new IllegalArgumentException("Schema Context reference cannot be NULL!");
        }
        if (context.getModules() == null) {
            throw new IllegalStateException("Schema Context does not contain defined modules!");
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
            generatedTypes.addAll(allChoicesToGenTypes(module));
            generatedTypes.addAll(allAugmentsToGenTypes(module));
            generatedTypes.addAll(allRPCMethodsToGenType(module));
            generatedTypes.addAll(allNotificationsToGenType(module));
            generatedTypes.addAll(allIdentitiesToGenTypes(module, context));
            generatedTypes.addAll(allGroupingsToGenTypes(module));
        }
        return generatedTypes;
    }

    @Override
    public List<Type> generateTypes(final SchemaContext context, final Set<Module> modules) {
        if (context == null) {
            throw new IllegalArgumentException("Schema Context reference cannot be NULL!");
        }
        if (context.getModules() == null) {
            throw new IllegalStateException("Schema Context does not contain defined modules!");
        }
        if (modules == null) {
            throw new IllegalArgumentException("Sef of Modules cannot be NULL!");
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
            generatedTypes.addAll(allChoicesToGenTypes(contextModule));
            generatedTypes.addAll(allAugmentsToGenTypes(contextModule));
            generatedTypes.addAll(allRPCMethodsToGenType(contextModule));
            generatedTypes.addAll(allNotificationsToGenType(contextModule));
            generatedTypes.addAll(allIdentitiesToGenTypes(contextModule, context));
            generatedTypes.addAll(allGroupingsToGenTypes(contextModule));

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
            throw new IllegalArgumentException("Type Definitions for module " + module.getName() + " cannot be NULL!");
        }

        final Set<TypeDefinition<?>> typeDefinitions = module.getTypeDefinitions();
        final List<Type> generatedTypes = new ArrayList<>();
        for (final TypeDefinition<?> typedef : typeDefinitions) {
            if (typedef != null) {
                final Type type = ((TypeProviderImpl) typeProvider).generatedTypeForExtendedDefinitionType(typedef);
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
            throw new IllegalArgumentException("Reference to Set of Child Nodes in module " + module.getName()
                    + " cannot be NULL!");
        }

        final List<Type> generatedTypes = new ArrayList<>();
        final DataNodeIterator it = new DataNodeIterator(module);
        final List<ContainerSchemaNode> schemaContainers = it.allContainers();
        final String basePackageName = moduleNamespaceToPackageName(module);
        for (final ContainerSchemaNode container : schemaContainers) {
            generatedTypes.add(containerToGenType(basePackageName, container));
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
            throw new IllegalArgumentException("Reference to Set of Child Nodes in module " + module.getName()
                    + " cannot be NULL!");
        }

        final List<Type> generatedTypes = new ArrayList<>();
        final DataNodeIterator it = new DataNodeIterator(module);
        final List<ListSchemaNode> schemaLists = it.allLists();
        final String basePackageName = moduleNamespaceToPackageName(module);
        if (schemaLists != null) {
            for (final ListSchemaNode list : schemaLists) {
                generatedTypes.addAll(listToGenType(basePackageName, list));
            }
        }
        return generatedTypes;
    }

    private List<GeneratedType> allChoicesToGenTypes(final Module module) {
        if (module == null) {
            throw new IllegalArgumentException("Module reference cannot be NULL!");
        }
        if (module.getName() == null) {
            throw new IllegalArgumentException("Module name cannot be NULL!");
        }

        final DataNodeIterator it = new DataNodeIterator(module);
        final List<ChoiceNode> choiceNodes = it.allChoices();
        final String basePackageName = moduleNamespaceToPackageName(module);

        final List<GeneratedType> generatedTypes = new ArrayList<>();
        for (final ChoiceNode choice : choiceNodes) {
            if (choice != null) {
                generatedTypes.addAll(choiceToGeneratedType(basePackageName, choice));
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
            throw new IllegalArgumentException("Reference to Set of Augmentation Definitions in module "
                    + module.getName() + " cannot be NULL!");
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

        final Set<AugmentationSchema> augmentations = module.getAugmentations();
        final List<AugmentationSchema> sortedAugmentations = new ArrayList<>(augmentations);
        Collections.sort(sortedAugmentations, new Comparator<AugmentationSchema>() {

            @Override
            public int compare(AugmentationSchema augSchema1, AugmentationSchema augSchema2) {

                if (augSchema1.getTargetPath().getPath().size() > augSchema2.getTargetPath().getPath().size()) {
                    return 1;
                } else if (augSchema1.getTargetPath().getPath().size() < augSchema2.getTargetPath().getPath().size()) {
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

        final GeneratedTypeBuilder moduleDataTypeBuilder = moduleTypeBuilder(module, "Data");

        final String basePackageName = moduleNamespaceToPackageName(module);
        if (moduleDataTypeBuilder != null) {
            final Set<DataSchemaNode> dataNodes = module.getChildNodes();
            resolveDataSchemaNodes(basePackageName, moduleDataTypeBuilder, dataNodes);
        }
        return moduleDataTypeBuilder.toInstance();
    }

    private List<Type> allRPCMethodsToGenType(final Module module) {
        if (module == null) {
            throw new IllegalArgumentException("Module reference cannot be NULL!");
        }

        if (module.getName() == null) {
            throw new IllegalArgumentException("Module name cannot be NULL!");
        }

        if (module.getChildNodes() == null) {
            throw new IllegalArgumentException("Reference to Set of RPC Method Definitions in module "
                    + module.getName() + " cannot be NULL!");
        }

        final String basePackageName = moduleNamespaceToPackageName(module);
        final Set<RpcDefinition> rpcDefinitions = module.getRpcs();
        final List<Type> genRPCTypes = new ArrayList<>();
        final GeneratedTypeBuilder interfaceBuilder = moduleTypeBuilder(module, "Service");
        final Type future = Types.typeForClass(Future.class);
        for (final RpcDefinition rpc : rpcDefinitions) {
            if (rpc != null) {

                String rpcName = parseToClassName(rpc.getQName().getLocalName());
                String rpcMethodName = parseToValidParamName(rpcName);
                MethodSignatureBuilder method = interfaceBuilder.addMethod(rpcMethodName);

                final List<DataNodeIterator> rpcInOut = new ArrayList<>();

                ContainerSchemaNode input = rpc.getInput();
                ContainerSchemaNode output = rpc.getOutput();

                if (input != null) {
                    rpcInOut.add(new DataNodeIterator(input));
                    GeneratedTypeBuilder inType = addRawInterfaceDefinition(basePackageName, input, rpcName);
                    resolveDataSchemaNodes(basePackageName, inType, input.getChildNodes());
                    Type inTypeInstance = inType.toInstance();
                    genRPCTypes.add(inTypeInstance);
                    method.addParameter(inTypeInstance, "input");
                }

                Type outTypeInstance = Types.typeForClass(Void.class);
                if (output != null) {
                    rpcInOut.add(new DataNodeIterator(output));

                    GeneratedTypeBuilder outType = addRawInterfaceDefinition(basePackageName, output, rpcName);
                    resolveDataSchemaNodes(basePackageName, outType, output.getChildNodes());
                    outTypeInstance = outType.toInstance();
                    genRPCTypes.add(outTypeInstance);

                }

                final Type rpcRes = Types.parameterizedTypeFor(Types.typeForClass(RpcResult.class), outTypeInstance);
                method.setReturnType(Types.parameterizedTypeFor(future, rpcRes));
                for (DataNodeIterator it : rpcInOut) {
                    List<ContainerSchemaNode> nContainers = it.allContainers();
                    if ((nContainers != null) && !nContainers.isEmpty()) {
                        for (final ContainerSchemaNode container : nContainers) {
                            genRPCTypes.add(containerToGenType(basePackageName, container));
                        }
                    }
                    List<ListSchemaNode> nLists = it.allLists();
                    if ((nLists != null) && !nLists.isEmpty()) {
                        for (final ListSchemaNode list : nLists) {
                            genRPCTypes.addAll(listToGenType(basePackageName, list));
                        }
                    }
                }
            }
        }
        genRPCTypes.add(interfaceBuilder.toInstance());
        return genRPCTypes;
    }

    private List<Type> allNotificationsToGenType(final Module module) {
        if (module == null) {
            throw new IllegalArgumentException("Module reference cannot be NULL!");
        }

        if (module.getName() == null) {
            throw new IllegalArgumentException("Module name cannot be NULL!");
        }

        if (module.getChildNodes() == null) {
            throw new IllegalArgumentException("Reference to Set of Notification Definitions in module "
                    + module.getName() + " cannot be NULL!");
        }

        final String basePackageName = moduleNamespaceToPackageName(module);
        final List<Type> genNotifyTypes = new ArrayList<>();
        final Set<NotificationDefinition> notifications = module.getNotifications();

        for (final NotificationDefinition notification : notifications) {
            if (notification != null) {
                DataNodeIterator it = new DataNodeIterator(notification);

                // Containers
                for (ContainerSchemaNode node : it.allContainers()) {
                    genNotifyTypes.add(containerToGenType(basePackageName, node));
                }
                // Lists
                for (ListSchemaNode node : it.allLists()) {
                    genNotifyTypes.addAll(listToGenType(basePackageName, node));
                }
                final GeneratedTypeBuilder notificationTypeBuilder = addDefaultInterfaceDefinition(basePackageName,
                        notification);
                notificationTypeBuilder.addImplementsType(Types.typeForClass(Notification.class));
                // Notification object
                resolveDataSchemaNodes(basePackageName, notificationTypeBuilder, notification.getChildNodes());
                genNotifyTypes.add(notificationTypeBuilder.toInstance());
            }
        }
        return genNotifyTypes;
    }

    private List<Type> allIdentitiesToGenTypes(final Module module, final SchemaContext context) {
        List<Type> genTypes = new ArrayList<>();

        final Set<IdentitySchemaNode> schemaIdentities = module.getIdentities();

        final String basePackageName = moduleNamespaceToPackageName(module);

        if (schemaIdentities != null && !schemaIdentities.isEmpty()) {
            for (final IdentitySchemaNode identity : schemaIdentities) {
                genTypes.add(identityToGenType(basePackageName, identity, context));
            }
        }
        return genTypes;
    }

    private GeneratedType identityToGenType(final String basePackageName, final IdentitySchemaNode identity,
            final SchemaContext context) {
        if (identity == null) {
            return null;
        }

        final String packageName = packageNameForGeneratedType(basePackageName, identity.getPath());
        final String genTypeName = parseToClassName(identity.getQName().getLocalName());
        final GeneratedTOBuilderImpl newType = new GeneratedTOBuilderImpl(packageName, genTypeName);

        IdentitySchemaNode baseIdentity = identity.getBaseIdentity();
        if (baseIdentity != null) {
            Module baseIdentityParentModule = SchemaContextUtil.findParentModule(context, baseIdentity);

            final String returnTypePkgName = moduleNamespaceToPackageName(baseIdentityParentModule);
            final String returnTypeName = parseToClassName(baseIdentity.getQName().getLocalName());

            GeneratedTransferObject gto = new GeneratedTOBuilderImpl(returnTypePkgName, returnTypeName).toInstance();
            newType.setExtendsType(gto);
        } else {
            newType.setExtendsType(Types.getBaseIdentityTO());
        }
        newType.setAbstract(true);
        return newType.toInstance();
    }

    private List<Type> allGroupingsToGenTypes(final Module module) {
        final List<Type> genTypes = new ArrayList<>();
        final String basePackageName = moduleNamespaceToPackageName(module);
        final Set<GroupingDefinition> groupings = module.getGroupings();
        if (groupings != null && !groupings.isEmpty()) {
            for (final GroupingDefinition grouping : groupings) {
                genTypes.add(groupingToGenType(basePackageName, grouping));
            }
        }
        return genTypes;
    }

    private GeneratedType groupingToGenType(final String basePackageName, GroupingDefinition grouping) {
        if (grouping == null) {
            return null;
        }

        final String packageName = packageNameForGeneratedType(basePackageName, grouping.getPath());
        final Set<DataSchemaNode> schemaNodes = grouping.getChildNodes();
        final GeneratedTypeBuilder typeBuilder = addDefaultInterfaceDefinition(packageName, grouping);

        resolveDataSchemaNodes(basePackageName, typeBuilder, schemaNodes);
        return typeBuilder.toInstance();
    }

    private EnumTypeDefinition enumTypeDefFromExtendedType(final TypeDefinition<?> typeDefinition) {
        if (typeDefinition != null) {
            if (typeDefinition.getBaseType() instanceof EnumTypeDefinition) {
                return (EnumTypeDefinition) typeDefinition.getBaseType();
            } else if (typeDefinition.getBaseType() instanceof ExtendedType) {
                return enumTypeDefFromExtendedType(typeDefinition.getBaseType());
            }
        }
        return null;
    }

    private EnumBuilder resolveInnerEnumFromTypeDefinition(final EnumTypeDefinition enumTypeDef, final String enumName,
            final GeneratedTypeBuilder typeBuilder) {
        if ((enumTypeDef != null) && (typeBuilder != null) && (enumTypeDef.getQName() != null)
                && (enumTypeDef.getQName().getLocalName() != null)) {

            final String enumerationName = parseToClassName(enumName);
            final EnumBuilder enumBuilder = typeBuilder.addEnumeration(enumerationName);

            if (enumBuilder != null) {
                final List<EnumPair> enums = enumTypeDef.getValues();
                if (enums != null) {
                    int listIndex = 0;
                    for (final EnumPair enumPair : enums) {
                        if (enumPair != null) {
                            final String enumPairName = parseToClassName(enumPair.getName());
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

    private GeneratedTypeBuilder moduleTypeBuilder(final Module module, final String postfix) {
        if (module == null) {
            throw new IllegalArgumentException("Module reference cannot be NULL!");
        }
        String packageName = moduleNamespaceToPackageName(module);
        final String moduleName = parseToClassName(module.getName()) + postfix;

        return new GeneratedTypeBuilderImpl(packageName, moduleName);

    }

    private List<Type> augmentationToGenTypes(final String augmentPackageName, final AugmentationSchema augSchema) {
        if (augmentPackageName == null) {
            throw new IllegalArgumentException("Package Name cannot be NULL!");
        }
        if (augSchema == null) {
            throw new IllegalArgumentException("Augmentation Schema cannot be NULL!");
        }
        if (augSchema.getTargetPath() == null) {
            throw new IllegalStateException("Augmentation Schema does not contain Target Path (Target Path is NULL).");
        }

        final List<Type> genTypes = new ArrayList<>();

        // EVERY augmented interface will extends Augmentation<T> interface
        // and DataObject interface!!!
        final SchemaPath targetPath = augSchema.getTargetPath();
        final DataSchemaNode targetSchemaNode = findDataSchemaNode(schemaContext, targetPath);
        if ((targetSchemaNode != null) && (targetSchemaNode.getQName() != null)
                && (targetSchemaNode.getQName().getLocalName() != null)) {
            final Module targetModule = findParentModule(schemaContext, targetSchemaNode);
            final String targetBasePackage = moduleNamespaceToPackageName(targetModule);
            final String targetPackageName = packageNameForGeneratedType(targetBasePackage, targetSchemaNode.getPath());
            final String targetSchemaNodeName = targetSchemaNode.getQName().getLocalName();
            final Set<DataSchemaNode> augChildNodes = augSchema.getChildNodes();

            if (!(targetSchemaNode instanceof ChoiceNode)) {
                final GeneratedTypeBuilder augTypeBuilder = addRawAugmentGenTypeDefinition(augmentPackageName,
                        targetPackageName, targetSchemaNodeName, augSchema);
                final GeneratedType augType = augTypeBuilder.toInstance();
                genTypes.add(augType);
            } else {
                final Type refChoiceType = new ReferencedTypeImpl(targetPackageName,
                        parseToClassName(targetSchemaNodeName));
                final ChoiceNode choiceTarget = (ChoiceNode) targetSchemaNode;
                final Set<ChoiceCaseNode> choiceCaseNodes = choiceTarget.getCases();
                genTypes.addAll(augmentCasesToGenTypes(augmentPackageName, refChoiceType, choiceCaseNodes));
            }
            genTypes.addAll(augmentationBodyToGenTypes(augmentPackageName, augChildNodes));
        }
        return genTypes;
    }

    private List<GeneratedType> augmentCasesToGenTypes(final String augmentPackageName, final Type refChoiceType,
            final Set<ChoiceCaseNode> choiceCaseNodes) {
        if (augmentPackageName == null) {
            throw new IllegalArgumentException("Augment Package Name string cannot be NULL!");
        }
        if (choiceCaseNodes == null) {
            throw new IllegalArgumentException("Set of Choice Case Nodes cannot be NULL!");
        }
        final List<GeneratedType> genTypes = generateTypesFromAugmentedChoiceCases(augmentPackageName, refChoiceType,
                choiceCaseNodes);
        return genTypes;
    }

    private GeneratedTypeBuilder addRawAugmentGenTypeDefinition(final String augmentPackageName,
            final String targetPackageName, final String targetSchemaNodeName, final AugmentationSchema augSchema) {
        final String targetTypeName = parseToClassName(targetSchemaNodeName);
        Map<String, GeneratedTypeBuilder> augmentBuilders = genTypeBuilders.get(augmentPackageName);
        if (augmentBuilders == null) {
            augmentBuilders = new HashMap<>();
            genTypeBuilders.put(augmentPackageName, augmentBuilders);
        }

        final String augTypeName = augGenTypeName(augmentBuilders, targetTypeName);
        final Type targetTypeRef = new ReferencedTypeImpl(targetPackageName, targetTypeName);
        final Set<DataSchemaNode> augChildNodes = augSchema.getChildNodes();

        final GeneratedTypeBuilder augTypeBuilder = new GeneratedTypeBuilderImpl(augmentPackageName, augTypeName);

        augTypeBuilder.addImplementsType(Types.DATA_OBJECT);
        augTypeBuilder.addImplementsType(Types.augmentationTypeFor(targetTypeRef));

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
                augSchemaIts.add(new DataNodeIterator((DataNodeContainer) childNode));

                if (childNode instanceof ContainerSchemaNode) {
                    genTypes.add(containerToGenType(augBasePackageName, (ContainerSchemaNode) childNode));
                } else if (childNode instanceof ListSchemaNode) {
                    genTypes.addAll(listToGenType(augBasePackageName, (ListSchemaNode) childNode));
                }
            } else if (childNode instanceof ChoiceNode) {
                final ChoiceNode choice = (ChoiceNode) childNode;
                for (final ChoiceCaseNode caseNode : choice.getCases()) {
                    augSchemaIts.add(new DataNodeIterator(caseNode));
                }
                genTypes.addAll(choiceToGeneratedType(augBasePackageName, (ChoiceNode) childNode));
            }
        }

        for (final DataNodeIterator it : augSchemaIts) {
            final List<ContainerSchemaNode> augContainers = it.allContainers();
            final List<ListSchemaNode> augLists = it.allLists();
            final List<ChoiceNode> augChoices = it.allChoices();

            if (augContainers != null) {
                for (final ContainerSchemaNode container : augContainers) {
                    genTypes.add(containerToGenType(augBasePackageName, container));
                }
            }
            if (augLists != null) {
                for (final ListSchemaNode list : augLists) {
                    genTypes.addAll(listToGenType(augBasePackageName, list));
                }
            }
            if (augChoices != null) {
                for (final ChoiceNode choice : augChoices) {
                    genTypes.addAll(choiceToGeneratedType(augBasePackageName, choice));
                }
            }
        }
        return genTypes;
    }

    private String augGenTypeName(final Map<String, GeneratedTypeBuilder> builders, final String genTypeName) {
        String augTypeName = genTypeName;

        int index = 1;
        while ((builders != null) && builders.containsKey(genTypeName + index)) {
            index++;
        }
        augTypeName += index;
        return augTypeName;
    }

    private GeneratedType containerToGenType(final String basePackageName, ContainerSchemaNode containerNode) {
        if (containerNode == null) {
            return null;
        }

        final String packageName = packageNameForGeneratedType(basePackageName, containerNode.getPath());
        final Set<DataSchemaNode> schemaNodes = containerNode.getChildNodes();
        final GeneratedTypeBuilder typeBuilder = addDefaultInterfaceDefinition(packageName, containerNode);

        resolveDataSchemaNodes(basePackageName, typeBuilder, schemaNodes);
        return typeBuilder.toInstance();
    }

    private GeneratedTypeBuilder resolveDataSchemaNodes(final String basePackageName,
            final GeneratedTypeBuilder typeBuilder, final Set<DataSchemaNode> schemaNodes) {
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

    private GeneratedTypeBuilder augSchemaNodeToMethods(final String basePackageName,
            final GeneratedTypeBuilder typeBuilder, final Set<DataSchemaNode> schemaNodes) {
        if ((schemaNodes != null) && (typeBuilder != null)) {
            for (final DataSchemaNode schemaNode : schemaNodes) {
                if (schemaNode.isAugmenting()) {
                    addSchemaNodeToBuilderAsMethod(basePackageName, schemaNode, typeBuilder);
                }
            }
        }
        return typeBuilder;
    }

    private void addSchemaNodeToBuilderAsMethod(final String basePackageName, final DataSchemaNode schemaNode,
            final GeneratedTypeBuilder typeBuilder) {
        if (schemaNode != null && typeBuilder != null) {
            if (schemaNode instanceof LeafSchemaNode) {
                resolveLeafSchemaNodeAsMethod(typeBuilder, (LeafSchemaNode) schemaNode);
            } else if (schemaNode instanceof LeafListSchemaNode) {
                resolveLeafListSchemaNode(typeBuilder, (LeafListSchemaNode) schemaNode);
            } else if (schemaNode instanceof ContainerSchemaNode) {
                resolveContainerSchemaNode(basePackageName, typeBuilder, (ContainerSchemaNode) schemaNode);
            } else if (schemaNode instanceof ListSchemaNode) {
                resolveListSchemaNode(basePackageName, typeBuilder, (ListSchemaNode) schemaNode);
            } else if (schemaNode instanceof ChoiceNode) {
                resolveChoiceSchemaNode(basePackageName, typeBuilder, (ChoiceNode) schemaNode);
            }
        }
    }

    private void resolveChoiceSchemaNode(final String basePackageName, final GeneratedTypeBuilder typeBuilder,
            final ChoiceNode choiceNode) {
        if (basePackageName == null) {
            throw new IllegalArgumentException("Base Package Name cannot be NULL!");
        }
        if (typeBuilder == null) {
            throw new IllegalArgumentException("Generated Type Builder cannot be NULL!");
        }
        if (choiceNode == null) {
            throw new IllegalArgumentException("Choice Schema Node cannot be NULL!");
        }

        final String choiceName = choiceNode.getQName().getLocalName();
        if (choiceName != null) {
            final String packageName = packageNameForGeneratedType(basePackageName, choiceNode.getPath());
            final GeneratedTypeBuilder choiceType = addDefaultInterfaceDefinition(packageName, choiceNode);
            constructGetter(typeBuilder, choiceName, choiceNode.getDescription(), choiceType);
        }
    }

    private List<GeneratedType> choiceToGeneratedType(final String basePackageName, final ChoiceNode choiceNode) {
        if (basePackageName == null) {
            throw new IllegalArgumentException("Base Package Name cannot be NULL!");
        }
        if (choiceNode == null) {
            throw new IllegalArgumentException("Choice Schema Node cannot be NULL!");
        }

        final List<GeneratedType> generatedTypes = new ArrayList<>();
        final String packageName = packageNameForGeneratedType(basePackageName, choiceNode.getPath());
        final GeneratedTypeBuilder choiceTypeBuilder = addRawInterfaceDefinition(packageName, choiceNode);
        choiceTypeBuilder.addImplementsType(Types.DATA_OBJECT);
        final GeneratedType choiceType = choiceTypeBuilder.toInstance();

        generatedTypes.add(choiceType);
        final Set<ChoiceCaseNode> caseNodes = choiceNode.getCases();
        if ((caseNodes != null) && !caseNodes.isEmpty()) {
            generatedTypes.addAll(generateTypesFromChoiceCases(basePackageName, choiceType, caseNodes));
        }
        return generatedTypes;
    }

    private List<GeneratedType> generateTypesFromChoiceCases(final String basePackageName, final Type refChoiceType,
            final Set<ChoiceCaseNode> caseNodes) {
        if (basePackageName == null) {
            throw new IllegalArgumentException("Base Package Name cannot be NULL!");
        }
        if (refChoiceType == null) {
            throw new IllegalArgumentException("Referenced Choice Type cannot be NULL!");
        }
        if (caseNodes == null) {
            throw new IllegalArgumentException("Set of Choice Case Nodes cannot be NULL!");
        }

        final List<GeneratedType> generatedTypes = new ArrayList<>();
        for (final ChoiceCaseNode caseNode : caseNodes) {
            if (caseNode != null && !caseNode.isAddedByUses()) {
                final String packageName = packageNameForGeneratedType(basePackageName, caseNode.getPath());
                final GeneratedTypeBuilder caseTypeBuilder = addDefaultInterfaceDefinition(packageName, caseNode);
                caseTypeBuilder.addImplementsType(refChoiceType);

                final Set<DataSchemaNode> childNodes = caseNode.getChildNodes();
                if (childNodes != null) {
                    resolveDataSchemaNodes(basePackageName, caseTypeBuilder, childNodes);
                }
                generatedTypes.add(caseTypeBuilder.toInstance());
            }
        }

        return generatedTypes;
    }

    private List<GeneratedType> generateTypesFromAugmentedChoiceCases(final String basePackageName,
            final Type refChoiceType, final Set<ChoiceCaseNode> caseNodes) {
        if (basePackageName == null) {
            throw new IllegalArgumentException("Base Package Name cannot be NULL!");
        }
        if (refChoiceType == null) {
            throw new IllegalArgumentException("Referenced Choice Type cannot be NULL!");
        }
        if (caseNodes == null) {
            throw new IllegalArgumentException("Set of Choice Case Nodes cannot be NULL!");
        }

        final List<GeneratedType> generatedTypes = new ArrayList<>();
        for (final ChoiceCaseNode caseNode : caseNodes) {
            if (caseNode != null && caseNode.isAugmenting()) {
                final String packageName = packageNameForGeneratedType(basePackageName, caseNode.getPath());
                final GeneratedTypeBuilder caseTypeBuilder = addDefaultInterfaceDefinition(packageName, caseNode);
                caseTypeBuilder.addImplementsType(refChoiceType);

                final Set<DataSchemaNode> childNodes = caseNode.getChildNodes();
                if (childNodes != null) {
                    resolveDataSchemaNodes(basePackageName, caseTypeBuilder, childNodes);
                }
                generatedTypes.add(caseTypeBuilder.toInstance());
            }
        }

        return generatedTypes;
    }

    private boolean resolveLeafSchemaNodeAsMethod(final GeneratedTypeBuilder typeBuilder, final LeafSchemaNode leaf) {
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
                    returnType = typeProvider.javaTypeForSchemaDefinitionType(typeDef);
                } else {
                    final EnumTypeDefinition enumTypeDef = enumTypeDefFromExtendedType(typeDef);
                    final EnumBuilder enumBuilder = resolveInnerEnumFromTypeDefinition(enumTypeDef, leafName,
                            typeBuilder);

                    if (enumBuilder != null) {
                        returnType = new ReferencedTypeImpl(enumBuilder.getPackageName(), enumBuilder.getName());
                    }
                    ((TypeProviderImpl) typeProvider).putReferencedType(leaf.getPath(), returnType);
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

    private boolean resolveLeafSchemaNodeAsProperty(final GeneratedTOBuilder toBuilder, final LeafSchemaNode leaf,
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
                final Type returnType = typeProvider.javaTypeForSchemaDefinitionType(typeDef);

                if (returnType != null) {
                    final GeneratedPropertyBuilder propBuilder = toBuilder.addProperty(parseToClassName(leafName));

                    propBuilder.setReadOnly(isReadOnly);
                    propBuilder.setReturnType(returnType);
                    propBuilder.setComment(leafDesc);

                    toBuilder.addEqualsIdentity(propBuilder);
                    toBuilder.addHashIdentity(propBuilder);
                    toBuilder.addToStringProperty(propBuilder);

                    return true;
                }
            }
        }
        return false;
    }

    private boolean resolveLeafListSchemaNode(final GeneratedTypeBuilder typeBuilder, final LeafListSchemaNode node) {
        if ((node != null) && (typeBuilder != null)) {
            final String nodeName = node.getQName().getLocalName();
            String nodeDesc = node.getDescription();
            if (nodeDesc == null) {
                nodeDesc = "";
            }

            if (nodeName != null) {
                final TypeDefinition<?> type = node.getType();
                final Type listType = Types.listTypeFor(typeProvider.javaTypeForSchemaDefinitionType(type));

                constructGetter(typeBuilder, nodeName, nodeDesc, listType);
                if (!node.isConfiguration()) {
                    constructSetter(typeBuilder, nodeName, nodeDesc, listType);
                }
                return true;
            }
        }
        return false;
    }

    private boolean resolveContainerSchemaNode(final String basePackageName, final GeneratedTypeBuilder typeBuilder,
            final ContainerSchemaNode containerNode) {
        if ((containerNode != null) && (typeBuilder != null)) {
            final String nodeName = containerNode.getQName().getLocalName();

            if (nodeName != null) {
                final String packageName = packageNameForGeneratedType(basePackageName, containerNode.getPath());

                final GeneratedTypeBuilder rawGenType = addDefaultInterfaceDefinition(packageName, containerNode);
                constructGetter(typeBuilder, nodeName, containerNode.getDescription(), rawGenType);

                return true;
            }
        }
        return false;
    }

    private boolean resolveListSchemaNode(final String basePackageName, final GeneratedTypeBuilder typeBuilder,
            final ListSchemaNode schemaNode) {
        if ((schemaNode != null) && (typeBuilder != null)) {
            final String listName = schemaNode.getQName().getLocalName();

            if (listName != null) {
                final String packageName = packageNameForGeneratedType(basePackageName, schemaNode.getPath());
                final GeneratedTypeBuilder rawGenType = addDefaultInterfaceDefinition(packageName, schemaNode);
                constructGetter(typeBuilder, listName, schemaNode.getDescription(), Types.listTypeFor(rawGenType));
                if (!schemaNode.isConfiguration()) {
                    constructSetter(typeBuilder, listName, schemaNode.getDescription(), Types.listTypeFor(rawGenType));
                }
                return true;
            }
        }
        return false;
    }

    /**
     * Method instantiates new Generated Type Builder and sets the implements
     * definitions of Data Object and Augmentable.
     * 
     * @param packageName
     *            Generated Type Package Name
     * @param schemaNode
     *            Schema Node definition
     * @return Generated Type Builder instance for Schema Node definition
     */
    private GeneratedTypeBuilder addDefaultInterfaceDefinition(final String packageName, final SchemaNode schemaNode) {
        final GeneratedTypeBuilder builder = addRawInterfaceDefinition(packageName, schemaNode, "");
        builder.addImplementsType(Types.DATA_OBJECT);
        builder.addImplementsType(Types.augmentableTypeFor(builder));
        return builder;
    }

    /**
     * 
     * @param packageName
     * @param schemaNode
     * @return
     */
    private GeneratedTypeBuilder addRawInterfaceDefinition(final String packageName, final SchemaNode schemaNode) {
        return addRawInterfaceDefinition(packageName, schemaNode, "");
    }

    private GeneratedTypeBuilder addRawInterfaceDefinition(final String packageName, final SchemaNode schemaNode,
            final String prefix) {
        if (schemaNode == null) {
            throw new IllegalArgumentException("Data Schema Node cannot be NULL!");
        }
        if (packageName == null) {
            throw new IllegalArgumentException("Package Name for Generated Type cannot be NULL!");
        }
        if (schemaNode.getQName() == null) {
            throw new IllegalArgumentException("QName for Data Schema Node cannot be NULL!");
        }
        final String schemaNodeName = schemaNode.getQName().getLocalName();
        if (schemaNodeName == null) {
            throw new IllegalArgumentException("Local Name of QName for Data Schema Node cannot be NULL!");
        }

        final String genTypeName;
        if (prefix == null) {
            genTypeName = parseToClassName(schemaNodeName);
        } else {
            genTypeName = prefix + parseToClassName(schemaNodeName);
        }

        final GeneratedTypeBuilder newType = new GeneratedTypeBuilderImpl(packageName, genTypeName);
        if (!genTypeBuilders.containsKey(packageName)) {
            final Map<String, GeneratedTypeBuilder> builders = new HashMap<>();
            builders.put(genTypeName, newType);
            genTypeBuilders.put(packageName, builders);
        } else {
            final Map<String, GeneratedTypeBuilder> builders = genTypeBuilders.get(packageName);
            if (!builders.containsKey(genTypeName)) {
                builders.put(genTypeName, newType);
            }
        }
        return newType;
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

    private MethodSignatureBuilder constructGetter(final GeneratedTypeBuilder interfaceBuilder,
            final String schemaNodeName, final String comment, final Type returnType) {
        final MethodSignatureBuilder getMethod = interfaceBuilder.addMethod(getterMethodName(schemaNodeName));

        getMethod.setComment(comment);
        getMethod.setReturnType(returnType);

        return getMethod;
    }

    private MethodSignatureBuilder constructSetter(final GeneratedTypeBuilder interfaceBuilder,
            final String schemaNodeName, final String comment, final Type parameterType) {
        final MethodSignatureBuilder setMethod = interfaceBuilder.addMethod(setterMethodName(schemaNodeName));

        setMethod.setComment(comment);
        setMethod.addParameter(parameterType, parseToValidParamName(schemaNodeName));
        setMethod.setReturnType(Types.voidType());

        return setMethod;
    }

    private List<Type> listToGenType(final String basePackageName, final ListSchemaNode list) {
        if (basePackageName == null) {
            throw new IllegalArgumentException("Package Name for Generated Type cannot be NULL!");
        }
        if (list == null) {
            throw new IllegalArgumentException("List Schema Node cannot be NULL!");
        }

        final String packageName = packageNameForGeneratedType(basePackageName, list.getPath());
        final GeneratedTypeBuilder typeBuilder = resolveListTypeBuilder(packageName, list);
        final List<String> listKeys = listKeys(list);
        GeneratedTOBuilder genTOBuilder = resolveListKeyTOBuilder(packageName, list, listKeys);

        final Set<DataSchemaNode> schemaNodes = list.getChildNodes();

        for (final DataSchemaNode schemaNode : schemaNodes) {
            if (schemaNode.isAugmenting()) {
                continue;
            }
            addSchemaNodeToListBuilders(basePackageName, schemaNode, typeBuilder, genTOBuilder, listKeys);
        }
        return typeBuildersToGenTypes(typeBuilder, genTOBuilder);
    }

    private void addSchemaNodeToListBuilders(final String basePackageName, final DataSchemaNode schemaNode,
            final GeneratedTypeBuilder typeBuilder, final GeneratedTOBuilder genTOBuilder, final List<String> listKeys) {
        if (schemaNode == null) {
            throw new IllegalArgumentException("Data Schema Node cannot be NULL!");
        }

        if (typeBuilder == null) {
            throw new IllegalArgumentException("Generated Type Builder cannot be NULL!");
        }

        if (schemaNode instanceof LeafSchemaNode) {
            final LeafSchemaNode leaf = (LeafSchemaNode) schemaNode;
            if (!isPartOfListKey(leaf, listKeys)) {
                resolveLeafSchemaNodeAsMethod(typeBuilder, leaf);
            } else {
                resolveLeafSchemaNodeAsProperty(genTOBuilder, leaf, true);
            }
        } else if (schemaNode instanceof LeafListSchemaNode) {
            resolveLeafListSchemaNode(typeBuilder, (LeafListSchemaNode) schemaNode);
        } else if (schemaNode instanceof ContainerSchemaNode) {
            resolveContainerSchemaNode(basePackageName, typeBuilder, (ContainerSchemaNode) schemaNode);
        } else if (schemaNode instanceof ListSchemaNode) {
            resolveListSchemaNode(basePackageName, typeBuilder, (ListSchemaNode) schemaNode);
        }
    }

    private List<Type> typeBuildersToGenTypes(final GeneratedTypeBuilder typeBuilder, GeneratedTOBuilder genTOBuilder) {
        final List<Type> genTypes = new ArrayList<>();
        if (typeBuilder == null) {
            throw new IllegalArgumentException("Generated Type Builder cannot be NULL!");
        }

        if (genTOBuilder != null) {
            final GeneratedTransferObject genTO = genTOBuilder.toInstance();
            constructGetter(typeBuilder, genTO.getName(), "Returns Primary Key of Yang List Type", genTO);
            genTypes.add(genTO);
        }
        genTypes.add(typeBuilder.toInstance());
        return genTypes;
    }

    /**
     * @param list
     * @return
     */
    private GeneratedTOBuilder resolveListKey(final String packageName, final ListSchemaNode list) {
        final String listName = list.getQName().getLocalName() + "Key";
        return schemaNodeToTransferObjectBuilder(packageName, list, listName);
    }

    private boolean isPartOfListKey(final LeafSchemaNode leaf, final List<String> keys) {
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

    private GeneratedTypeBuilder resolveListTypeBuilder(final String packageName, final ListSchemaNode list) {
        if (packageName == null) {
            throw new IllegalArgumentException("Package Name for Generated Type cannot be NULL!");
        }
        if (list == null) {
            throw new IllegalArgumentException("List Schema Node cannot be NULL!");
        }

        final String schemaNodeName = list.getQName().getLocalName();
        final String genTypeName = parseToClassName(schemaNodeName);

        GeneratedTypeBuilder typeBuilder = null;
        final Map<String, GeneratedTypeBuilder> builders = genTypeBuilders.get(packageName);
        if (builders != null) {
            typeBuilder = builders.get(genTypeName);
        }
        if (typeBuilder == null) {
            typeBuilder = addDefaultInterfaceDefinition(packageName, list);
        }
        return typeBuilder;
    }

    private GeneratedTOBuilder resolveListKeyTOBuilder(final String packageName, final ListSchemaNode list,
            final List<String> listKeys) {
        GeneratedTOBuilder genTOBuilder = null;
        if (listKeys.size() > 0) {
            genTOBuilder = resolveListKey(packageName, list);
        }
        return genTOBuilder;
    }
}
