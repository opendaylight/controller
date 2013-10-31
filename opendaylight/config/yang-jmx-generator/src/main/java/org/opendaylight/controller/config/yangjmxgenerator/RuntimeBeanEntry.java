/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yangjmxgenerator;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import org.opendaylight.controller.config.yangjmxgenerator.attribute.AttributeIfc;
import org.opendaylight.controller.config.yangjmxgenerator.attribute.JavaAttribute;
import org.opendaylight.controller.config.yangjmxgenerator.attribute.ListAttribute;
import org.opendaylight.controller.config.yangjmxgenerator.attribute.TOAttribute;
import org.opendaylight.controller.config.yangjmxgenerator.attribute.VoidAttribute;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.util.FullyQualifiedNameHelper;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.util.NameConflictException;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.model.api.ChoiceCaseNode;
import org.opendaylight.yangtools.yang.model.api.ContainerSchemaNode;
import org.opendaylight.yangtools.yang.model.api.DataNodeContainer;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.IdentitySchemaNode;
import org.opendaylight.yangtools.yang.model.api.LeafListSchemaNode;
import org.opendaylight.yangtools.yang.model.api.LeafSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ListSchemaNode;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.RpcDefinition;
import org.opendaylight.yangtools.yang.model.api.SchemaNode;
import org.opendaylight.yangtools.yang.model.api.UnknownSchemaNode;
import org.opendaylight.yangtools.yang.model.api.UsesNode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

/**
 * Holds information about runtime bean to be generated. There are two kinds of
 * RuntimeBeanEntry instances: if isRoot flag is set to true, this bean
 * represents state that must be present at time of configuration module
 * instantiation. Root RB must have depthLevel set to 0 and cannot have
 * children. There might be other RBs defined in yang, but no other RB can have
 * isRoot set to true. At least one RB must be root and all other RBs must be
 * lined via children so that a tree with all beans can be created.
 */
public class RuntimeBeanEntry {
    private final String packageName;
    private final String yangName, javaNamePrefix;
    private final boolean isRoot;
    private final Optional<String> keyYangName, keyJavaName;
    private final Map<String, AttributeIfc> attributeMap;
    private final List<RuntimeBeanEntry> children;
    private final Set<Rpc> rpcs;

    @VisibleForTesting
    public RuntimeBeanEntry(String packageName,
            DataSchemaNode nodeForReporting, String yangName,
            String javaNamePrefix, boolean isRoot,
            Optional<String> keyYangName, List<AttributeIfc> attributes,
            List<RuntimeBeanEntry> children, Set<Rpc> rpcs) {

        checkArgument(isRoot == false || keyYangName.isPresent() == false,
                "Root RuntimeBeanEntry must not have key " + "set");
        this.packageName = packageName;
        this.isRoot = isRoot;
        this.yangName = yangName;
        this.javaNamePrefix = javaNamePrefix;
        this.children = Collections.unmodifiableList(children);
        this.rpcs = Collections.unmodifiableSet(rpcs);

        this.keyYangName = keyYangName;
        Map<String, AttributeIfc> map = new HashMap<>();

        for (AttributeIfc a : attributes) {
            checkState(map.containsKey(a.getAttributeYangName()) == false,
                    "Attribute already defined: " + a.getAttributeYangName()
                            + " in " + nodeForReporting);
            map.put(a.getAttributeYangName(), a);
        }

        if (keyYangName.isPresent()) {
            AttributeIfc keyJavaName = map.get(keyYangName.get());
            checkArgument(keyJavaName != null, "Key " + keyYangName.get()
                    + " not found in attribute " + "list " + attributes
                    + " in " + nodeForReporting);
            this.keyJavaName = Optional
                    .of(keyJavaName.getUpperCaseCammelCase());
        } else {
            keyJavaName = Optional.absent();
        }
        attributeMap = Collections.unmodifiableMap(map);
    }

    /**
     * @return map containing all class names as key, extracted RuntimeBeans as
     *         values. If more than zero values is returned, exactly one
     *         RuntimeBeanEntry will have isRoot set to true, even if yang does
     *         not contain special configuration for it.
     */
    public static Map<String, RuntimeBeanEntry> extractClassNameToRuntimeBeanMap(
            String packageName, ChoiceCaseNode container,
            String moduleYangName, TypeProviderWrapper typeProviderWrapper,
            String javaNamePrefix, Module currentModule) {

        Map<QName, Set<RpcDefinition>> identitiesToRpcs = getIdentitiesToRpcs(currentModule);

        AttributesRpcsAndRuntimeBeans attributesRpcsAndRuntimeBeans = extractSubtree(
                packageName, container, typeProviderWrapper, currentModule,
                identitiesToRpcs);
        Map<String, RuntimeBeanEntry> result = new HashMap<>();

        List<AttributeIfc> attributes;
        Set<Rpc> rpcs;
        if (attributesRpcsAndRuntimeBeans.isEmpty() == false) {
            attributes = attributesRpcsAndRuntimeBeans.getAttributes();
            rpcs = attributesRpcsAndRuntimeBeans.getRpcs();
        } else {
            // create artificial root if not defined in yang
            attributes = Collections.emptyList();
            rpcs = Collections.emptySet();
        }
        RuntimeBeanEntry rootRuntimeBeanEntry = createRoot(packageName,
                container, moduleYangName, attributes, javaNamePrefix,
                attributesRpcsAndRuntimeBeans.getRuntimeBeanEntries(), rpcs);

        Deque<RuntimeBeanEntry> stack = new LinkedList<>();
        stack.add(rootRuntimeBeanEntry);

        while (stack.isEmpty() == false) {
            RuntimeBeanEntry first = stack.pollFirst();
            if (result.containsKey(first.getJavaNameOfRuntimeMXBean())) {
                throw new NameConflictException(
                        first.getJavaNameOfRuntimeMXBean(), null, null);
            }
            result.put(first.getJavaNameOfRuntimeMXBean(), first);
            stack.addAll(first.getChildren());
        }
        return result;
    }

    private static Map<QName/* of identity */, Set<RpcDefinition>> getIdentitiesToRpcs(
            Module currentModule) {
        // currently only looks for local identities (found in currentModule)
        Map<QName, Set<RpcDefinition>> result = new HashMap<>();
        for (IdentitySchemaNode identity : currentModule.getIdentities()) {
            // add all
            result.put(identity.getQName(), new HashSet<RpcDefinition>());
        }

        for (RpcDefinition rpc : currentModule.getRpcs()) {
            ContainerSchemaNode input = rpc.getInput();
            for (UsesNode uses : input.getUses()) {

                if (uses.getGroupingPath().getPath().size() != 1)
                    continue;

                // check grouping path
                QName qname = uses.getGroupingPath().getPath().get(0);
                if (false == qname
                        .equals(ConfigConstants.RPC_CONTEXT_REF_GROUPING_QNAME))
                    continue;

                for (SchemaNode refinedNode : uses.getRefines().values()) {

                    for (UnknownSchemaNode unknownSchemaNode : refinedNode
                            .getUnknownSchemaNodes()) {
                        if (ConfigConstants.RPC_CONTEXT_INSTANCE_EXTENSION_QNAME
                                .equals(unknownSchemaNode.getNodeType())) {
                            String localIdentityName = unknownSchemaNode
                                    .getNodeParameter();
                            QName identityQName = new QName(
                                    currentModule.getNamespace(),
                                    currentModule.getRevision(),
                                    localIdentityName);
                            Set<RpcDefinition> rpcDefinitions = result
                                    .get(identityQName);
                            if (rpcDefinitions == null) {
                                throw new IllegalArgumentException(
                                        "Identity referenced by rpc not found. Identity:"
                                                + localIdentityName + " , rpc "
                                                + rpc);
                            }
                            rpcDefinitions.add(rpc);
                        }
                    }

                }
            }
        }
        return result;
    }

    /**
     * Get direct descendants of this subtree, together with attributes defined
     * in subtree.
     */
    private static AttributesRpcsAndRuntimeBeans extractSubtree(
            String packageName, DataNodeContainer subtree,
            TypeProviderWrapper typeProviderWrapper, Module currentModule,
            Map<QName, Set<RpcDefinition>> identitiesToRpcs) {

        List<AttributeIfc> attributes = Lists.newArrayList();
        // List<JavaAttribute> javaAttributes = new ArrayList<>();
        // List<TOAttribute> toAttributes = new ArrayList<>();
        List<RuntimeBeanEntry> runtimeBeanEntries = new ArrayList<>();
        for (DataSchemaNode child : subtree.getChildNodes()) {
            // child leaves can be java attributes, TO attributes, or child
            // runtime beans
            if (child instanceof LeafSchemaNode) {
                // just save the attribute
                LeafSchemaNode leaf = (LeafSchemaNode) child;
                attributes.add(new JavaAttribute(leaf, typeProviderWrapper));
            } else if (child instanceof ContainerSchemaNode) {
                ContainerSchemaNode container = (ContainerSchemaNode) child;
                // this can be either TO or hierarchical RB
                TOAttribute toAttribute = TOAttribute.create(container,
                        typeProviderWrapper);
                attributes.add(toAttribute);
            } else if (child instanceof ListSchemaNode) {
                if (isInnerStateBean(child)) {
                    ListSchemaNode listSchemaNode = (ListSchemaNode) child;
                    RuntimeBeanEntry hierarchicalChild = createHierarchical(
                            packageName, listSchemaNode, typeProviderWrapper,
                            currentModule, identitiesToRpcs);
                    runtimeBeanEntries.add(hierarchicalChild);
                } else /* ordinary list attribute */{
                    ListAttribute listAttribute = ListAttribute.create(
                            (ListSchemaNode) child, typeProviderWrapper);
                    attributes.add(listAttribute);
                }

            } else if (child instanceof LeafListSchemaNode) {
                ListAttribute listAttribute = ListAttribute.create(
                        (LeafListSchemaNode) child, typeProviderWrapper);
                attributes.add(listAttribute);
            } else {
                throw new IllegalStateException("Unexpected running-data node "
                        + child);
            }
        }
        Set<Rpc> rpcs = new HashSet<>();
        SchemaNode subtreeSchemaNode = (SchemaNode) subtree;
        for (UnknownSchemaNode unknownSchemaNode : subtreeSchemaNode
                .getUnknownSchemaNodes()) {
            if (ConfigConstants.RPC_CONTEXT_INSTANCE_EXTENSION_QNAME
                    .equals(unknownSchemaNode.getNodeType())) {
                String localIdentityName = unknownSchemaNode.getNodeParameter();
                QName identityQName = new QName(currentModule.getNamespace(),
                        currentModule.getRevision(), localIdentityName);
                Set<RpcDefinition> rpcDefinitions = identitiesToRpcs
                        .get(identityQName);
                if (rpcDefinitions == null) {
                    throw new IllegalArgumentException("Cannot find identity "
                            + localIdentityName + " to be used as "
                            + "context reference when resolving "
                            + unknownSchemaNode);
                }
                // convert RpcDefinition to Rpc
                for (RpcDefinition rpcDefinition : rpcDefinitions) {
                    String name = ModuleMXBeanEntry
                            .findJavaParameter(rpcDefinition);
                    AttributeIfc returnType;
                    if (rpcDefinition.getOutput() == null
                            || rpcDefinition.getOutput().getChildNodes().size() == 0) {
                        returnType = VoidAttribute.getInstance();
                    } else if (rpcDefinition.getOutput().getChildNodes().size() == 1) {
                        DataSchemaNode returnDSN = rpcDefinition.getOutput()
                                .getChildNodes().iterator().next();
                        returnType = getReturnTypeAttribute(returnDSN, typeProviderWrapper);

                    } else {
                        throw new IllegalArgumentException(
                                "More than one child node in rpc output is not supported. "
                                        + "Error occured in " + rpcDefinition);
                    }
                    List<JavaAttribute> parameters = new ArrayList<>();
                    for (DataSchemaNode childNode : sortAttributes(rpcDefinition.getInput()
                            .getChildNodes())) {
                        if (childNode.isAddedByUses() == false) { // skip
                                                                  // refined
                                                                  // context-instance
                            checkArgument(childNode instanceof LeafSchemaNode, "Unexpected type of rpc input type. "
                                    + "Currently only leafs and empty output nodes are supported, got " + childNode);
                            JavaAttribute javaAttribute = new JavaAttribute(
                                    (LeafSchemaNode) childNode,
                                    typeProviderWrapper);
                            parameters.add(javaAttribute);
                        }
                    }
                    Rpc newRpc = new Rpc(returnType, name, rpcDefinition
                            .getQName().getLocalName(), parameters);
                    rpcs.add(newRpc);
                }
            }
        }
        return new AttributesRpcsAndRuntimeBeans(runtimeBeanEntries,
                attributes, rpcs);
    }

    private static AttributeIfc getReturnTypeAttribute(DataSchemaNode child, TypeProviderWrapper typeProviderWrapper) {
        if (child instanceof LeafSchemaNode) {
            LeafSchemaNode leaf = (LeafSchemaNode) child;
            return new JavaAttribute(leaf, typeProviderWrapper);
        } else if (child instanceof ContainerSchemaNode) {
            ContainerSchemaNode container = (ContainerSchemaNode) child;
            TOAttribute toAttribute = TOAttribute.create(container, typeProviderWrapper);
            return toAttribute;
        } else if (child instanceof ListSchemaNode) {
            return ListAttribute.create((ListSchemaNode) child, typeProviderWrapper);
        } else if (child instanceof LeafListSchemaNode) {
            return ListAttribute.create((LeafListSchemaNode) child, typeProviderWrapper);
        } else {
            throw new IllegalStateException("Unknown output data node " + child + " for rpc");
        }
    }

    private static Collection<DataSchemaNode> sortAttributes(Set<DataSchemaNode> childNodes) {
        final TreeSet<DataSchemaNode> dataSchemaNodes = new TreeSet<>(new Comparator<DataSchemaNode>() {
            @Override
            public int compare(DataSchemaNode o1, DataSchemaNode o2) {
                return o1.getQName().getLocalName().compareTo(o2.getQName().getLocalName());
            }
        });
        dataSchemaNodes.addAll(childNodes);
        return dataSchemaNodes;
    }

    private static boolean isInnerStateBean(DataSchemaNode child) {
        for (UnknownSchemaNode unknownSchemaNode : child
                .getUnknownSchemaNodes()) {
            if (unknownSchemaNode.getNodeType().equals(
                    ConfigConstants.INNER_STATE_BEAN_EXTENSION_QNAME))
                return true;
        }
        return false;
    }

    private static RuntimeBeanEntry createHierarchical(String packageName,
            ListSchemaNode listSchemaNode,
            TypeProviderWrapper typeProviderWrapper, Module currentModule,
            Map<QName, Set<RpcDefinition>> identitiesToRpcs) {

        // supported are numeric types, strings, enums
        // get all attributes
        AttributesRpcsAndRuntimeBeans attributesRpcsAndRuntimeBeans = extractSubtree(
                packageName, listSchemaNode, typeProviderWrapper,
                currentModule, identitiesToRpcs);

        Optional<String> keyYangName;
        if (listSchemaNode.getKeyDefinition().size() == 0) {
            keyYangName = Optional.absent();
        } else if (listSchemaNode.getKeyDefinition().size() == 1) {
            // key must be either null or one of supported key types
            QName keyQName = listSchemaNode.getKeyDefinition().iterator()
                    .next();
            keyYangName = Optional.of(keyQName.getLocalName());

        } else {
            throw new IllegalArgumentException(
                    "More than one key is not supported in " + listSchemaNode);
        }

        String javaNamePrefix = ModuleMXBeanEntry
                .findJavaNamePrefix(listSchemaNode);

        RuntimeBeanEntry rbFromAttributes = new RuntimeBeanEntry(packageName,
                listSchemaNode, listSchemaNode.getQName().getLocalName(),
                javaNamePrefix, false, keyYangName,
                attributesRpcsAndRuntimeBeans.getAttributes(),
                attributesRpcsAndRuntimeBeans.getRuntimeBeanEntries(),
                attributesRpcsAndRuntimeBeans.getRpcs());

        return rbFromAttributes;
    }

    private static RuntimeBeanEntry createRoot(String packageName,
            DataSchemaNode nodeForReporting, String attributeYangName,
            List<AttributeIfc> attributes, String javaNamePrefix,
            List<RuntimeBeanEntry> children, Set<Rpc> rpcs) {
        return new RuntimeBeanEntry(packageName, nodeForReporting,
                attributeYangName, javaNamePrefix, true,
                Optional.<String> absent(), attributes, children, rpcs);
    }

    public boolean isRoot() {
        return isRoot;
    }

    public Optional<String> getKeyYangName() {
        return keyYangName;
    }

    public Optional<String> getKeyJavaName() {
        return keyJavaName;
    }

    public Collection<AttributeIfc> getAttributes() {
        return attributeMap.values();
    }

    public Map<String, AttributeIfc> getYangPropertiesToTypesMap() {
        return attributeMap;
    }

    public String getYangName() {
        return yangName;
    }

    public String getPackageName() {
        return packageName;
    }

    public String getJavaNamePrefix() {
        return javaNamePrefix;
    }

    public List<RuntimeBeanEntry> getChildren() {
        return children;
    }

    public Set<Rpc> getRpcs() {
        return rpcs;
    }

    private static class AttributesRpcsAndRuntimeBeans {
        private final List<RuntimeBeanEntry> runtimeBeanEntries;
        private final List<AttributeIfc> attributes;
        private final Set<Rpc> rpcs;

        public AttributesRpcsAndRuntimeBeans(
                List<RuntimeBeanEntry> runtimeBeanEntries,
                List<AttributeIfc> attributes, Set<Rpc> rpcs) {
            this.runtimeBeanEntries = runtimeBeanEntries;
            this.attributes = attributes;
            this.rpcs = rpcs;
        }

        private List<AttributeIfc> getAttributes() {
            return attributes;
        }

        public List<RuntimeBeanEntry> getRuntimeBeanEntries() {
            return runtimeBeanEntries;
        }

        public boolean isEmpty() {
            return attributes.isEmpty() && rpcs.isEmpty();
        }

        private Set<Rpc> getRpcs() {
            return rpcs;
        }
    }

    public static class Rpc {
        private final String name;
        private final List<JavaAttribute> parameters;
        private final AttributeIfc returnType;
        private final String yangName;

        Rpc(AttributeIfc returnType, String name, String yangName,
                List<JavaAttribute> parameters) {
            this.returnType = returnType;
            this.name = name;
            this.parameters = parameters;
            this.yangName = yangName;
        }

        public String getYangName() {
            return yangName;
        }

        public String getName() {
            return name;
        }

        public List<JavaAttribute> getParameters() {
            return parameters;
        }

        public AttributeIfc getReturnType() {
            return returnType;
        }
    }

    private static final String MXBEAN_SUFFIX = "RuntimeMXBean";

    public String getJavaNameOfRuntimeMXBean() {
        return getJavaNameOfRuntimeMXBean(javaNamePrefix);
    }

    public String getFullyQualifiedName(String typeName) {
        return FullyQualifiedNameHelper.getFullyQualifiedName(packageName,
                typeName);
    }

    private static String getJavaNameOfRuntimeMXBean(String javaNamePrefix) {
        return javaNamePrefix + MXBEAN_SUFFIX;
    }

    @Override
    public String toString() {
        return "RuntimeBeanEntry{" + "isRoot=" + isRoot + ", yangName='"
                + yangName + '\'' + ", packageName='" + packageName + '\''
                + ", keyYangName=" + keyYangName + '}';
    }
}
