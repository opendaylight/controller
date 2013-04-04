/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.yang.model.parser.builder.impl;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.opendaylight.controller.yang.common.QName;
import org.opendaylight.controller.yang.model.api.AugmentationSchema;
import org.opendaylight.controller.yang.model.api.DataSchemaNode;
import org.opendaylight.controller.yang.model.api.Deviation;
import org.opendaylight.controller.yang.model.api.ExtensionDefinition;
import org.opendaylight.controller.yang.model.api.FeatureDefinition;
import org.opendaylight.controller.yang.model.api.GroupingDefinition;
import org.opendaylight.controller.yang.model.api.IdentitySchemaNode;
import org.opendaylight.controller.yang.model.api.Module;
import org.opendaylight.controller.yang.model.api.ModuleImport;
import org.opendaylight.controller.yang.model.api.NotificationDefinition;
import org.opendaylight.controller.yang.model.api.RpcDefinition;
import org.opendaylight.controller.yang.model.api.TypeDefinition;
import org.opendaylight.controller.yang.model.api.UsesNode;
import org.opendaylight.controller.yang.model.parser.builder.api.AugmentationSchemaBuilder;
import org.opendaylight.controller.yang.model.parser.builder.api.Builder;
import org.opendaylight.controller.yang.model.parser.builder.api.ChildNodeBuilder;
import org.opendaylight.controller.yang.model.parser.builder.api.DataSchemaNodeBuilder;
import org.opendaylight.controller.yang.model.parser.builder.api.GroupingBuilder;
import org.opendaylight.controller.yang.model.parser.builder.api.TypeAwareBuilder;
import org.opendaylight.controller.yang.model.parser.builder.api.TypeDefinitionAwareBuilder;
import org.opendaylight.controller.yang.model.parser.builder.api.TypeDefinitionBuilder;
import org.opendaylight.controller.yang.model.parser.builder.api.UsesNodeBuilder;

/**
 * This builder builds Module object. If this module is dependent on external
 * module/modules, these dependencies must be resolved before module is built,
 * otherwise result may not be valid.
 */
public class ModuleBuilder implements Builder {

    private final ModuleImpl instance;
    private final String name;
    private String prefix;
    private Date revision;

    private final Set<ModuleImport> imports = new HashSet<ModuleImport>();
    private Set<AugmentationSchema> augmentations;

    /**
     * All nodes, that can contain other nodes
     */
    private final Map<List<String>, Builder> moduleNodes = new HashMap<List<String>, Builder>();

    /**
     * Holds all child (DataSchemaNode) nodes: anyxml, choice, case, container,
     * list, leaf, leaf-list.
     */
    private final Map<List<String>, DataSchemaNodeBuilder> addedChilds = new HashMap<List<String>, DataSchemaNodeBuilder>();

    private final Map<List<String>, GroupingBuilder> addedGroupings = new HashMap<List<String>, GroupingBuilder>();
    private final Set<AugmentationSchemaBuilder> addedAugments = new HashSet<AugmentationSchemaBuilder>();
    private final Map<List<String>, UsesNodeBuilder> addedUsesNodes = new HashMap<List<String>, UsesNodeBuilder>();
    private final Map<List<String>, RpcDefinitionBuilder> addedRpcs = new HashMap<List<String>, RpcDefinitionBuilder>();
    private final Set<NotificationBuilder> addedNotifications = new HashSet<NotificationBuilder>();
    private final Set<IdentitySchemaNodeBuilder> addedIdentities = new HashSet<IdentitySchemaNodeBuilder>();
    private final Map<List<String>, FeatureBuilder> addedFeatures = new HashMap<List<String>, FeatureBuilder>();
    private final Map<String, DeviationBuilder> addedDeviations = new HashMap<String, DeviationBuilder>();
    private final Map<List<String>, TypeDefinitionBuilder> addedTypedefs = new HashMap<List<String>, TypeDefinitionBuilder>();
    private final List<ExtensionBuilder> addedExtensions = new ArrayList<ExtensionBuilder>();

    private final Map<List<String>, TypeAwareBuilder> dirtyNodes = new HashMap<List<String>, TypeAwareBuilder>();
    private final Map<List<String>, UnionTypeBuilder> unionTypes = new HashMap<List<String>, UnionTypeBuilder>();

    public ModuleBuilder(String name) {
        this.name = name;
        instance = new ModuleImpl(name);
    }


    /**
     * Build new Module object based on this builder.
     */
    @Override
    public Module build() {
        instance.setImports(imports);

        // TYPEDEFS
        final Set<TypeDefinition<?>> typedefs = buildModuleTypedefs(addedTypedefs);
        instance.setTypeDefinitions(typedefs);

        // CHILD NODES
        final Map<QName, DataSchemaNode> childNodes = buildModuleChildNodes(addedChilds);
        instance.setChildNodes(childNodes);

        // GROUPINGS
        final Set<GroupingDefinition> groupings = buildModuleGroupings(addedGroupings);
        instance.setGroupings(groupings);

        // USES
        final Set<UsesNode> usesNodeDefinitions = buildUsesNodes(addedUsesNodes);
        instance.setUses(usesNodeDefinitions);

        // FEATURES
        final Set<FeatureDefinition> features = buildModuleFeatures(addedFeatures);
        instance.setFeatures(features);

        // NOTIFICATIONS
        final Set<NotificationDefinition> notifications = new HashSet<NotificationDefinition>();
        for (NotificationBuilder entry : addedNotifications) {
            notifications.add((NotificationDefinition) entry.build());
        }
        instance.setNotifications(notifications);

        // AUGMENTATIONS
        instance.setAugmentations(augmentations);

        // RPCs
        final Set<RpcDefinition> rpcs = buildModuleRpcs(addedRpcs);
        instance.setRpcs(rpcs);

        // DEVIATIONS
        final Set<Deviation> deviations = new HashSet<Deviation>();
        for (Map.Entry<String, DeviationBuilder> entry : addedDeviations
                .entrySet()) {
            deviations.add(entry.getValue().build());
        }
        instance.setDeviations(deviations);

        // EXTENSIONS
        final List<ExtensionDefinition> extensions = new ArrayList<ExtensionDefinition>();
        for(ExtensionBuilder b : addedExtensions) {
            extensions.add(b.build());
        }
        instance.setExtensionSchemaNodes(extensions);

        // IDENTITIES
        final Set<IdentitySchemaNode> identities = new HashSet<IdentitySchemaNode>();
        for(IdentitySchemaNodeBuilder idBuilder : addedIdentities) {
            identities.add(idBuilder.build());
        }
        instance.setIdentities(identities);

        return instance;
    }

    public Builder getNode(List<String> path) {
        return moduleNodes.get(path);
    }

    public Map<List<String>, TypeAwareBuilder> getDirtyNodes() {
        return dirtyNodes;
    }

    public Set<AugmentationSchemaBuilder> getAddedAugments() {
        return addedAugments;
    }

    public Set<IdentitySchemaNodeBuilder> getAddedIdentities() {
        return addedIdentities;
    }

    public String getName() {
        return name;
    }

    public String getPrefix() {
        return prefix;
    }

    public Date getRevision() {
        return revision;
    }

    public void addDirtyNode(List<String> path) {
        List<String> dirtyNodePath = new ArrayList<String>(path);
        TypeAwareBuilder nodeBuilder = (TypeAwareBuilder) moduleNodes
                .get(dirtyNodePath);
        dirtyNodes.put(dirtyNodePath, nodeBuilder);
    }

    public void setNamespace(URI namespace) {
        instance.setNamespace(namespace);
    }

    public void setRevision(Date revision) {
        this.revision = revision;
        instance.setRevision(revision);
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
        instance.setPrefix(prefix);
    }

    public void setYangVersion(String yangVersion) {
        instance.setYangVersion(yangVersion);
    }

    public void setDescription(String description) {
        instance.setDescription(description);
    }

    public void setReference(String reference) {
        instance.setReference(reference);
    }

    public void setOrganization(String organization) {
        instance.setOrganization(organization);
    }

    public void setContact(String contact) {
        instance.setContact(contact);
    }

    public void setAugmentations(Set<AugmentationSchema> augmentations) {
        this.augmentations = augmentations;
    }

    public boolean addModuleImport(final String moduleName,
            final Date revision, final String prefix) {
        ModuleImport moduleImport = createModuleImport(moduleName, revision,
                prefix);
        return imports.add(moduleImport);
    }

    public Set<ModuleImport> getModuleImports() {
        return imports;
    }

    public ExtensionBuilder addExtension(QName qname) {
        ExtensionBuilder builder = new ExtensionBuilder(qname);
        return builder;
    }

    public ContainerSchemaNodeBuilder addContainerNode(QName containerName,
            List<String> parentPath) {
        List<String> pathToNode = new ArrayList<String>(parentPath);

        ContainerSchemaNodeBuilder containerBuilder = new ContainerSchemaNodeBuilder(
                containerName);

        ChildNodeBuilder parent = (ChildNodeBuilder) moduleNodes
                .get(pathToNode);
        if (parent != null) {
            if(parent instanceof AugmentationSchemaBuilder) {
                containerBuilder.setAugmenting(true);
            }
            parent.addChildNode(containerBuilder);
        }

        pathToNode.add(containerName.getLocalName());
        moduleNodes.put(pathToNode, containerBuilder);
        addedChilds.put(pathToNode, containerBuilder);

        return containerBuilder;
    }

    public ListSchemaNodeBuilder addListNode(QName listName,
            List<String> parentPath) {
        List<String> pathToNode = new ArrayList<String>(parentPath);

        ListSchemaNodeBuilder listBuilder = new ListSchemaNodeBuilder(listName);

        ChildNodeBuilder parent = (ChildNodeBuilder) moduleNodes
                .get(pathToNode);
        if (parent != null) {
            if(parent instanceof AugmentationSchemaBuilder) {
                listBuilder.setAugmenting(true);
            }
            parent.addChildNode(listBuilder);
        }

        pathToNode.add(listName.getLocalName());
        moduleNodes.put(pathToNode, listBuilder);
        addedChilds.put(pathToNode, listBuilder);

        return listBuilder;
    }

    public LeafSchemaNodeBuilder addLeafNode(QName leafName,
            List<String> parentPath) {
        List<String> pathToNode = new ArrayList<String>(parentPath);

        LeafSchemaNodeBuilder leafBuilder = new LeafSchemaNodeBuilder(leafName);

        ChildNodeBuilder parent = (ChildNodeBuilder) moduleNodes.get(pathToNode);
        if (parent != null) {
            if(parent instanceof AugmentationSchemaBuilder) {
                leafBuilder.setAugmenting(true);
            }
            parent.addChildNode(leafBuilder);
        }

        pathToNode.add(leafName.getLocalName());
        addedChilds.put(pathToNode, leafBuilder);
        moduleNodes.put(pathToNode, leafBuilder);

        return leafBuilder;
    }

    public LeafListSchemaNodeBuilder addLeafListNode(QName leafListName,
            List<String> parentPath) {
        List<String> pathToNode = new ArrayList<String>(parentPath);

        LeafListSchemaNodeBuilder leafListBuilder = new LeafListSchemaNodeBuilder(
                leafListName);
        ChildNodeBuilder parent = (ChildNodeBuilder) moduleNodes.get(pathToNode);
        if (parent != null) {
            if(parent instanceof AugmentationSchemaBuilder) {
                leafListBuilder.setAugmenting(true);
            }
            parent.addChildNode(leafListBuilder);
        }

        pathToNode.add(leafListName.getLocalName());
        addedChilds.put(pathToNode, leafListBuilder);
        moduleNodes.put(pathToNode, leafListBuilder);

        return leafListBuilder;
    }

    public GroupingBuilder addGrouping(QName qname, List<String> parentPath) {
        List<String> pathToGroup = new ArrayList<String>(parentPath);

        GroupingBuilder builder = new GroupingBuilderImpl(qname);
        ChildNodeBuilder parentNodeBuilder = (ChildNodeBuilder) moduleNodes.get(pathToGroup);
        if (parentNodeBuilder != null) {
            parentNodeBuilder.addGrouping(builder);
        }

        pathToGroup.add(qname.getLocalName());
        moduleNodes.put(pathToGroup, builder);
        addedGroupings.put(pathToGroup, builder);

        return builder;
    }

    public AugmentationSchemaBuilder addAugment(String name,
            List<String> parentPath) {
        List<String> pathToAugment = new ArrayList<String>(parentPath);

        AugmentationSchemaBuilder builder = new AugmentationSchemaBuilderImpl(name);

        // augment can only be in 'module' or 'uses' statement
        UsesNodeBuilder parent = addedUsesNodes.get(pathToAugment);
        if (parent != null) {
            parent.addAugment(builder);
        }

        pathToAugment.add(name);
        moduleNodes.put(pathToAugment, builder);
        addedAugments.add(builder);

        return builder;
    }

    public UsesNodeBuilder addUsesNode(String groupingPathStr,
            List<String> parentPath) {
        List<String> pathToUses = new ArrayList<String>(parentPath);

        UsesNodeBuilder usesBuilder = new UsesNodeBuilderImpl(groupingPathStr);

        ChildNodeBuilder parent = (ChildNodeBuilder) moduleNodes.get(pathToUses);
        if (parent != null) {
            if(parent instanceof AugmentationSchemaBuilder) {
                usesBuilder.setAugmenting(true);
            }
            parent.addUsesNode(usesBuilder);
        }

        pathToUses.add(groupingPathStr);
        addedUsesNodes.put(pathToUses, usesBuilder);

        return usesBuilder;
    }

    public RpcDefinitionBuilder addRpc(QName qname, List<String> parentPath) {
        List<String> pathToRpc = new ArrayList<String>(parentPath);

        RpcDefinitionBuilder rpcBuilder = new RpcDefinitionBuilder(qname);

        pathToRpc.add(qname.getLocalName());
        addedRpcs.put(pathToRpc, rpcBuilder);

        QName inputQName = new QName(qname.getNamespace(), qname.getRevision(),
                qname.getPrefix(), "input");
        ContainerSchemaNodeBuilder inputBuilder = new ContainerSchemaNodeBuilder(inputQName);
        List<String> pathToInput = new ArrayList<String>(pathToRpc);
        pathToInput.add("input");
        moduleNodes.put(pathToInput, inputBuilder);
        rpcBuilder.setInput(inputBuilder);

        QName outputQName = new QName(qname.getNamespace(),
                qname.getRevision(), qname.getPrefix(), "output");
        ContainerSchemaNodeBuilder outputBuilder = new ContainerSchemaNodeBuilder(outputQName);
        List<String> pathToOutput = new ArrayList<String>(pathToRpc);
        pathToOutput.add("output");
        moduleNodes.put(pathToOutput, outputBuilder);
        rpcBuilder.setOutput(outputBuilder);

        return rpcBuilder;
    }

    public NotificationBuilder addNotification(QName notificationName,
            List<String> parentPath) {
        List<String> pathToNotification = new ArrayList<String>(parentPath);

        NotificationBuilder notificationBuilder = new NotificationBuilder(
                notificationName);

        pathToNotification.add(notificationName.getLocalName());
        moduleNodes.put(pathToNotification, notificationBuilder);
        addedNotifications.add(notificationBuilder);

        return notificationBuilder;
    }

    public FeatureBuilder addFeature(QName featureName, List<String> parentPath) {
        List<String> pathToFeature = new ArrayList<String>(parentPath);
        pathToFeature.add(featureName.getLocalName());

        FeatureBuilder builder = new FeatureBuilder(featureName);
        addedFeatures.put(pathToFeature, builder);
        return builder;
    }

    public TypedefBuilder addTypedef(QName typeDefName, List<String> parentPath) {
        List<String> pathToType = new ArrayList<String>(parentPath);
        TypedefBuilder builder = new TypedefBuilder(typeDefName);
        TypeDefinitionAwareBuilder parent = (TypeDefinitionAwareBuilder) moduleNodes.get(pathToType);
        if (parent != null) {
            parent.addTypedef(builder);
        }
        pathToType.add(typeDefName.getLocalName());
        addedTypedefs.put(pathToType, builder);
        moduleNodes.put(pathToType, builder);
        return builder;
    }

    public Set<TypeDefinitionBuilder> getModuleTypedefs() {
        Set<TypeDefinitionBuilder> typedefs = new HashSet<TypeDefinitionBuilder>();
        for (Map.Entry<List<String>, TypeDefinitionBuilder> entry : addedTypedefs.entrySet()) {
            if (entry.getKey().size() == 2) {
                typedefs.add(entry.getValue());
            }
        }
        return typedefs;
    }

    public void setType(TypeDefinition<?> type, List<String> parentPath) {
        TypeAwareBuilder parent = (TypeAwareBuilder) moduleNodes.get(parentPath);
        parent.setType(type);
    }

    public void addUnionType(List<String> parentPath) {
        TypeAwareBuilder parent = (TypeAwareBuilder) moduleNodes.get(parentPath);
        UnionTypeBuilder union = new UnionTypeBuilder();
        parent.setType(union.build());

        List<String> path = new ArrayList<String>(parentPath);
        path.add("union");

        unionTypes.put(path, union);
        moduleNodes.put(path, union);
    }

    public DeviationBuilder addDeviation(String targetPath) {
        DeviationBuilder builder = new DeviationBuilder(targetPath);
        addedDeviations.put(targetPath, builder);
        return builder;
    }

    public IdentitySchemaNodeBuilder addIdentity(QName qname) {
        IdentitySchemaNodeBuilder builder = new IdentitySchemaNodeBuilder(qname);
        addedIdentities.add(builder);

        return builder;
    }

    public void addConfiguration(boolean configuration, List<String> parentPath) {
        Builder builder = moduleNodes.get(parentPath);
        if (builder instanceof DeviationBuilder) {
            // skip
            // TODO
        } else {
            DataSchemaNodeBuilder configBuilder = (DataSchemaNodeBuilder) moduleNodes.get(parentPath);
            configBuilder.setConfiguration(configuration);
        }
    }

    public UnknownSchemaNodeBuilder addUnknownSchemaNode(QName qname, List<String> parentPath) {
        UnknownSchemaNodeBuilder builder = new UnknownSchemaNodeBuilder(qname);
        return builder;
    }


    private class ModuleImpl implements Module {
        private URI namespace;
        private final String name;
        private Date revision;
        private String prefix;
        private String yangVersion;
        private String description;
        private String reference;
        private String organization;
        private String contact;
        private Set<ModuleImport> imports = Collections.emptySet();
        private Set<FeatureDefinition> features = Collections.emptySet();
        private Set<TypeDefinition<?>> typeDefinitions = Collections.emptySet();
        private Set<NotificationDefinition> notifications = Collections.emptySet();
        private Set<AugmentationSchema> augmentations = Collections.emptySet();
        private Set<RpcDefinition> rpcs = Collections.emptySet();
        private Set<Deviation> deviations = Collections.emptySet();
        private Map<QName, DataSchemaNode> childNodes = Collections.emptyMap();
        private Set<GroupingDefinition> groupings = Collections.emptySet();
        private Set<UsesNode> uses = Collections.emptySet();
        private List<ExtensionDefinition> extensionSchemaNodes = Collections.emptyList();
        private Set<IdentitySchemaNode> identities = Collections.emptySet();

        private ModuleImpl(String name) {
            this.name = name;
        }

        @Override
        public URI getNamespace() {
            return namespace;
        }

        private void setNamespace(URI namespace) {
            this.namespace = namespace;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public Date getRevision() {
            return revision;
        }

        private void setRevision(Date revision) {
            this.revision = revision;
        }

        @Override
        public String getPrefix() {
            return prefix;
        }

        private void setPrefix(String prefix) {
            this.prefix = prefix;
        }

        @Override
        public String getYangVersion() {
            return yangVersion;
        }

        private void setYangVersion(String yangVersion) {
            this.yangVersion = yangVersion;
        }

        @Override
        public String getDescription() {
            return description;
        }

        private void setDescription(String description) {
            this.description = description;
        }

        @Override
        public String getReference() {
            return reference;
        }

        private void setReference(String reference) {
            this.reference = reference;
        }

        @Override
        public String getOrganization() {
            return organization;
        }

        private void setOrganization(String organization) {
            this.organization = organization;
        }

        @Override
        public String getContact() {
            return contact;
        }

        private void setContact(String contact) {
            this.contact = contact;
        }

        @Override
        public Set<ModuleImport> getImports() {
            return imports;
        }

        private void setImports(Set<ModuleImport> imports) {
            if(imports != null) {
                this.imports = imports;
            }
        }

        @Override
        public Set<FeatureDefinition> getFeatures() {
            return features;
        }

        private void setFeatures(Set<FeatureDefinition> features) {
            if(features != null) {
                this.features = features;
            }
        }

        @Override
        public Set<TypeDefinition<?>> getTypeDefinitions() {
            return typeDefinitions;
        }

        private void setTypeDefinitions(Set<TypeDefinition<?>> typeDefinitions) {
            if(typeDefinitions != null) {
                this.typeDefinitions = typeDefinitions;
            }
        }

        @Override
        public Set<NotificationDefinition> getNotifications() {
            return notifications;
        }

        private void setNotifications(Set<NotificationDefinition> notifications) {
            if(notifications != null) {
                this.notifications = notifications;
            }
        }

        @Override
        public Set<AugmentationSchema> getAugmentations() {
            return augmentations;
        }

        private void setAugmentations(Set<AugmentationSchema> augmentations) {
            if(augmentations != null) {
                this.augmentations = augmentations;
            }
        }

        @Override
        public Set<RpcDefinition> getRpcs() {
            return rpcs;
        }

        private void setRpcs(Set<RpcDefinition> rpcs) {
            if(rpcs != null) {
                this.rpcs = rpcs;
            }
        }

        @Override
        public Set<Deviation> getDeviations() {
            return deviations;
        }

        private void setDeviations(Set<Deviation> deviations) {
            if(deviations != null) {
                this.deviations = deviations;
            }
        }

        @Override
        public Set<DataSchemaNode> getChildNodes() {
            return new HashSet<DataSchemaNode>(childNodes.values());
        }

        private void setChildNodes(Map<QName, DataSchemaNode> childNodes) {
            if(childNodes != null) {
                this.childNodes = childNodes;
            }
        }

        @Override
        public Set<GroupingDefinition> getGroupings() {
            return groupings;
        }

        private void setGroupings(Set<GroupingDefinition> groupings) {
            if(groupings != null) {
                this.groupings = groupings;
            }
        }

        @Override
        public Set<UsesNode> getUses() {
            return uses;
        }

        private void setUses(Set<UsesNode> uses) {
            if(uses != null) {
                this.uses = uses;
            }
        }

        @Override
        public List<ExtensionDefinition> getExtensionSchemaNodes() {
            return extensionSchemaNodes;
        }

        private void setExtensionSchemaNodes(List<ExtensionDefinition> extensionSchemaNodes) {
            if(extensionSchemaNodes != null) {
                this.extensionSchemaNodes = extensionSchemaNodes;
            }
        }

        @Override
        public Set<IdentitySchemaNode> getIdentities() {
            return identities;
        }

        private void setIdentities(Set<IdentitySchemaNode> identities) {
            if(identities != null) {
                this.identities = identities;
            }
        }

        @Override
        public DataSchemaNode getDataChildByName(QName name) {
            return childNodes.get(name);
        }

        @Override
        public DataSchemaNode getDataChildByName(String name) {
            DataSchemaNode result = null;
            for (Map.Entry<QName, DataSchemaNode> entry : childNodes.entrySet()) {
                if (entry.getKey().getLocalName().equals(name)) {
                    result = entry.getValue();
                    break;
                }
            }
            return result;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((namespace == null) ? 0 : namespace.hashCode());
            result = prime * result + ((name == null) ? 0 : name.hashCode());
            result = prime * result + ((revision == null) ? 0 : revision.hashCode());
            result = prime * result + ((prefix == null) ? 0 : prefix.hashCode());
            result = prime * result + ((yangVersion == null) ? 0 : yangVersion.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            ModuleImpl other = (ModuleImpl) obj;
            if (namespace == null) {
                if (other.namespace != null) {
                    return false;
                }
            } else if (!namespace.equals(other.namespace)) {
                return false;
            }
            if (name == null) {
                if (other.name != null) {
                    return false;
                }
            } else if (!name.equals(other.name)) {
                return false;
            }
            if (revision == null) {
                if (other.revision != null) {
                    return false;
                }
            } else if (!revision.equals(other.revision)) {
                return false;
            }
            if (prefix == null) {
                if (other.prefix != null) {
                    return false;
                }
            } else if (!prefix.equals(other.prefix)) {
                return false;
            }
            if (yangVersion == null) {
                if (other.yangVersion != null) {
                    return false;
                }
            } else if (!yangVersion.equals(other.yangVersion)) {
                return false;
            }
            return true;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder(
                    ModuleImpl.class.getSimpleName());
            sb.append("[\n");
            sb.append("name=" + name + ",\n");
            sb.append("namespace=" + namespace + ",\n");
            sb.append("revision=" + revision + ",\n");
            sb.append("prefix=" + prefix + ",\n");
            sb.append("yangVersion=" + yangVersion + ",\n");
            sb.append("description=" + description + ",\n");
            sb.append("reference=" + reference + ",\n");
            sb.append("organization=" + organization + ",\n");
            sb.append("contact=" + contact + ",\n");
            sb.append("childNodes=" + childNodes.values() + ",\n");
            sb.append("groupings=" + groupings + ",\n");
            sb.append("imports=" + imports + ",\n");
            sb.append("features=" + features + ",\n");
            sb.append("typeDefinitions=" + typeDefinitions + ",\n");
            sb.append("notifications=" + notifications + ",\n");
            sb.append("augmentations=" + augmentations + ",\n");
            sb.append("rpcs=" + rpcs + ",\n");
            sb.append("deviations=" + deviations + "\n");
            sb.append("uses=" + uses + "\n");
            sb.append("]");
            return sb.toString();
        }
    }

    private ModuleImport createModuleImport(final String moduleName,
            final Date revision, final String prefix) {
        ModuleImport moduleImport = new ModuleImport() {
            @Override
            public String getModuleName() {
                return moduleName;
            }

            @Override
            public Date getRevision() {
                return revision;
            }

            @Override
            public String getPrefix() {
                return prefix;
            }

            @Override
            public int hashCode() {
                final int prime = 31;
                int result = 1;
                result = prime * result
                        + ((moduleName == null) ? 0 : moduleName.hashCode());
                result = prime * result
                        + ((revision == null) ? 0 : revision.hashCode());
                result = prime * result
                        + ((prefix == null) ? 0 : prefix.hashCode());
                return result;
            }

            @Override
            public boolean equals(Object obj) {
                if (this == obj) {
                    return true;
                }
                if (obj == null) {
                    return false;
                }
                if (getClass() != obj.getClass()) {
                    return false;
                }
                ModuleImport other = (ModuleImport) obj;
                if (getModuleName() == null) {
                    if (other.getModuleName() != null) {
                        return false;
                    }
                } else if (!getModuleName().equals(other.getModuleName())) {
                    return false;
                }
                if (getRevision() == null) {
                    if (other.getRevision() != null) {
                        return false;
                    }
                } else if (!getRevision().equals(other.getRevision())) {
                    return false;
                }
                if (getPrefix() == null) {
                    if (other.getPrefix() != null) {
                        return false;
                    }
                } else if (!getPrefix().equals(other.getPrefix())) {
                    return false;
                }
                return true;
            }

            @Override
            public String toString() {
                return "ModuleImport[moduleName=" + moduleName + ", revision="
                        + revision + ", prefix=" + prefix + "]";
            }
        };
        return moduleImport;
    }

    /**
     * Traverse through given addedChilds and add only direct module childs.
     * Direct module child path size is 2 (1. module name, 2. child name).
     *
     * @param addedChilds
     * @return map of children, where key is child QName and value is child
     *         itself
     */
    private Map<QName, DataSchemaNode> buildModuleChildNodes(
            Map<List<String>, DataSchemaNodeBuilder> addedChilds) {
        final Map<QName, DataSchemaNode> childNodes = new HashMap<QName, DataSchemaNode>();
        for (Map.Entry<List<String>, DataSchemaNodeBuilder> entry : addedChilds
                .entrySet()) {
            if (entry.getKey().size() == 2) {
                DataSchemaNode node = entry.getValue().build();
                QName qname = entry.getValue().getQName();
                childNodes.put(qname, node);
            }
        }
        return childNodes;
    }

    /**
     * Traverse through given addedGroupings and add only direct module
     * groupings. Direct module grouping path size is 2 (1. module name, 2.
     * grouping name).
     *
     * @param addedGroupings
     * @return set of built GroupingDefinition objects
     */
    private Set<GroupingDefinition> buildModuleGroupings(
            Map<List<String>, GroupingBuilder> addedGroupings) {
        final Set<GroupingDefinition> groupings = new HashSet<GroupingDefinition>();
        for (Map.Entry<List<String>, GroupingBuilder> entry : addedGroupings
                .entrySet()) {
            if (entry.getKey().size() == 2) {
                groupings.add(entry.getValue().build());
            }
        }
        return groupings;
    }

    /**
     * Traverse through given addedRpcs and build RpcDefinition objects.
     *
     * @param addedRpcs
     * @return set of built RpcDefinition objects
     */
    private Set<RpcDefinition> buildModuleRpcs(
            Map<List<String>, RpcDefinitionBuilder> addedRpcs) {
        final Set<RpcDefinition> rpcs = new HashSet<RpcDefinition>();
        RpcDefinitionBuilder builder;
        for (Map.Entry<List<String>, RpcDefinitionBuilder> entry : addedRpcs
                .entrySet()) {
            builder = entry.getValue();
            RpcDefinition rpc = builder.build();
            rpcs.add(rpc);
        }
        return rpcs;
    }

    /**
     * Traverse through given addedTypedefs and add only direct module typedef
     * statements. Direct module typedef path size is 2 (1. module name, 2.
     * typedef name).
     *
     * @param addedTypedefs
     * @return set of built module typedef statements
     */
    private Set<TypeDefinition<?>> buildModuleTypedefs(
            Map<List<String>, TypeDefinitionBuilder> addedTypedefs) {
        Set<TypeDefinition<?>> typedefs = new HashSet<TypeDefinition<?>>();
        for (Map.Entry<List<String>, TypeDefinitionBuilder> entry : addedTypedefs
                .entrySet()) {
            if (entry.getKey().size() == 2) {
                TypeDefinition<? extends TypeDefinition<?>> node = entry
                        .getValue().build();
                typedefs.add(node);
            }
        }
        return typedefs;
    }

    /**
     * Traverse through given addedUsesNodes and add only direct module uses
     * nodes. Direct module uses node path size is 2 (1. module name, 2. uses
     * name).
     *
     * @param addedUsesNodes
     * @return set of built module uses nodes
     */
    private Set<UsesNode> buildUsesNodes(
            Map<List<String>, UsesNodeBuilder> addedUsesNodes) {
        final Set<UsesNode> usesNodeDefinitions = new HashSet<UsesNode>();
        for (Map.Entry<List<String>, UsesNodeBuilder> entry : addedUsesNodes
                .entrySet()) {
            if (entry.getKey().size() == 2) {
                usesNodeDefinitions.add(entry.getValue().build());
            }
        }
        return usesNodeDefinitions;
    }

    /**
     * Traverse through given addedFeatures and add only direct module features.
     * Direct module feature path size is 2 (1. module name, 2. feature name).
     *
     * @param addedFeatures
     * @return set of built module features
     */
    private Set<FeatureDefinition> buildModuleFeatures(
            Map<List<String>, FeatureBuilder> addedFeatures) {
        Set<FeatureDefinition> features = new HashSet<FeatureDefinition>();
        for (Map.Entry<List<String>, FeatureBuilder> entry : addedFeatures
                .entrySet()) {
            if (entry.getKey().size() == 2) {
                features.add(entry.getValue().build());
            }
        }
        return features;
    }

}
