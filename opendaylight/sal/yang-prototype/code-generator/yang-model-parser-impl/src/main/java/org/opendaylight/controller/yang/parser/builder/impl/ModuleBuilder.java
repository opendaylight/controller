/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.yang.parser.builder.impl;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
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
import org.opendaylight.controller.yang.model.api.SchemaPath;
import org.opendaylight.controller.yang.model.api.TypeDefinition;
import org.opendaylight.controller.yang.model.api.UsesNode;
import org.opendaylight.controller.yang.parser.builder.api.AugmentationSchemaBuilder;
import org.opendaylight.controller.yang.parser.builder.api.Builder;
import org.opendaylight.controller.yang.parser.builder.api.DataNodeContainerBuilder;
import org.opendaylight.controller.yang.parser.builder.api.DataSchemaNodeBuilder;
import org.opendaylight.controller.yang.parser.builder.api.GroupingBuilder;
import org.opendaylight.controller.yang.parser.builder.api.SchemaNodeBuilder;
import org.opendaylight.controller.yang.parser.builder.api.TypeAwareBuilder;
import org.opendaylight.controller.yang.parser.builder.api.TypeDefinitionAwareBuilder;
import org.opendaylight.controller.yang.parser.builder.api.TypeDefinitionBuilder;
import org.opendaylight.controller.yang.parser.builder.api.UsesNodeBuilder;
import org.opendaylight.controller.yang.parser.util.RefineHolder;
import org.opendaylight.controller.yang.parser.util.YangParseException;

/**
 * This builder builds Module object. If this module is dependent on external
 * module/modules, these dependencies must be resolved before module is built,
 * otherwise result may not be valid.
 */
public class ModuleBuilder implements Builder {
    private final ModuleImpl instance;
    private final String name;
    private URI namespace;
    private String prefix;
    private Date revision;

    private int augmentsResolved;

    private final Set<ModuleImport> imports = new HashSet<ModuleImport>();

    /**
     * Holds all child (DataSchemaNode) nodes: anyxml, choice, case, container,
     * list, leaf, leaf-list.
     */
    private final Map<List<String>, DataSchemaNodeBuilder> childNodes = new HashMap<List<String>, DataSchemaNodeBuilder>();

    private final Map<List<String>, GroupingBuilder> addedGroupings = new HashMap<List<String>, GroupingBuilder>();
    private final List<AugmentationSchemaBuilder> addedAugments = new ArrayList<AugmentationSchemaBuilder>();
    private final Map<List<String>, UsesNodeBuilder> addedUsesNodes = new HashMap<List<String>, UsesNodeBuilder>();
    private final Map<List<String>, RpcDefinitionBuilder> addedRpcs = new HashMap<List<String>, RpcDefinitionBuilder>();
    private final Set<NotificationBuilder> addedNotifications = new HashSet<NotificationBuilder>();
    private final Set<IdentitySchemaNodeBuilder> addedIdentities = new HashSet<IdentitySchemaNodeBuilder>();
    private final Map<List<String>, FeatureBuilder> addedFeatures = new HashMap<List<String>, FeatureBuilder>();
    private final Map<List<String>, DeviationBuilder> addedDeviations = new HashMap<List<String>, DeviationBuilder>();
    private final Map<List<String>, TypeDefinitionBuilder> addedTypedefs = new HashMap<List<String>, TypeDefinitionBuilder>();
    private final Map<List<String>, UnionTypeBuilder> addedUnionTypes = new HashMap<List<String>, UnionTypeBuilder>();
    private final List<ExtensionBuilder> addedExtensions = new ArrayList<ExtensionBuilder>();
    private final Set<UnknownSchemaNodeBuilder> addedUnknownNodes = new HashSet<UnknownSchemaNodeBuilder>();

    private final Map<List<String>, TypeAwareBuilder> dirtyNodes = new HashMap<List<String>, TypeAwareBuilder>();

    private final LinkedList<Builder> actualPath = new LinkedList<Builder>();

    public ModuleBuilder(final String name) {
        this.name = name;
        instance = new ModuleImpl(name);
    }

    /**
     * Build new Module object based on this builder.
     */
    @Override
    public Module build() {
        instance.setPrefix(prefix);
        instance.setRevision(revision);
        instance.setImports(imports);
        instance.setNamespace(namespace);

        // TYPEDEFS
        final Set<TypeDefinition<?>> typedefs = buildModuleTypedefs(addedTypedefs);
        instance.setTypeDefinitions(typedefs);

        // CHILD NODES
        final Map<QName, DataSchemaNode> children = buildModuleChildNodes(childNodes);
        instance.setChildNodes(children);

        // GROUPINGS
        final Set<GroupingDefinition> groupings = buildModuleGroupings(addedGroupings);
        instance.setGroupings(groupings);

        // USES
        final Set<UsesNode> usesDefinitions = buildUsesNodes(addedUsesNodes);
        instance.setUses(usesDefinitions);

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
        final Set<AugmentationSchema> augmentations = new HashSet<AugmentationSchema>();
        for (AugmentationSchemaBuilder builder : addedAugments) {
            augmentations.add(builder.build());
        }
        instance.setAugmentations(augmentations);

        // RPCs
        final Set<RpcDefinition> rpcs = buildModuleRpcs(addedRpcs);
        instance.setRpcs(rpcs);

        // DEVIATIONS
        final Set<Deviation> deviations = new HashSet<Deviation>();
        for (Map.Entry<List<String>, DeviationBuilder> entry : addedDeviations
                .entrySet()) {
            deviations.add(entry.getValue().build());
        }
        instance.setDeviations(deviations);

        // EXTENSIONS
        final List<ExtensionDefinition> extensions = new ArrayList<ExtensionDefinition>();
        for (ExtensionBuilder b : addedExtensions) {
            extensions.add(b.build());
        }
        instance.setExtensionSchemaNodes(extensions);

        // IDENTITIES
        final Set<IdentitySchemaNode> identities = new HashSet<IdentitySchemaNode>();
        for (IdentitySchemaNodeBuilder idBuilder : addedIdentities) {
            identities.add(idBuilder.build());
        }
        instance.setIdentities(identities);

        return instance;
    }

    @Override
    public int getLine() {
        return 0;
    }

    public void enterNode(final Builder node) {
        actualPath.push(node);
    }

    public void exitNode() {
        actualPath.pop();
    }

    public Builder getActualNode() {
        if (actualPath.isEmpty()) {
            return null;
        } else {
            return actualPath.get(0);
        }
    }

    public Builder getModuleNode(final List<String> path) {
        return childNodes.get(path);
    }

    public GroupingBuilder getGrouping(final List<String> path) {
        return addedGroupings.get(path);
    }

    public Builder getModuleTypedef(final List<String> path) {
        return addedTypedefs.get(path);
    }

    public Set<DataSchemaNodeBuilder> getChildNodes() {
        final Set<DataSchemaNodeBuilder> children = new HashSet<DataSchemaNodeBuilder>();
        for (Map.Entry<List<String>, DataSchemaNodeBuilder> entry : childNodes
                .entrySet()) {
            final List<String> path = entry.getKey();
            final DataSchemaNodeBuilder child = entry.getValue();
            if (path.size() == 2) {
                children.add(child);
            }
        }
        return children;
    }

    public Map<List<String>, TypeAwareBuilder> getDirtyNodes() {
        return dirtyNodes;
    }

    public List<AugmentationSchemaBuilder> getAugments() {
        return addedAugments;
    }

    public Set<IdentitySchemaNodeBuilder> getIdentities() {
        return addedIdentities;
    }

    public Map<List<String>, UsesNodeBuilder> getUsesNodes() {
        return addedUsesNodes;
    }

    public Set<UnknownSchemaNodeBuilder> getUnknownNodes() {
        return addedUnknownNodes;
    }

    public Set<TypeDefinitionBuilder> getModuleTypedefs() {
        final Set<TypeDefinitionBuilder> typedefs = new HashSet<TypeDefinitionBuilder>();
        for (Map.Entry<List<String>, TypeDefinitionBuilder> entry : addedTypedefs
                .entrySet()) {
            if (entry.getKey().size() == 2) {
                typedefs.add(entry.getValue());
            }
        }
        return typedefs;
    }

    public String getName() {
        return name;
    }

    public URI getNamespace() {
        return namespace;
    }

    public void setNamespace(final URI namespace) {
        this.namespace = namespace;
    }

    public String getPrefix() {
        return prefix;
    }

    public Date getRevision() {
        return revision;
    }

    public int getAugmentsResolved() {
        return augmentsResolved;
    }

    public void augmentResolved() {
        augmentsResolved++;
    }

    public void addDirtyNode(final List<String> path) {
        final List<String> dirtyNodePath = new ArrayList<String>(path);
        final TypeAwareBuilder nodeBuilder = (TypeAwareBuilder) actualPath
                .getFirst();
        dirtyNodes.put(dirtyNodePath, nodeBuilder);
    }

    public void setRevision(final Date revision) {
        this.revision = revision;
    }

    public void setPrefix(final String prefix) {
        this.prefix = prefix;
    }

    public void setYangVersion(final String yangVersion) {
        instance.setYangVersion(yangVersion);
    }

    public void setDescription(final String description) {
        instance.setDescription(description);
    }

    public void setReference(final String reference) {
        instance.setReference(reference);
    }

    public void setOrganization(final String organization) {
        instance.setOrganization(organization);
    }

    public void setContact(final String contact) {
        instance.setContact(contact);
    }

    public boolean addModuleImport(final String moduleName,
            final Date revision, final String prefix) {
        final ModuleImport moduleImport = createModuleImport(moduleName,
                revision, prefix);
        return imports.add(moduleImport);
    }

    public Set<ModuleImport> getModuleImports() {
        return imports;
    }

    public ExtensionBuilder addExtension(final QName qname, final int line) {
        final ExtensionBuilder builder = new ExtensionBuilder(qname, line);
        addedExtensions.add(builder);
        return builder;
    }

    public ContainerSchemaNodeBuilder addContainerNode(
            final QName containerName, final List<String> parentPath,
            final int line) {
        final List<String> pathToNode = new ArrayList<String>(parentPath);
        final ContainerSchemaNodeBuilder containerBuilder = new ContainerSchemaNodeBuilder(
                containerName, line);
        updateParent(containerBuilder, line, "container");

        pathToNode.add(containerName.getLocalName());
        childNodes.put(pathToNode, containerBuilder);

        return containerBuilder;
    }

    public ListSchemaNodeBuilder addListNode(final QName listName,
            final List<String> parentPath, final int line) {
        final List<String> pathToNode = new ArrayList<String>(parentPath);
        final ListSchemaNodeBuilder listBuilder = new ListSchemaNodeBuilder(
                listName, line);
        updateParent(listBuilder, line, "list");

        pathToNode.add(listName.getLocalName());
        childNodes.put(pathToNode, listBuilder);

        return listBuilder;
    }

    public LeafSchemaNodeBuilder addLeafNode(final QName leafName,
            final List<String> parentPath, final int line) {
        final List<String> pathToNode = new ArrayList<String>(parentPath);
        final LeafSchemaNodeBuilder leafBuilder = new LeafSchemaNodeBuilder(
                leafName, line);
        updateParent(leafBuilder, line, "leaf");

        pathToNode.add(leafName.getLocalName());
        childNodes.put(pathToNode, leafBuilder);

        return leafBuilder;
    }

    public LeafListSchemaNodeBuilder addLeafListNode(final QName qname,
            final List<String> parentPath, final int line) {
        final List<String> pathToNode = new ArrayList<String>(parentPath);
        final LeafListSchemaNodeBuilder leafListBuilder = new LeafListSchemaNodeBuilder(
                qname, line);
        updateParent(leafListBuilder, line, "leaf-list");

        pathToNode.add(qname.getLocalName());
        childNodes.put(pathToNode, leafListBuilder);

        return leafListBuilder;
    }

    public GroupingBuilder addGrouping(final QName qname,
            final List<String> parentPath, final int line) {
        final List<String> pathToGroup = new ArrayList<String>(parentPath);
        final GroupingBuilder builder = new GroupingBuilderImpl(qname, line);

        if (!(actualPath.isEmpty())) {
            final Builder parent = actualPath.getFirst();
            if (parent instanceof DataNodeContainerBuilder) {
                ((DataNodeContainerBuilder) parent).addGrouping(builder);
            } else {
                throw new YangParseException(name, line,
                        "Unresolved parent of grouping " + qname.getLocalName());
            }
        }

        pathToGroup.add(qname.getLocalName());
        addedGroupings.put(pathToGroup, builder);

        return builder;
    }

    public AugmentationSchemaBuilder addAugment(final String name,
            final List<String> parentPath, final int line) {
        final List<String> pathToAugment = new ArrayList<String>(parentPath);
        final AugmentationSchemaBuilder builder = new AugmentationSchemaBuilderImpl(
                name, line);

        // augment can only be in 'module' or 'uses' statement
        if (!(actualPath.isEmpty())) {
            final Builder parent = actualPath.getFirst();
            if (parent instanceof UsesNodeBuilder) {
                ((UsesNodeBuilder) parent).addAugment(builder);
            } else {
                throw new YangParseException(this.name, line,
                        "Unresolved parent of augment " + name);
            }
        }

        pathToAugment.add(name);
        addedAugments.add(builder);

        return builder;
    }

    public UsesNodeBuilder addUsesNode(final String groupingPathStr,
            final List<String> parentPath, final int line) {
        final List<String> pathToUses = new ArrayList<String>(parentPath);
        final UsesNodeBuilder usesBuilder = new UsesNodeBuilderImpl(
                groupingPathStr, line);

        if (!(actualPath.isEmpty())) {
            final Builder parent = actualPath.getFirst();
            if (parent instanceof DataNodeContainerBuilder) {
                if (parent instanceof AugmentationSchemaBuilder) {
                    usesBuilder.setAugmenting(true);
                }
                ((DataNodeContainerBuilder) parent).addUsesNode(usesBuilder);
            } else {
                throw new YangParseException(name, line,
                        "Unresolved parent of uses " + groupingPathStr);
            }
        }

        pathToUses.add(groupingPathStr);
        addedUsesNodes.put(pathToUses, usesBuilder);
        return usesBuilder;
    }

    public void addRefine(final RefineHolder refine,
            final List<String> parentPath) {
        final List<String> path = new ArrayList<String>(parentPath);

        if (actualPath.isEmpty()) {
            throw new YangParseException(name, refine.getLine(),
                    "refine can be defined only in uses statement");
        } else {
            final Builder parent = actualPath.getFirst();
            if (parent instanceof UsesNodeBuilder) {
                ((UsesNodeBuilder) parent).addRefine(refine);
            } else {
                throw new YangParseException(name, refine.getLine(),
                        "refine can be defined only in uses statement");
            }
        }

        path.add(refine.getName());
    }

    public RpcDefinitionBuilder addRpc(final QName qname,
            final List<String> parentPath, final int line) {

        if (!(actualPath.isEmpty())) {
            throw new YangParseException(name, line,
                    "rpc can be defined only in module or submodule");
        }

        final List<String> pathToRpc = new ArrayList<String>(parentPath);
        final RpcDefinitionBuilder rpcBuilder = new RpcDefinitionBuilder(qname,
                line);

        pathToRpc.add(qname.getLocalName());
        addedRpcs.put(pathToRpc, rpcBuilder);

        return rpcBuilder;
    }

    public ContainerSchemaNodeBuilder addRpcInput(final QName inputQName,
            final int line) {
        final Builder parent = actualPath.getFirst();
        if (!(parent instanceof RpcDefinitionBuilder)) {
            throw new YangParseException(name, line,
                    "input can be defined only in rpc statement");
        }
        final RpcDefinitionBuilder rpc = (RpcDefinitionBuilder) parent;

        final ContainerSchemaNodeBuilder inputBuilder = new ContainerSchemaNodeBuilder(
                inputQName, line);
        rpc.setInput(inputBuilder);
        return inputBuilder;
    }

    public ContainerSchemaNodeBuilder addRpcOutput(final QName outputQName,
            final int line) {
        final Builder parent = actualPath.getFirst();
        if (!(parent instanceof RpcDefinitionBuilder)) {
            throw new YangParseException(name, line,
                    "output can be defined only in rpc statement");
        }
        final RpcDefinitionBuilder rpc = (RpcDefinitionBuilder) parent;

        final ContainerSchemaNodeBuilder outputBuilder = new ContainerSchemaNodeBuilder(
                outputQName, line);
        rpc.setOutput(outputBuilder);
        return outputBuilder;
    }

    public NotificationBuilder addNotification(final QName notificationName,
            final List<String> parentPath, final int line) {
        if (!(actualPath.isEmpty())) {
            throw new YangParseException(name, line,
                    "notification can be defined only in module or submodule");
        }

        final NotificationBuilder builder = new NotificationBuilder(
                notificationName, line);

        final List<String> notificationPath = new ArrayList<String>(parentPath);
        notificationPath.add(notificationName.getLocalName());
        addedNotifications.add(builder);

        return builder;
    }

    public FeatureBuilder addFeature(final QName featureName,
            final List<String> parentPath, final int line) {
        if (!(actualPath.isEmpty())) {
            throw new YangParseException(name, line,
                    "feature can be defined only in module or submodule");
        }

        final List<String> pathToFeature = new ArrayList<String>(parentPath);
        pathToFeature.add(featureName.getLocalName());

        final FeatureBuilder builder = new FeatureBuilder(featureName, line);
        addedFeatures.put(pathToFeature, builder);
        return builder;
    }

    public ChoiceBuilder addChoice(final QName choiceName,
            final List<String> parentPath, final int line) {
        final List<String> pathToChoice = new ArrayList<String>(parentPath);
        final ChoiceBuilder builder = new ChoiceBuilder(choiceName, line);

        if (!(actualPath.isEmpty())) {
            Builder parent = actualPath.getFirst();
            if (parent instanceof DataNodeContainerBuilder) {
                if (parent instanceof AugmentationSchemaBuilder) {
                    builder.setAugmenting(true);
                }
                ((DataNodeContainerBuilder) parent).addChildNode(builder);
            } else {
                throw new YangParseException(name, line,
                        "Unresolved parent of choice "
                                + choiceName.getLocalName());
            }
        }

        pathToChoice.add(choiceName.getLocalName());
        childNodes.put(pathToChoice, builder);

        return builder;
    }

    public ChoiceCaseBuilder addCase(final QName caseName,
            final List<String> parentPath, final int line) {
        final List<String> pathToCase = new ArrayList<String>(parentPath);
        final ChoiceCaseBuilder builder = new ChoiceCaseBuilder(caseName, line);

        if (actualPath.isEmpty()) {
            throw new YangParseException(name, line, "'case' parent not found");
        } else {
            final Builder parent = actualPath.getFirst();
            if (parent instanceof ChoiceBuilder) {
                ((ChoiceBuilder) parent).addChildNode(builder);
            } else if (parent instanceof AugmentationSchemaBuilder) {
                builder.setAugmenting(true);
                ((AugmentationSchemaBuilder) parent).addChildNode(builder);
            } else {
                throw new YangParseException(name, line,
                        "Unresolved parent of 'case' "
                                + caseName.getLocalName());
            }
        }

        pathToCase.add(caseName.getLocalName());
        childNodes.put(pathToCase, builder);

        return builder;
    }

    public AnyXmlBuilder addAnyXml(final QName anyXmlName,
            final List<String> parentPath, final int line) {
        final List<String> pathToAnyXml = new ArrayList<String>(parentPath);
        final AnyXmlBuilder builder = new AnyXmlBuilder(anyXmlName, line);
        updateParent(builder, line, "anyxml");

        pathToAnyXml.add(anyXmlName.getLocalName());
        childNodes.put(pathToAnyXml, builder);

        return builder;
    }

    public TypeDefinitionBuilderImpl addTypedef(final QName typeDefName,
            final List<String> parentPath, final int line) {
        final List<String> pathToType = new ArrayList<String>(parentPath);
        final TypeDefinitionBuilderImpl builder = new TypeDefinitionBuilderImpl(
                typeDefName, line);

        if (!(actualPath.isEmpty())) {
            final Builder parent = actualPath.getFirst();
            if (parent instanceof TypeDefinitionAwareBuilder) {
                ((TypeDefinitionAwareBuilder) parent).addTypedef(builder);
            } else {
                throw new YangParseException(name, line,
                        "Unresolved parent of typedef "
                                + typeDefName.getLocalName());
            }
        }

        pathToType.add(typeDefName.getLocalName());
        addedTypedefs.put(pathToType, builder);
        return builder;
    }

    public void setType(final TypeDefinition<?> type,
            final List<String> parentPath) {

        if (!(actualPath.isEmpty())) {
            final Builder parent = actualPath.getFirst();
            if (parent instanceof TypeAwareBuilder) {
                ((TypeAwareBuilder) parent).setType(type);
            } else {
                throw new YangParseException("Failed to set type '"
                        + type.getQName().getLocalName()
                        + "'. Unknown parent node: " + parent);
            }
        }
    }

    public UnionTypeBuilder addUnionType(final List<String> currentPath,
            final URI namespace, final Date revision, final int line) {
        final List<String> pathToUnion = new ArrayList<String>(currentPath);
        final UnionTypeBuilder union = new UnionTypeBuilder(line);

        if (actualPath.isEmpty()) {
            throw new YangParseException(line, "union error");
        } else {
            final Builder parent = actualPath.getFirst();
            if (parent instanceof TypeAwareBuilder) {

                ((TypeAwareBuilder) parent).setTypedef(union);

                final List<String> path = new ArrayList<String>(pathToUnion);
                path.add("union");

                addedUnionTypes.put(path, union);
                return union;
            } else {
                throw new YangParseException(name, line,
                        "Unresolved parent of union type.");
            }
        }
    }

    public void addIdentityrefType(final String baseString,
            final List<String> parentPath, final SchemaPath schemaPath,
            final int line) {
        final List<String> pathToIdentityref = new ArrayList<String>(parentPath);
        final IdentityrefTypeBuilder identityref = new IdentityrefTypeBuilder(
                baseString, schemaPath, line);

        if (actualPath.isEmpty()) {
            throw new YangParseException(line, "identityref error");
        } else {
            final Builder parent = actualPath.getFirst();
            if (parent instanceof TypeAwareBuilder) {
                final TypeAwareBuilder typeParent = (TypeAwareBuilder) parent;
                typeParent.setTypedef(identityref);
                dirtyNodes.put(pathToIdentityref, typeParent);
            } else {
                throw new YangParseException(name, line,
                        "Unresolved parent of identityref type.");
            }
        }
    }

    public DeviationBuilder addDeviation(final String targetPath,
            final List<String> parentPath, final int line) {
        if (!(actualPath.isEmpty())) {
            throw new YangParseException(name, line,
                    "deviation can be defined only in module or submodule");
        }

        final List<String> pathToDeviation = new ArrayList<String>(parentPath);
        pathToDeviation.add(targetPath);
        final DeviationBuilder builder = new DeviationBuilder(targetPath, line);
        addedDeviations.put(pathToDeviation, builder);
        return builder;
    }

    public IdentitySchemaNodeBuilder addIdentity(final QName qname,
            final List<String> parentPath, final int line) {
        if (!(actualPath.isEmpty())) {
            throw new YangParseException(name, line,
                    "identity can be defined only in module or submodule");
        }

        final List<String> pathToIdentity = new ArrayList<String>(parentPath);
        final IdentitySchemaNodeBuilder builder = new IdentitySchemaNodeBuilder(
                qname, line);
        pathToIdentity.add(qname.getLocalName());
        addedIdentities.add(builder);
        return builder;
    }

    public void addConfiguration(final boolean configuration,
            final List<String> parentPath, final int line) {
        if (actualPath.isEmpty()) {
            throw new YangParseException(name, line,
                    "Parent node of config statement not found.");
        } else {
            final Builder parent = actualPath.getFirst();
            if (parent instanceof DataSchemaNodeBuilder) {
                ((DataSchemaNodeBuilder) parent)
                        .setConfiguration(configuration);
            } else if (parent instanceof RefineHolder) {
                ((RefineHolder) parent).setConfig(configuration);
            } else if (parent instanceof DeviationBuilder) {
                // skip: set config to deviation (deviate stmt) not supported by
                // current api
                return;
            } else {
                throw new YangParseException(name, line,
                        "Unresolved parent of config statement.");
            }
        }
    }

    public UnknownSchemaNodeBuilder addUnknownSchemaNode(final QName qname,
            final List<String> parentPath, final int line) {
        final UnknownSchemaNodeBuilder builder = new UnknownSchemaNodeBuilder(
                qname, line);

        if (!(actualPath.isEmpty())) {
            final Builder parent = actualPath.getFirst();
            if (parent instanceof SchemaNodeBuilder) {
                ((SchemaNodeBuilder) parent).addUnknownSchemaNode(builder);
            } else if (parent instanceof RefineHolder) {
                ((RefineHolder) parent).addUnknownSchemaNode(builder);
            } else {
                throw new YangParseException(name, line,
                        "Unresolved parent of unknown node '"
                                + qname.getLocalName() + "'");
            }
        }

        addedUnknownNodes.add(builder);
        return builder;
    }

    @Override
    public String toString() {
        return ModuleBuilder.class.getSimpleName() + "[" + name + "]";
    }

    private final class ModuleImpl implements Module {
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
        private Set<NotificationDefinition> notifications = Collections
                .emptySet();
        private Set<AugmentationSchema> augmentations = Collections.emptySet();
        private Set<RpcDefinition> rpcs = Collections.emptySet();
        private Set<Deviation> deviations = Collections.emptySet();
        private Map<QName, DataSchemaNode> childNodes = Collections.emptyMap();
        private Set<GroupingDefinition> groupings = Collections.emptySet();
        private Set<UsesNode> uses = Collections.emptySet();
        private List<ExtensionDefinition> extensionNodes = Collections
                .emptyList();
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
            if (imports != null) {
                this.imports = imports;
            }
        }

        @Override
        public Set<FeatureDefinition> getFeatures() {
            return features;
        }

        private void setFeatures(Set<FeatureDefinition> features) {
            if (features != null) {
                this.features = features;
            }
        }

        @Override
        public Set<TypeDefinition<?>> getTypeDefinitions() {
            return typeDefinitions;
        }

        private void setTypeDefinitions(Set<TypeDefinition<?>> typeDefinitions) {
            if (typeDefinitions != null) {
                this.typeDefinitions = typeDefinitions;
            }
        }

        @Override
        public Set<NotificationDefinition> getNotifications() {
            return notifications;
        }

        private void setNotifications(Set<NotificationDefinition> notifications) {
            if (notifications != null) {
                this.notifications = notifications;
            }
        }

        @Override
        public Set<AugmentationSchema> getAugmentations() {
            return augmentations;
        }

        private void setAugmentations(Set<AugmentationSchema> augmentations) {
            if (augmentations != null) {
                this.augmentations = augmentations;
            }
        }

        @Override
        public Set<RpcDefinition> getRpcs() {
            return rpcs;
        }

        private void setRpcs(Set<RpcDefinition> rpcs) {
            if (rpcs != null) {
                this.rpcs = rpcs;
            }
        }

        @Override
        public Set<Deviation> getDeviations() {
            return deviations;
        }

        private void setDeviations(Set<Deviation> deviations) {
            if (deviations != null) {
                this.deviations = deviations;
            }
        }

        @Override
        public Set<DataSchemaNode> getChildNodes() {
            return new HashSet<DataSchemaNode>(childNodes.values());
        }

        private void setChildNodes(Map<QName, DataSchemaNode> childNodes) {
            if (childNodes != null) {
                this.childNodes = childNodes;
            }
        }

        @Override
        public Set<GroupingDefinition> getGroupings() {
            return groupings;
        }

        private void setGroupings(Set<GroupingDefinition> groupings) {
            if (groupings != null) {
                this.groupings = groupings;
            }
        }

        @Override
        public Set<UsesNode> getUses() {
            return uses;
        }

        private void setUses(Set<UsesNode> uses) {
            if (uses != null) {
                this.uses = uses;
            }
        }

        @Override
        public List<ExtensionDefinition> getExtensionSchemaNodes() {
            return extensionNodes;
        }

        private void setExtensionSchemaNodes(
                List<ExtensionDefinition> extensionNodes) {
            if (extensionNodes != null) {
                this.extensionNodes = extensionNodes;
            }
        }

        @Override
        public Set<IdentitySchemaNode> getIdentities() {
            return identities;
        }

        private void setIdentities(Set<IdentitySchemaNode> identities) {
            if (identities != null) {
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
            result = prime * result
                    + ((namespace == null) ? 0 : namespace.hashCode());
            result = prime * result + ((name == null) ? 0 : name.hashCode());
            result = prime * result
                    + ((revision == null) ? 0 : revision.hashCode());
            result = prime * result
                    + ((prefix == null) ? 0 : prefix.hashCode());
            result = prime * result
                    + ((yangVersion == null) ? 0 : yangVersion.hashCode());
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
            sb.append("[");
            sb.append("name=" + name);
            sb.append(", namespace=" + namespace);
            sb.append(", revision=" + revision);
            sb.append(", prefix=" + prefix);
            sb.append(", yangVersion=" + yangVersion);
            sb.append("]");
            return sb.toString();
        }
    }

    private void updateParent(DataSchemaNodeBuilder nodeBuilder, int line,
            String nodeTypeName) {
        if (!(actualPath.isEmpty())) {
            final Builder parent = actualPath.getFirst();
            if (parent instanceof DataNodeContainerBuilder) {
                if (parent instanceof AugmentationSchemaBuilder) {
                    nodeBuilder.setAugmenting(true);
                }
                ((DataNodeContainerBuilder) parent).addChildNode(nodeBuilder);
            } else if (parent instanceof ChoiceBuilder) {
                ((ChoiceBuilder) parent).addChildNode(nodeBuilder);
            } else {
                throw new YangParseException(name, line,
                        "Unresolved parent of " + nodeTypeName + " "
                                + nodeBuilder.getQName().getLocalName());
            }
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
            List<String> path = entry.getKey();
            DataSchemaNodeBuilder child = entry.getValue();
            if (path.size() == 2) {
                DataSchemaNode node = child.build();
                QName qname = node.getQName();
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
            List<String> key = entry.getKey();
            TypeDefinitionBuilder typedefBuilder = entry.getValue();
            if (key.size() == 2) {
                TypeDefinition<? extends TypeDefinition<?>> node = typedefBuilder
                        .build();
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
        final Set<UsesNode> usesNodeDefs = new HashSet<UsesNode>();
        for (Map.Entry<List<String>, UsesNodeBuilder> entry : addedUsesNodes
                .entrySet()) {
            if (entry.getKey().size() == 2) {
                usesNodeDefs.add(entry.getValue().build());
            }
        }
        return usesNodeDefs;
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
