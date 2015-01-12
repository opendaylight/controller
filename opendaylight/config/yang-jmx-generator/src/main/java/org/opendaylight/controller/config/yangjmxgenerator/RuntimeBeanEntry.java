/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yangjmxgenerator;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Collections2;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.opendaylight.controller.config.yangjmxgenerator.attribute.AttributeIfc;
import org.opendaylight.controller.config.yangjmxgenerator.attribute.JavaAttribute;
import org.opendaylight.controller.config.yangjmxgenerator.attribute.ListAttribute;
import org.opendaylight.controller.config.yangjmxgenerator.attribute.TOAttribute;
import org.opendaylight.controller.config.yangjmxgenerator.attribute.VoidAttribute;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.util.FullyQualifiedNameHelper;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.util.NameConflictException;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.model.api.ContainerSchemaNode;
import org.opendaylight.yangtools.yang.model.api.DataNodeContainer;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.LeafListSchemaNode;
import org.opendaylight.yangtools.yang.model.api.LeafSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ListSchemaNode;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.RpcDefinition;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaNode;
import org.opendaylight.yangtools.yang.model.api.UnknownSchemaNode;
import org.opendaylight.yangtools.yang.model.api.UsesNode;
import org.opendaylight.yangtools.yang.model.util.SchemaContextUtil;

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

    private static final Function<SchemaNode, QName> QNAME_FROM_NODE = new Function<SchemaNode, QName>() {
        @Override
        public QName apply(final SchemaNode input) {
            return input.getQName();
        }
    };

    private static final Function<UnknownSchemaNode, String> UNKNOWN_NODE_TO_STRING = new Function<UnknownSchemaNode, String>() {
        @Override
        public String apply(final UnknownSchemaNode input) {
            return input.getQName().getLocalName() + input.getNodeParameter();
        }
    };

    private final String packageName;
    private final String yangName, javaNamePrefix;
    private final boolean isRoot;
    private final Optional<String> keyYangName, keyJavaName;
    private final Map<String, AttributeIfc> attributeMap;
    private final List<RuntimeBeanEntry> children;
    private final Set<Rpc> rpcs;

    @VisibleForTesting
    RuntimeBeanEntry(final String packageName,
            final DataNodeContainer nodeForReporting, final String yangName,
            final String javaNamePrefix, final boolean isRoot,
            final Optional<String> keyYangName, final List<AttributeIfc> attributes,
            final List<RuntimeBeanEntry> children, final Set<Rpc> rpcs) {

        checkArgument(isRoot == false || keyYangName.isPresent() == false,
                "Root RuntimeBeanEntry must not have key set");
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
                    "Attribute already defined: %s in %s", a.getAttributeYangName(), nodeForReporting);
            map.put(a.getAttributeYangName(), a);
        }

        if (keyYangName.isPresent()) {
            AttributeIfc keyJavaName = map.get(keyYangName.get());
            checkArgument(keyJavaName != null, "Key %s not found in attribute list %s in %s", keyYangName.get(),
                    attributes, nodeForReporting);
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
            final String packageName, final DataNodeContainer container,
            final String moduleYangName, final TypeProviderWrapper typeProviderWrapper,
            final String javaNamePrefix, final Module currentModule, final SchemaContext schemaContext) {


        AttributesRpcsAndRuntimeBeans attributesRpcsAndRuntimeBeans = extractSubtree(
                packageName, container, typeProviderWrapper, currentModule,
                schemaContext);
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

    private static Multimap<QName/* of identity */, RpcDefinition> getIdentitiesToRpcs(
            final SchemaContext schemaCtx) {
        Multimap<QName, RpcDefinition> result = HashMultimap.create();
        for (Module currentModule : schemaCtx.getModules()) {

            // Find all identities in current module for later identity->rpc mapping
            Set<QName> allIdentitiesInModule = Sets.newHashSet(Collections2.transform(currentModule.getIdentities(), QNAME_FROM_NODE));

            for (RpcDefinition rpc : currentModule.getRpcs()) {
                ContainerSchemaNode input = rpc.getInput();
                if (input != null) {
                    for (UsesNode uses : input.getUses()) {

                        // Check if the rpc is config rpc by looking for input argument rpc-context-ref
                        Iterator<QName> pathFromRoot = uses.getGroupingPath().getPathFromRoot().iterator();
                        if (!pathFromRoot.hasNext() ||
                                !pathFromRoot.next().equals(ConfigConstants.RPC_CONTEXT_REF_GROUPING_QNAME)) {
                            continue;
                        }

                        for (SchemaNode refinedNode : uses.getRefines().values()) {
                            for (UnknownSchemaNode unknownSchemaNode : refinedNode
                                    .getUnknownSchemaNodes()) {
                                if (ConfigConstants.RPC_CONTEXT_INSTANCE_EXTENSION_QNAME
                                        .equals(unknownSchemaNode.getNodeType())) {
                                    String localIdentityName = unknownSchemaNode
                                            .getNodeParameter();
                                    QName identityQName = QName.create(
                                            currentModule.getNamespace(),
                                            currentModule.getRevision(),
                                            localIdentityName);
                                    Preconditions.checkArgument(allIdentitiesInModule.contains(identityQName),
                                            "Identity referenced by rpc not found. Identity: %s, rpc: %s", localIdentityName, rpc);
                                    result.put(identityQName, rpc);
                                }
                            }
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
            final String packageName, final DataNodeContainer subtree,
            final TypeProviderWrapper typeProviderWrapper, final Module currentModule,
            final SchemaContext ctx) {

        Multimap<QName, RpcDefinition> identitiesToRpcs = getIdentitiesToRpcs(ctx);

        List<AttributeIfc> attributes = Lists.newArrayList();
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
                        typeProviderWrapper, packageName);
                attributes.add(toAttribute);
            } else if (child instanceof ListSchemaNode) {
                if (isInnerStateBean(child)) {
                    ListSchemaNode listSchemaNode = (ListSchemaNode) child;
                    RuntimeBeanEntry hierarchicalChild = createHierarchical(
                            packageName, listSchemaNode, typeProviderWrapper,
                            currentModule, ctx);
                    runtimeBeanEntries.add(hierarchicalChild);
                } else /* ordinary list attribute */{
                    ListAttribute listAttribute = ListAttribute.create(
                            (ListSchemaNode) child, typeProviderWrapper, packageName);
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
                QName identityQName = unknownSchemaNode.isAddedByUses() ?
                        findQNameFromGrouping(subtree, ctx, unknownSchemaNode, localIdentityName) :
                        QName.create(currentModule.getNamespace(), currentModule.getRevision(), localIdentityName);
                // convert RpcDefinition to Rpc
                for (RpcDefinition rpcDefinition : identitiesToRpcs.get(identityQName)) {
                    String name = TypeProviderWrapper
                            .findJavaParameter(rpcDefinition);
                    AttributeIfc returnType;
                    if (rpcDefinition.getOutput() == null
                            || rpcDefinition.getOutput().getChildNodes().isEmpty()) {
                        returnType = VoidAttribute.getInstance();
                    } else if (rpcDefinition.getOutput().getChildNodes().size() == 1) {
                        DataSchemaNode returnDSN = rpcDefinition.getOutput()
                                .getChildNodes().iterator().next();
                        returnType = getReturnTypeAttribute(returnDSN, typeProviderWrapper, packageName);

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

    /**
     * Find "proper" qname of unknown node in case it comes from a grouping
     */
    private static QName findQNameFromGrouping(final DataNodeContainer subtree, final SchemaContext ctx, final UnknownSchemaNode unknownSchemaNode, final String localIdentityName) {
        QName identityQName = null;
        for (UsesNode usesNode : subtree.getUses()) {
            SchemaNode dataChildByName = SchemaContextUtil.findDataSchemaNode(ctx, usesNode.getGroupingPath());
            Module m = SchemaContextUtil.findParentModule(ctx, dataChildByName);
            List<UnknownSchemaNode> unknownSchemaNodes = dataChildByName.getUnknownSchemaNodes();
            if(Collections2.transform(unknownSchemaNodes, UNKNOWN_NODE_TO_STRING).contains(UNKNOWN_NODE_TO_STRING.apply(unknownSchemaNode))) {
                identityQName = QName.create(dataChildByName.getQName(), localIdentityName);
            }
        }
        return identityQName;
    }

    private static AttributeIfc getReturnTypeAttribute(final DataSchemaNode child, final TypeProviderWrapper typeProviderWrapper,
            final String packageName) {
        if (child instanceof LeafSchemaNode) {
            LeafSchemaNode leaf = (LeafSchemaNode) child;
            return new JavaAttribute(leaf, typeProviderWrapper);
        } else if (child instanceof ContainerSchemaNode) {
            ContainerSchemaNode container = (ContainerSchemaNode) child;
            TOAttribute toAttribute = TOAttribute.create(container, typeProviderWrapper, packageName);
            return toAttribute;
        } else if (child instanceof ListSchemaNode) {
            return ListAttribute.create((ListSchemaNode) child, typeProviderWrapper, packageName);
        } else if (child instanceof LeafListSchemaNode) {
            return ListAttribute.create((LeafListSchemaNode) child, typeProviderWrapper);
        } else {
            throw new IllegalStateException("Unknown output data node " + child + " for rpc");
        }
    }

    private static Collection<DataSchemaNode> sortAttributes(final Collection<DataSchemaNode> childNodes) {
        final TreeSet<DataSchemaNode> dataSchemaNodes = new TreeSet<>(new Comparator<DataSchemaNode>() {
            @Override
            public int compare(final DataSchemaNode o1, final DataSchemaNode o2) {
                return o1.getQName().getLocalName().compareTo(o2.getQName().getLocalName());
            }
        });
        dataSchemaNodes.addAll(childNodes);
        return dataSchemaNodes;
    }

    private static boolean isInnerStateBean(final DataSchemaNode child) {
        for (UnknownSchemaNode unknownSchemaNode : child
                .getUnknownSchemaNodes()) {
            if (unknownSchemaNode.getNodeType().equals(
                    ConfigConstants.INNER_STATE_BEAN_EXTENSION_QNAME)) {
                return true;
            }
        }
        return false;
    }

    private static RuntimeBeanEntry createHierarchical(final String packageName,
            final ListSchemaNode listSchemaNode,
            final TypeProviderWrapper typeProviderWrapper, final Module currentModule,
            final SchemaContext ctx) {

        // supported are numeric types, strings, enums
        // get all attributes
        AttributesRpcsAndRuntimeBeans attributesRpcsAndRuntimeBeans = extractSubtree(
                packageName, listSchemaNode, typeProviderWrapper,
                currentModule, ctx);

        Optional<String> keyYangName;
        if (listSchemaNode.getKeyDefinition().isEmpty()) {
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

        String javaNamePrefix = TypeProviderWrapper
                .findJavaNamePrefix(listSchemaNode);

        RuntimeBeanEntry rbFromAttributes = new RuntimeBeanEntry(packageName,
                listSchemaNode, listSchemaNode.getQName().getLocalName(),
                javaNamePrefix, false, keyYangName,
                attributesRpcsAndRuntimeBeans.getAttributes(),
                attributesRpcsAndRuntimeBeans.getRuntimeBeanEntries(),
                attributesRpcsAndRuntimeBeans.getRpcs());

        return rbFromAttributes;
    }

    private static RuntimeBeanEntry createRoot(final String packageName,
            final DataNodeContainer nodeForReporting, final String attributeYangName,
            final List<AttributeIfc> attributes, final String javaNamePrefix,
            final List<RuntimeBeanEntry> children, final Set<Rpc> rpcs) {
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
                final List<RuntimeBeanEntry> runtimeBeanEntries,
                final List<AttributeIfc> attributes, final Set<Rpc> rpcs) {
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

        Rpc(final AttributeIfc returnType, final String name, final String yangName,
                final List<JavaAttribute> parameters) {
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

    public String getFullyQualifiedName(final String typeName) {
        return FullyQualifiedNameHelper.getFullyQualifiedName(packageName,
                typeName);
    }

    private static String getJavaNameOfRuntimeMXBean(final String javaNamePrefix) {
        return javaNamePrefix + MXBEAN_SUFFIX;
    }

    @Override
    public String toString() {
        return "RuntimeBeanEntry{" + "isRoot=" + isRoot + ", yangName='"
                + yangName + '\'' + ", packageName='" + packageName + '\''
                + ", keyYangName=" + keyYangName + '}';
    }
}
