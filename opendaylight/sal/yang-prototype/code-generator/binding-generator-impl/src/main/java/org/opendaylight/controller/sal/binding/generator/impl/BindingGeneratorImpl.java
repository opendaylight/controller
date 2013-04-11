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
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.opendaylight.controller.binding.generator.util.CodeGeneratorHelper;
import org.opendaylight.controller.binding.generator.util.Types;
import org.opendaylight.controller.sal.binding.generator.api.BindingGenerator;
import org.opendaylight.controller.sal.binding.generator.spi.TypeProvider;
import org.opendaylight.controller.sal.binding.model.api.GeneratedTransferObject;
import org.opendaylight.controller.sal.binding.model.api.GeneratedType;
import org.opendaylight.controller.sal.binding.model.api.Type;
import org.opendaylight.controller.sal.binding.model.api.type.builder.GeneratedPropertyBuilder;
import org.opendaylight.controller.sal.binding.model.api.type.builder.GeneratedTOBuilder;
import org.opendaylight.controller.sal.binding.model.api.type.builder.GeneratedTypeBuilder;
import org.opendaylight.controller.sal.binding.model.api.type.builder.MethodSignatureBuilder;
import org.opendaylight.controller.sal.binding.yang.types.TypeProviderImpl;
import org.opendaylight.controller.yang.common.QName;
import org.opendaylight.controller.yang.model.api.ContainerSchemaNode;
import org.opendaylight.controller.yang.model.api.DataNodeContainer;
import org.opendaylight.controller.yang.model.api.DataSchemaNode;
import org.opendaylight.controller.yang.model.api.LeafListSchemaNode;
import org.opendaylight.controller.yang.model.api.LeafSchemaNode;
import org.opendaylight.controller.yang.model.api.ListSchemaNode;
import org.opendaylight.controller.yang.model.api.Module;
import org.opendaylight.controller.yang.model.api.SchemaContext;
import org.opendaylight.controller.yang.model.api.SchemaPath;
import org.opendaylight.controller.yang.model.api.TypeDefinition;

public class BindingGeneratorImpl implements BindingGenerator {

    private static Calendar calendar = new GregorianCalendar();
    private Map<String, Map<String, GeneratedTypeBuilder>> genTypeBuilders;
    private List<ContainerSchemaNode> schemaContainers;
    private List<ListSchemaNode> schemaLists;
    private TypeProvider typeProvider;
    private String basePackageName;

    public BindingGeneratorImpl() {
        super();
    }

    @Override
    public List<Type> generateTypes(final SchemaContext context) {
        final List<Type> genTypes = new ArrayList<Type>();
        
        typeProvider = new TypeProviderImpl(context);
        if (context != null) {
            final Set<Module> modules = context.getModules();
            
            if (modules != null) {
                for (final Module module : modules) {
                    genTypeBuilders = new HashMap<String, Map<String, GeneratedTypeBuilder>>();
                    schemaContainers = new ArrayList<ContainerSchemaNode>();
                    schemaLists = new ArrayList<ListSchemaNode>();
                    
                    basePackageName = resolveBasePackageName(module.getNamespace(),
                            module.getYangVersion());

                    traverseModule(module);
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
                }
            }
        }

        return genTypes;
    }
    
    private String resolveGeneratedTypePackageName(final SchemaPath schemaPath) {
        final StringBuilder builder = new StringBuilder();
        builder.append(basePackageName);
        if ((schemaPath != null) && (schemaPath.getPath() != null)) {
            final List<QName> pathToNode = schemaPath.getPath();
            final int traversalSteps = (pathToNode.size() - 1); 
            for (int i = 0; i < traversalSteps; ++i) {
                builder.append(".");
                String nodeLocalName = pathToNode.get(i).getLocalName();

                // TODO: create method
                nodeLocalName = nodeLocalName.replace(":", ".");
                nodeLocalName = nodeLocalName.replace("-", ".");
                builder.append(nodeLocalName);
            }
            return builder.toString();
        }
        return null;
    }

    private GeneratedType containerToGenType(ContainerSchemaNode container) {
        if (container == null) {
            return null;
        }
        final Set<DataSchemaNode> schemaNodes = container.getChildNodes();
        final GeneratedTypeBuilder typeBuilder = addRawInterfaceDefinition(container);

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
        return typeBuilder.toInstance();
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
                final Type javaType = typeProvider
                        .javaTypeForSchemaDefinitionType(typeDef);

                constructGetter(typeBuilder, leafName, leafDesc, javaType);
                if (!leaf.isConfiguration()) {
                    constructSetter(typeBuilder, leafName, leafDesc, javaType);
                }
                return true;
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
                
                //TODO: properly resolve enum types
                final Type javaType = typeProvider
                        .javaTypeForSchemaDefinitionType(typeDef);

                final GeneratedPropertyBuilder propBuilder = toBuilder
                        .addProperty(CodeGeneratorHelper
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

        final String packageName = resolveGeneratedTypePackageName(schemaNode
                .getPath());
        final String schemaNodeName = schemaNode.getQName().getLocalName();

        if ((packageName != null) && (schemaNode != null)
                && (schemaNodeName != null)) {
            final String genTypeName = CodeGeneratorHelper
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
        method.append(CodeGeneratorHelper.parseToClassName(methodName));
        return method.toString();
    }

    private String setterMethodName(final String methodName) {
        final StringBuilder method = new StringBuilder();
        method.append("set");
        method.append(CodeGeneratorHelper.parseToClassName(methodName));
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
                CodeGeneratorHelper.parseToParamName(schemaNodeName));
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
    private GeneratedTOBuilder resolveListKey(final ListSchemaNode list) {
        final String packageName = resolveGeneratedTypePackageName(list
                .getPath());
        final String listName = list.getQName().getLocalName() + "Key";

        if ((packageName != null) && (list != null) && (listName != null)) {
            final String genTOName = CodeGeneratorHelper
                    .parseToClassName(listName);
            final GeneratedTOBuilder newType = new GeneratedTOBuilderImpl(
                    packageName, genTOName);

            return newType;
        }
        return null;
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
        final String packageName = resolveGeneratedTypePackageName(list
                .getPath());
        final String schemaNodeName = list.getQName().getLocalName();
        final String genTypeName = CodeGeneratorHelper
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

    private void traverseModule(final Module module) {
        final Set<DataSchemaNode> schemaNodes = module.getChildNodes();

        for (DataSchemaNode node : schemaNodes) {
            if (node instanceof ContainerSchemaNode) {
                schemaContainers.add((ContainerSchemaNode) node);
                traverse((ContainerSchemaNode) node);
            }
        }
    }

    private void traverse(final DataNodeContainer dataNode) {
        if (!containChildDataNodeContainer(dataNode)) {
            return;
        }

        final Set<DataSchemaNode> childs = dataNode.getChildNodes();
        if (childs != null) {
            for (DataSchemaNode childNode : childs) {
                if (childNode instanceof ContainerSchemaNode) {
                    final ContainerSchemaNode container = (ContainerSchemaNode) childNode;
                    schemaContainers.add(container);
                    traverse(container);
                }

                if (childNode instanceof ListSchemaNode) {
                    final ListSchemaNode list = (ListSchemaNode) childNode;
                    schemaLists.add(list);
                    traverse(list);
                }
            }
        }
    }

    /**
     * Returns <code>true</code> if and only if the child node contain at least
     * one child container schema node or child list schema node, otherwise will
     * always returns <code>false</code>
     * 
     * @param container
     * @return <code>true</code> if and only if the child node contain at least
     *         one child container schema node or child list schema node,
     *         otherwise will always returns <code>false</code>
     */
    private boolean containChildDataNodeContainer(
            final DataNodeContainer container) {
        if (container != null) {
            final Set<DataSchemaNode> childs = container.getChildNodes();
            if ((childs != null) && (childs.size() > 0)) {
                for (final DataSchemaNode childNode : childs) {
                    if (childNode instanceof DataNodeContainer) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
