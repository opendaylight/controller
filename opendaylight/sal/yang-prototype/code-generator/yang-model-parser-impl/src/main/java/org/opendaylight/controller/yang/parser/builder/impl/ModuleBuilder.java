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
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

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
import org.opendaylight.controller.yang.model.api.UnknownSchemaNode;
import org.opendaylight.controller.yang.model.api.UsesNode;
import org.opendaylight.controller.yang.parser.builder.api.AbstractDataNodeContainerBuilder;
import org.opendaylight.controller.yang.parser.builder.api.AugmentationSchemaBuilder;
import org.opendaylight.controller.yang.parser.builder.api.Builder;
import org.opendaylight.controller.yang.parser.builder.api.DataNodeContainerBuilder;
import org.opendaylight.controller.yang.parser.builder.api.DataSchemaNodeBuilder;
import org.opendaylight.controller.yang.parser.builder.api.GroupingBuilder;
import org.opendaylight.controller.yang.parser.builder.api.SchemaNodeBuilder;
import org.opendaylight.controller.yang.parser.builder.api.TypeAwareBuilder;
import org.opendaylight.controller.yang.parser.builder.api.TypeDefinitionBuilder;
import org.opendaylight.controller.yang.parser.builder.api.UsesNodeBuilder;
import org.opendaylight.controller.yang.parser.util.Comparators;
import org.opendaylight.controller.yang.parser.util.RefineHolder;
import org.opendaylight.controller.yang.parser.util.YangParseException;

/**
 * Builder of Module object. If this module is dependent on external
 * module/modules, these dependencies must be resolved before module is built,
 * otherwise result may not be valid.
 */
public class ModuleBuilder extends AbstractDataNodeContainerBuilder {
    private final ModuleImpl instance;
    private final String name;
    private URI namespace;
    private String prefix;
    private Date revision;

    private int augmentsResolved;

    private final LinkedList<Builder> actualPath = new LinkedList<Builder>();
    private final Set<TypeAwareBuilder> dirtyNodes = new HashSet<TypeAwareBuilder>();

    private final Set<ModuleImport> imports = new HashSet<ModuleImport>();
    private final List<AugmentationSchemaBuilder> addedAugments = new ArrayList<AugmentationSchemaBuilder>();
    private final List<AugmentationSchemaBuilder> allAugments = new ArrayList<AugmentationSchemaBuilder>();
    private final Set<UsesNodeBuilder> addedUsesNodes = new HashSet<UsesNodeBuilder>();
    private final List<UsesNodeBuilder> allUsesNodes = new ArrayList<UsesNodeBuilder>();
    private final Set<RpcDefinitionBuilder> addedRpcs = new HashSet<RpcDefinitionBuilder>();
    private final Set<NotificationBuilder> addedNotifications = new HashSet<NotificationBuilder>();
    private final Set<IdentitySchemaNodeBuilder> addedIdentities = new HashSet<IdentitySchemaNodeBuilder>();
    private final Set<FeatureBuilder> addedFeatures = new HashSet<FeatureBuilder>();
    private final Set<DeviationBuilder> addedDeviations = new HashSet<DeviationBuilder>();
    private final Set<TypeDefinitionBuilder> addedTypedefs = new HashSet<TypeDefinitionBuilder>();
    private final List<ExtensionBuilder> addedExtensions = new ArrayList<ExtensionBuilder>();
    private final List<UnknownSchemaNodeBuilder> addedUnknownNodes = new ArrayList<UnknownSchemaNodeBuilder>();
    private final List<UnknownSchemaNodeBuilder> allUnknownNodes = new ArrayList<UnknownSchemaNodeBuilder>();

    public ModuleBuilder(final String name) {
        super(0, null);
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
        final Set<TypeDefinition<?>> typedefs = new TreeSet<TypeDefinition<?>>(Comparators.SCHEMA_NODE_COMP);
        for (TypeDefinitionBuilder tdb : addedTypedefs) {
            typedefs.add(tdb.build());
        }
        instance.setTypeDefinitions(typedefs);

        // CHILD NODES
        final Map<QName, DataSchemaNode> children = new TreeMap<QName, DataSchemaNode>(Comparators.QNAME_COMP);
        for (DataSchemaNodeBuilder child : addedChildNodes) {
            children.put(child.getQName(), child.build());
        }
        instance.setChildNodes(children);

        // GROUPINGS
        final Set<GroupingDefinition> groupings = new TreeSet<GroupingDefinition>(Comparators.SCHEMA_NODE_COMP);
        for (GroupingBuilder gb : addedGroupings) {
            groupings.add(gb.build());
        }
        instance.setGroupings(groupings);

        // USES
        final Set<UsesNode> usesDefinitions = new HashSet<UsesNode>();
        for (UsesNodeBuilder unb : addedUsesNodes) {
            usesDefinitions.add(unb.build());
        }
        instance.setUses(usesDefinitions);

        // FEATURES
        final Set<FeatureDefinition> features = new TreeSet<FeatureDefinition>(Comparators.SCHEMA_NODE_COMP);
        for (FeatureBuilder fb : addedFeatures) {
            features.add(fb.build());
        }
        instance.setFeatures(features);

        // NOTIFICATIONS
        final Set<NotificationDefinition> notifications = new TreeSet<NotificationDefinition>(
                Comparators.SCHEMA_NODE_COMP);
        for (NotificationBuilder entry : addedNotifications) {
            notifications.add(entry.build());
        }
        instance.setNotifications(notifications);

        // AUGMENTATIONS
        final Set<AugmentationSchema> augmentations = new HashSet<AugmentationSchema>();
        for (AugmentationSchemaBuilder builder : addedAugments) {
            augmentations.add(builder.build());
        }
        instance.setAugmentations(augmentations);

        // RPCs
        final Set<RpcDefinition> rpcs = new TreeSet<RpcDefinition>(Comparators.SCHEMA_NODE_COMP);
        for (RpcDefinitionBuilder rpc : addedRpcs) {
            rpcs.add(rpc.build());
        }
        instance.setRpcs(rpcs);

        // DEVIATIONS
        final Set<Deviation> deviations = new HashSet<Deviation>();
        for (DeviationBuilder entry : addedDeviations) {
            deviations.add(entry.build());
        }
        instance.setDeviations(deviations);

        // EXTENSIONS
        final List<ExtensionDefinition> extensions = new ArrayList<ExtensionDefinition>();
        for (ExtensionBuilder eb : addedExtensions) {
            extensions.add(eb.build());
        }
        Collections.sort(extensions, Comparators.SCHEMA_NODE_COMP);
        instance.setExtensionSchemaNodes(extensions);

        // IDENTITIES
        final Set<IdentitySchemaNode> identities = new TreeSet<IdentitySchemaNode>(Comparators.SCHEMA_NODE_COMP);
        for (IdentitySchemaNodeBuilder id : addedIdentities) {
            identities.add(id.build());
        }
        instance.setIdentities(identities);

        // UNKNOWN NODES
        final List<UnknownSchemaNode> unknownNodes = new ArrayList<UnknownSchemaNode>();
        for (UnknownSchemaNodeBuilder unb : addedUnknownNodes) {
            unknownNodes.add(unb.build());
        }
        instance.setUnknownSchemaNodes(unknownNodes);

        return instance;
    }

    @Override
    public void setParent(Builder parent) {
        throw new YangParseException(name, 0, "Can not set parent to module");
    }

    @Override
    public SchemaPath getPath() {
        return null;
    }

    @Override
    public Set<TypeDefinitionBuilder> getTypeDefinitionBuilders() {
        return addedTypedefs;
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

    public Builder getActualParent() {
        if (actualPath.size() < 2) {
            return null;
        } else {
            return actualPath.get(1);
        }
    }

    public Set<TypeAwareBuilder> getDirtyNodes() {
        return dirtyNodes;
    }

    public List<AugmentationSchemaBuilder> getAllAugments() {
        return allAugments;
    }

    public Set<IdentitySchemaNodeBuilder> getIdentities() {
        return addedIdentities;
    }

    public List<UsesNodeBuilder> getAllUsesNodes() {
        return allUsesNodes;
    }

    public Set<DeviationBuilder> getDeviations() {
        return addedDeviations;
    }

    public List<UnknownSchemaNodeBuilder> getAllUnknownNodes() {
        return allUnknownNodes;
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

    public void markActualNodeDirty() {
        final TypeAwareBuilder nodeBuilder = (TypeAwareBuilder) getActualNode();
        dirtyNodes.add(nodeBuilder);
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

    public boolean addModuleImport(final String moduleName, final Date revision, final String prefix) {
        final ModuleImport moduleImport = createModuleImport(moduleName, revision, prefix);
        return imports.add(moduleImport);
    }

    public Set<ModuleImport> getModuleImports() {
        return imports;
    }

    public ExtensionBuilder addExtension(final QName qname, final int line) {
        final ExtensionBuilder builder = new ExtensionBuilder(line, qname);
        addedExtensions.add(builder);
        return builder;
    }

    public ContainerSchemaNodeBuilder addContainerNode(final int line, final QName containerName,
            final SchemaPath schemaPath) {
        final ContainerSchemaNodeBuilder builder = new ContainerSchemaNodeBuilder(line, containerName, schemaPath);

        Builder parent = getActualNode();
        builder.setParent(parent);
        addChildToParent(parent, builder, containerName.getLocalName());

        return builder;
    }

    public ListSchemaNodeBuilder addListNode(final int line, final QName listName, final SchemaPath schemaPath) {
        final ListSchemaNodeBuilder builder = new ListSchemaNodeBuilder(line, listName, schemaPath);

        Builder parent = getActualNode();
        builder.setParent(parent);
        addChildToParent(parent, builder, listName.getLocalName());

        return builder;
    }

    public LeafSchemaNodeBuilder addLeafNode(final int line, final QName leafName, final SchemaPath schemaPath) {
        final LeafSchemaNodeBuilder builder = new LeafSchemaNodeBuilder(leafName, schemaPath, line);

        Builder parent = getActualNode();
        builder.setParent(parent);
        addChildToParent(parent, builder, leafName.getLocalName());

        return builder;
    }

    public LeafListSchemaNodeBuilder addLeafListNode(final int line, final QName leafListName,
            final SchemaPath schemaPath) {
        final LeafListSchemaNodeBuilder builder = new LeafListSchemaNodeBuilder(line, leafListName, schemaPath);

        Builder parent = getActualNode();
        builder.setParent(parent);
        addChildToParent(parent, builder, leafListName.getLocalName());

        return builder;
    }

    public GroupingBuilder addGrouping(final int line, final QName qname) {
        final GroupingBuilder builder = new GroupingBuilderImpl(qname, line);

        Builder parent = getActualNode();
        builder.setParent(parent);

        if (parent == null) {
            for (GroupingBuilder child : addedGroupings) {
                if (child.getQName().getLocalName().equals(qname.getLocalName())) {
                    throw new YangParseException(name, line, "Duplicate node found at line " + child.getLine());
                }
            }
            addedGroupings.add(builder);
        } else {
            if (parent instanceof DataNodeContainerBuilder) {
                DataNodeContainerBuilder parentNode = (DataNodeContainerBuilder) parent;
                for (DataSchemaNodeBuilder child : parentNode.getChildNodeBuilders()) {
                    if (child.getQName().getLocalName().equals(qname.getLocalName())) {
                        throw new YangParseException(name, line, "Duplicate node found at line " + child.getLine());
                    }
                }
                parentNode.addGrouping(builder);
            } else if (parent instanceof RpcDefinitionBuilder) {
                RpcDefinitionBuilder parentNode = (RpcDefinitionBuilder) parent;
                for (GroupingBuilder child : parentNode.getGroupings()) {
                    if (child.getQName().getLocalName().equals(qname.getLocalName())) {
                        throw new YangParseException(name, line, "Duplicate node found at line " + child.getLine());
                    }
                }
                parentNode.addGrouping(builder);
            } else {
                throw new YangParseException(name, line, "Unresolved parent of grouping " + qname.getLocalName());
            }
        }

        return builder;
    }

    public AugmentationSchemaBuilder addAugment(final int line, final String augmentTargetStr) {
        final AugmentationSchemaBuilder builder = new AugmentationSchemaBuilderImpl(line, augmentTargetStr);

        Builder parent = getActualNode();
        builder.setParent(parent);

        if (parent == null) {
            addedAugments.add(builder);
        } else {
            // augment can only be in 'module' or 'uses' statement
            if (parent instanceof UsesNodeBuilder) {
                ((UsesNodeBuilder) parent).addAugment(builder);
            } else {
                throw new YangParseException(name, line, "Augment can be declared only under module or uses.");
            }
        }
        allAugments.add(builder);

        return builder;
    }

    @Override
    public void addUsesNode(UsesNodeBuilder usesBuilder) {
        addedUsesNodes.add(usesBuilder);
        allUsesNodes.add(usesBuilder);
    }

    public UsesNodeBuilder addUsesNode(final int line, final String groupingPathStr) {
        final UsesNodeBuilder usesBuilder = new UsesNodeBuilderImpl(line, groupingPathStr);

        Builder parent = getActualNode();
        usesBuilder.setParent(parent);

        if (parent == null) {
            addedUsesNodes.add(usesBuilder);
        } else {
            if (!(parent instanceof DataNodeContainerBuilder)) {
                throw new YangParseException(name, line, "Unresolved parent of uses '" + groupingPathStr + "'.");
            }
            if (parent instanceof AugmentationSchemaBuilder) {
                usesBuilder.setAugmenting(true);
            }
            ((DataNodeContainerBuilder) parent).addUsesNode(usesBuilder);
        }
        allUsesNodes.add(usesBuilder);
        return usesBuilder;
    }

    public void addRefine(final RefineHolder refine, final List<String> parentPath) {
        final List<String> path = new ArrayList<String>(parentPath);

        if (actualPath.isEmpty()) {
            throw new YangParseException(name, refine.getLine(), "refine can be defined only in uses statement");
        } else {
            final Builder parent = getActualNode();
            if (parent instanceof UsesNodeBuilder) {
                ((UsesNodeBuilder) parent).addRefine(refine);
            } else {
                throw new YangParseException(name, refine.getLine(), "refine can be defined only in uses statement");
            }
            refine.setParent(parent);
        }

        path.add(refine.getName());
    }

    public RpcDefinitionBuilder addRpc(final int line, final QName qname) {
        Builder parent = getActualNode();
        if (parent != null) {
            throw new YangParseException(name, line, "rpc can be defined only in module or submodule");
        }

        final RpcDefinitionBuilder rpcBuilder = new RpcDefinitionBuilder(line, qname);
        for (RpcDefinitionBuilder rpc : addedRpcs) {
            if (rpc.getQName().getLocalName().equals(qname.getLocalName())) {
                throw new YangParseException(name, line, "Duplicate node found at line " + rpc.getLine());
            }
        }
        addedRpcs.add(rpcBuilder);
        return rpcBuilder;
    }

    public ContainerSchemaNodeBuilder addRpcInput(final SchemaPath schemaPath, final QName inputQName, final int line) {
        final Builder parent = getActualNode();
        if (!(parent instanceof RpcDefinitionBuilder)) {
            throw new YangParseException(name, line, "input can be defined only in rpc statement");
        }
        final RpcDefinitionBuilder rpc = (RpcDefinitionBuilder) parent;

        final ContainerSchemaNodeBuilder inputBuilder = new ContainerSchemaNodeBuilder(line, inputQName, schemaPath);
        inputBuilder.setParent(rpc);

        rpc.setInput(inputBuilder);
        return inputBuilder;
    }

    public ContainerSchemaNodeBuilder addRpcOutput(final SchemaPath schemaPath, final QName outputQName, final int line) {
        final Builder parent = actualPath.getFirst();
        if (!(parent instanceof RpcDefinitionBuilder)) {
            throw new YangParseException(name, line, "output can be defined only in rpc statement");
        }
        final RpcDefinitionBuilder rpc = (RpcDefinitionBuilder) parent;

        final ContainerSchemaNodeBuilder outputBuilder = new ContainerSchemaNodeBuilder(line, outputQName, schemaPath);
        outputBuilder.setParent(rpc);

        rpc.setOutput(outputBuilder);
        return outputBuilder;
    }

    public NotificationBuilder addNotification(final QName notificationName, final List<String> parentPath,
            final int line) {
        if (!(actualPath.isEmpty())) {
            throw new YangParseException(name, line, "notification can be defined only in module or submodule");
        }
        for (NotificationBuilder nb : addedNotifications) {
            if (nb.getQName().equals(notificationName)) {
                throw new YangParseException(name, line, "Duplicate node found at line " + nb.getLine());
            }
        }

        final NotificationBuilder builder = new NotificationBuilder(line, notificationName);
        addedNotifications.add(builder);

        return builder;
    }

    public FeatureBuilder addFeature(final int line, final QName featureName) {
        Builder parent = getActualNode();
        if (parent != null) {
            throw new YangParseException(name, line, "feature can be defined only in module or submodule");
        }

        final FeatureBuilder builder = new FeatureBuilder(line, featureName);
        for (FeatureBuilder fb : addedFeatures) {
            if (fb.getQName().getLocalName().equals(featureName.getLocalName())) {
                throw new YangParseException(name, line, "Duplicate node found at line " + fb.getLine());
            }
        }
        addedFeatures.add(builder);
        return builder;
    }

    public ChoiceBuilder addChoice(final int line, final QName choiceName) {
        final ChoiceBuilder builder = new ChoiceBuilder(line, choiceName);

        Builder parent = getActualNode();
        builder.setParent(parent);
        addChildToParent(parent, builder, choiceName.getLocalName());

        return builder;
    }

    public ChoiceCaseBuilder addCase(final int line, final QName caseName) {
        Builder parent = getActualNode();
        if (parent == null) {
            throw new YangParseException(name, line, "'case' parent not found");
        }

        final ChoiceCaseBuilder builder = new ChoiceCaseBuilder(line, caseName);
        builder.setParent(parent);

        if (parent instanceof ChoiceBuilder) {
            ((ChoiceBuilder) parent).addChildNode(builder);
        } else if (parent instanceof AugmentationSchemaBuilder) {
            ((AugmentationSchemaBuilder) parent).addChildNode(builder);
        } else {
            throw new YangParseException(name, line, "Unresolved parent of 'case' " + caseName.getLocalName());
        }

        return builder;
    }

    public AnyXmlBuilder addAnyXml(final int line, final QName anyXmlName, final SchemaPath schemaPath) {
        final AnyXmlBuilder builder = new AnyXmlBuilder(line, anyXmlName, schemaPath);

        Builder parent = getActualNode();
        builder.setParent(parent);
        addChildToParent(parent, builder, anyXmlName.getLocalName());

        return builder;
    }

    @Override
    public void addTypedef(TypeDefinitionBuilder typedefBuilder) {
        for (TypeDefinitionBuilder tdb : addedTypedefs) {
            if (tdb.getQName().getLocalName().equals(typedefBuilder.getQName().getLocalName())) {
                throw new YangParseException(name, typedefBuilder.getLine(), "Duplicate node found at line "
                        + tdb.getLine());
            }
        }
        addedTypedefs.add(typedefBuilder);
    }

    public TypeDefinitionBuilderImpl addTypedef(final int line, final QName typeDefName) {
        final TypeDefinitionBuilderImpl builder = new TypeDefinitionBuilderImpl(typeDefName, line);

        Builder parent = getActualNode();
        builder.setParent(parent);

        if (parent == null) {
            for (TypeDefinitionBuilder tdb : addedTypedefs) {
                if (tdb.getQName().getLocalName().equals(builder.getQName().getLocalName())) {
                    throw new YangParseException(name, builder.getLine(), "Duplicate node found at line "
                            + tdb.getLine());
                }
            }
            addedTypedefs.add(builder);
        } else {
            if (parent instanceof DataNodeContainerBuilder) {
                DataNodeContainerBuilder parentNode = (DataNodeContainerBuilder) parent;
                for (DataSchemaNodeBuilder child : parentNode.getChildNodeBuilders()) {
                    if (child.getQName().getLocalName().equals(typeDefName.getLocalName())) {
                        throw new YangParseException(name, line, "Duplicate node found at line " + child.getLine());
                    }
                }
                parentNode.addTypedef(builder);
            } else if (parent instanceof RpcDefinitionBuilder) {
                RpcDefinitionBuilder rpcParent = (RpcDefinitionBuilder) parent;
                for (TypeDefinitionBuilder tdb : rpcParent.getTypeDefinitions()) {
                    if (tdb.getQName().getLocalName().equals(builder.getQName().getLocalName())) {
                        throw new YangParseException(name, builder.getLine(), "Duplicate node found at line "
                                + tdb.getLine());
                    }
                }
                rpcParent.addTypedef(builder);
            } else {
                throw new YangParseException(name, line, "Unresolved parent of typedef " + typeDefName.getLocalName());
            }
        }

        return builder;
    }

    public void setType(final TypeDefinition<?> type) {
        Builder parent = getActualNode();
        if (parent == null || !(parent instanceof TypeAwareBuilder)) {
            throw new YangParseException("Failed to set type '" + type.getQName().getLocalName()
                    + "'. Invalid parent node: " + parent);
        }
        ((TypeAwareBuilder) parent).setType(type);
    }

    public UnionTypeBuilder addUnionType(final int line, final URI namespace, final Date revision) {
        final Builder parent = getActualNode();
        if (parent == null) {
            throw new YangParseException(line, "Error while parsing union type");
        } else {
            final UnionTypeBuilder union = new UnionTypeBuilder(line);
            if (parent instanceof TypeAwareBuilder) {
                ((TypeAwareBuilder) parent).setTypedef(union);
                return union;
            } else {
                throw new YangParseException(name, line, "Invalid parent of union type.");
            }
        }
    }

    public void addIdentityrefType(final int line, final SchemaPath schemaPath, final String baseString) {
        final IdentityrefTypeBuilder identityref = new IdentityrefTypeBuilder(baseString, schemaPath, line);

        final Builder parent = getActualNode();
        if (parent == null) {
            throw new YangParseException(line, "Error while parsing identityref type.");
        } else {
            if (parent instanceof TypeAwareBuilder) {
                final TypeAwareBuilder typeParent = (TypeAwareBuilder) parent;
                typeParent.setTypedef(identityref);
                dirtyNodes.add(typeParent);
            } else {
                throw new YangParseException(name, line, "Invalid parent of identityref type.");
            }
        }
    }

    public DeviationBuilder addDeviation(final int line, final String targetPath) {
        Builder parent = getActualNode();
        if (parent != null) {
            throw new YangParseException(name, line, "deviation can be defined only in module or submodule");
        }

        final DeviationBuilder builder = new DeviationBuilder(line, targetPath);
        addedDeviations.add(builder);
        return builder;
    }

    public IdentitySchemaNodeBuilder addIdentity(final QName qname, final List<String> parentPath, final int line) {
        Builder parent = getActualNode();
        if (parent != null) {
            throw new YangParseException(name, line, "identity can be defined only in module or submodule");
        }
        for (IdentitySchemaNodeBuilder idBuilder : addedIdentities) {
            if (idBuilder.getQName().equals(qname)) {
                throw new YangParseException(name, line, "Duplicate node found at line " + idBuilder.getLine());
            }
        }

        final IdentitySchemaNodeBuilder builder = new IdentitySchemaNodeBuilder(line, qname);
        addedIdentities.add(builder);
        return builder;
    }

    @Override
    public void addUnknownSchemaNode(final UnknownSchemaNodeBuilder builder) {
        addedUnknownNodes.add(builder);
        allUnknownNodes.add(builder);
    }

    public UnknownSchemaNodeBuilder addUnknownSchemaNode(final int line, final QName qname) {
        final Builder parent = getActualNode();
        final UnknownSchemaNodeBuilder builder = new UnknownSchemaNodeBuilder(line, qname);
        builder.setParent(parent);
        allUnknownNodes.add(builder);

        if (parent == null) {
            addedUnknownNodes.add(builder);
        } else {
            if (parent instanceof SchemaNodeBuilder) {
                ((SchemaNodeBuilder) parent).addUnknownSchemaNode(builder);
            } else if (parent instanceof DataNodeContainerBuilder) {
                ((DataNodeContainerBuilder) parent).addUnknownSchemaNode(builder);
            } else if (parent instanceof RefineHolder) {
                ((RefineHolder) parent).addUnknownSchemaNode(builder);
            } else {
                throw new YangParseException(name, line, "Unresolved parent of unknown node '" + qname.getLocalName()
                        + "'");
            }
        }

        return builder;
    }

    @Override
    public String toString() {
        return "module " + name;
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
        private Set<NotificationDefinition> notifications = Collections.emptySet();
        private Set<AugmentationSchema> augmentations = Collections.emptySet();
        private Set<RpcDefinition> rpcs = Collections.emptySet();
        private Set<Deviation> deviations = Collections.emptySet();
        private Map<QName, DataSchemaNode> childNodes = Collections.emptyMap();
        private Set<GroupingDefinition> groupings = Collections.emptySet();
        private Set<UsesNode> uses = Collections.emptySet();
        private List<ExtensionDefinition> extensionNodes = Collections.emptyList();
        private Set<IdentitySchemaNode> identities = Collections.emptySet();
        private List<UnknownSchemaNode> unknownNodes = Collections.emptyList();

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
            return new LinkedHashSet<DataSchemaNode>(childNodes.values());
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

        private void setExtensionSchemaNodes(final List<ExtensionDefinition> extensionNodes) {
            if (extensionNodes != null) {
                this.extensionNodes = extensionNodes;
            }
        }

        @Override
        public Set<IdentitySchemaNode> getIdentities() {
            return identities;
        }

        private void setIdentities(final Set<IdentitySchemaNode> identities) {
            if (identities != null) {
                this.identities = identities;
            }
        }

        @Override
        public List<UnknownSchemaNode> getUnknownSchemaNodes() {
            return unknownNodes;
        }

        private void setUnknownSchemaNodes(final List<UnknownSchemaNode> unknownNodes) {
            if (unknownNodes != null) {
                this.unknownNodes = unknownNodes;
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
            StringBuilder sb = new StringBuilder(ModuleImpl.class.getSimpleName());
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

    private void addChildToParent(final Builder parent, final DataSchemaNodeBuilder child, final String childLocalName) {
        final int line = child.getLine();
        if (parent == null) {
            // if parent == null => node is defined under module
            // All leafs, leaf-lists, lists, containers, choices, rpcs,
            // notifications, and anyxmls defined within a parent node or at the
            // top level of the module or its submodules share the same
            // identifier namespace.
            for (DataSchemaNodeBuilder childNode : addedChildNodes) {
                if (childNode.getQName().getLocalName().equals(childLocalName)) {
                    throw new YangParseException(name, line, "Duplicate node found at line " + childNode.getLine());
                }
            }
            for (RpcDefinitionBuilder rpc : addedRpcs) {
                if (rpc.getQName().getLocalName().equals(childLocalName)) {
                    throw new YangParseException(name, line, "Duplicate node found at line " + rpc.getLine());
                }
            }
            for (NotificationBuilder notification : addedNotifications) {
                if (notification.getQName().getLocalName().equals(childLocalName)) {
                    throw new YangParseException(name, line, "Duplicate node found at line " + notification.getLine());
                }
            }
            addedChildNodes.add(child);
        } else {
            // no need for checking rpc and notification because they can be
            // defined only under module or submodule
            if (parent instanceof DataNodeContainerBuilder) {
                DataNodeContainerBuilder parentNode = (DataNodeContainerBuilder) parent;
                for (DataSchemaNodeBuilder childNode : parentNode.getChildNodeBuilders()) {
                    if (childNode.getQName().getLocalName().equals(childLocalName)) {
                        throw new YangParseException(name, line, "Duplicate node found at line " + childNode.getLine());
                    }
                }
                parentNode.addChildNode(child);
            } else if (parent instanceof ChoiceBuilder) {
                ChoiceBuilder parentNode = (ChoiceBuilder) parent;
                for (ChoiceCaseBuilder caseBuilder : parentNode.getCases()) {
                    if (caseBuilder.getQName().getLocalName().equals(childLocalName)) {
                        throw new YangParseException(name, line, "Duplicate node found at line "
                                + caseBuilder.getLine());
                    }
                }
                parentNode.addChildNode(child);
            } else {
                throw new YangParseException(name, line, "Unresolved parent of node '" + childLocalName + "'.");
            }
        }
    }

    private ModuleImport createModuleImport(final String moduleName, final Date revision, final String prefix) {
        final ModuleImport moduleImport = new ModuleImport() {
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
                result = prime * result + ((moduleName == null) ? 0 : moduleName.hashCode());
                result = prime * result + ((revision == null) ? 0 : revision.hashCode());
                result = prime * result + ((prefix == null) ? 0 : prefix.hashCode());
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
                return "ModuleImport[moduleName=" + moduleName + ", revision=" + revision + ", prefix=" + prefix + "]";
            }
        };
        return moduleImport;
    }

}
