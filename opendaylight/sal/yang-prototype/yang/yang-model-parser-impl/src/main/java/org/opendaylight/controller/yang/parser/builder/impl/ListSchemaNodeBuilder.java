/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.yang.parser.builder.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.opendaylight.controller.yang.common.QName;
import org.opendaylight.controller.yang.model.api.AugmentationSchema;
import org.opendaylight.controller.yang.model.api.ConstraintDefinition;
import org.opendaylight.controller.yang.model.api.DataSchemaNode;
import org.opendaylight.controller.yang.model.api.GroupingDefinition;
import org.opendaylight.controller.yang.model.api.ListSchemaNode;
import org.opendaylight.controller.yang.model.api.SchemaPath;
import org.opendaylight.controller.yang.model.api.Status;
import org.opendaylight.controller.yang.model.api.TypeDefinition;
import org.opendaylight.controller.yang.model.api.UnknownSchemaNode;
import org.opendaylight.controller.yang.model.api.UsesNode;
import org.opendaylight.controller.yang.parser.builder.api.AbstractDataNodeContainerBuilder;
import org.opendaylight.controller.yang.parser.builder.api.AugmentationSchemaBuilder;
import org.opendaylight.controller.yang.parser.builder.api.AugmentationTargetBuilder;
import org.opendaylight.controller.yang.parser.builder.api.DataSchemaNodeBuilder;
import org.opendaylight.controller.yang.parser.builder.api.GroupingBuilder;
import org.opendaylight.controller.yang.parser.builder.api.GroupingMember;
import org.opendaylight.controller.yang.parser.builder.api.TypeDefinitionBuilder;
import org.opendaylight.controller.yang.parser.builder.api.UsesNodeBuilder;
import org.opendaylight.controller.yang.parser.util.Comparators;
import org.opendaylight.controller.yang.parser.util.YangParseException;

public final class ListSchemaNodeBuilder extends AbstractDataNodeContainerBuilder implements DataSchemaNodeBuilder,
        AugmentationTargetBuilder, GroupingMember {
    private boolean isBuilt;
    private final ListSchemaNodeImpl instance;
    // SchemaNode args
    private SchemaPath schemaPath;
    private String description;
    private String reference;
    private Status status = Status.CURRENT;
    // DataSchemaNode args
    private boolean augmenting;
    private boolean addedByUses;
    private Boolean configuration;
    private final ConstraintsBuilder constraints;
    // DataNodeContainer args
    private Set<TypeDefinition<?>> typedefs;
    private final Set<TypeDefinitionBuilder> addedTypedefs = new HashSet<TypeDefinitionBuilder>();
    private Set<UsesNode> usesNodes;
    private final Set<UsesNodeBuilder> addedUsesNodes = new HashSet<UsesNodeBuilder>();
    // AugmentationTarget args
    private Set<AugmentationSchema> augmentations;
    private final Set<AugmentationSchemaBuilder> addedAugmentations = new HashSet<AugmentationSchemaBuilder>();
    // ListSchemaNode args
    private List<QName> keyDefinition = Collections.emptyList();
    private boolean userOrdered;

    public ListSchemaNodeBuilder(final String moduleName, final int line, final QName qname, final SchemaPath schemaPath) {
        super(moduleName, line, qname);
        this.schemaPath = schemaPath;
        instance = new ListSchemaNodeImpl(qname);
        constraints = new ConstraintsBuilder(moduleName, line);
    }

    public ListSchemaNodeBuilder(final ListSchemaNodeBuilder b) {
        super(b.getModuleName(), b.getLine(), b.getQName());
        instance = new ListSchemaNodeImpl(b.getQName());
        constraints = b.getConstraints();
        schemaPath = b.getPath();
        description = b.getDescription();
        reference = b.getReference();
        status = b.getStatus();
        augmenting = b.isAugmenting();
        addedByUses = b.isAddedByUses();
        configuration = b.isConfiguration();
        keyDefinition = b.getKeyDefinition();
        userOrdered = b.isUserOrdered();
        childNodes = b.getChildNodes();
        addedChildNodes.addAll(b.getChildNodeBuilders());
        groupings = b.getGroupings();
        addedGroupings.addAll(b.getGroupingBuilders());
        typedefs = b.typedefs;
        addedTypedefs.addAll(b.getTypeDefinitionBuilders());
        usesNodes = b.usesNodes;
        addedUsesNodes.addAll(b.getUsesNodes());
        augmentations = b.augmentations;
        addedAugmentations.addAll(b.getAugmentations());
        unknownNodes = b.unknownNodes;
        addedUnknownNodes.addAll(b.getUnknownNodeBuilders());
    }

    @Override
    public ListSchemaNode build() {
        if (!isBuilt) {
            instance.setKeyDefinition(keyDefinition);
            instance.setPath(schemaPath);
            instance.setDescription(description);
            instance.setReference(reference);
            instance.setStatus(status);
            instance.setAugmenting(augmenting);
            instance.setAddedByUses(addedByUses);
            instance.setConfiguration(configuration);
            instance.setUserOrdered(userOrdered);

            // CHILD NODES
            final Map<QName, DataSchemaNode> childs = new TreeMap<QName, DataSchemaNode>(Comparators.QNAME_COMP);
            if (childNodes == null || childNodes.isEmpty()) {
                for (DataSchemaNodeBuilder node : addedChildNodes) {
                    childs.put(node.getQName(), node.build());
                }
            } else {
                for (DataSchemaNode node : childNodes) {
                    childs.put(node.getQName(), node);
                }
            }
            instance.setChildNodes(childs);

            // TYPEDEFS
            if (typedefs == null) {
                typedefs = new TreeSet<TypeDefinition<?>>(Comparators.SCHEMA_NODE_COMP);
                for (TypeDefinitionBuilder entry : addedTypedefs) {
                    typedefs.add(entry.build());
                }
            }
            instance.setTypeDefinitions(typedefs);

            // USES
            if (usesNodes == null) {
                usesNodes = new HashSet<UsesNode>();
                for (UsesNodeBuilder builder : addedUsesNodes) {
                    usesNodes.add(builder.build());
                }
            }
            instance.setUses(usesNodes);

            // GROUPINGS
            if (groupings == null) {
                groupings = new TreeSet<GroupingDefinition>(Comparators.SCHEMA_NODE_COMP);
                for (GroupingBuilder builder : addedGroupings) {
                    groupings.add(builder.build());
                }
            }
            instance.setGroupings(groupings);

            // AUGMENTATIONS
            if (augmentations == null) {
                augmentations = new HashSet<AugmentationSchema>();
                for (AugmentationSchemaBuilder builder : addedAugmentations) {
                    augmentations.add(builder.build());
                }
            }
            instance.setAvailableAugmentations(augmentations);

            // UNKNOWN NODES
            if (unknownNodes == null) {
                unknownNodes = new ArrayList<UnknownSchemaNode>();
                for (UnknownSchemaNodeBuilder b : addedUnknownNodes) {
                    unknownNodes.add(b.build());
                }
                Collections.sort(unknownNodes, Comparators.SCHEMA_NODE_COMP);
            }
            instance.setUnknownSchemaNodes(unknownNodes);

            instance.setConstraints(constraints.build());
            instance.setAvailableAugmentations(augmentations);

            isBuilt = true;
        }
        return instance;
    }

    @Override
    public void rebuild() {
        isBuilt = false;
        build();
    }

    @Override
    public Set<TypeDefinitionBuilder> getTypeDefinitionBuilders() {
        return addedTypedefs;
    }

    @Override
    public void addTypedef(final TypeDefinitionBuilder type) {
        String typeName = type.getQName().getLocalName();
        for (TypeDefinitionBuilder addedTypedef : addedTypedefs) {
            throw new YangParseException(moduleName, type.getLine(), "Can not add typedef '" + typeName
                    + "': typedef with same name already declared at line " + addedTypedef.getLine());
        }
        addedTypedefs.add(type);
    }

    public void setTypedefs(final Set<TypeDefinition<?>> typedefs) {
        this.typedefs = typedefs;
    }

    public SchemaPath getPath() {
        return schemaPath;
    }

    @Override
    public void setPath(final SchemaPath schemaPath) {
        this.schemaPath = schemaPath;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public void setDescription(final String description) {
        this.description = description;
    }

    public String getReference() {
        return reference;
    }

    @Override
    public void setReference(String reference) {
        this.reference = reference;
    }

    public Status getStatus() {
        return status;
    }

    @Override
    public void setStatus(Status status) {
        if (status != null) {
            this.status = status;
        }
    }

    public Set<UsesNodeBuilder> getUsesNodes() {
        return addedUsesNodes;
    }

    @Override
    public void addUsesNode(final UsesNodeBuilder usesBuilder) {
        addedUsesNodes.add(usesBuilder);
    }

    public void setUsesnodes(final Set<UsesNode> usesNodes) {
        this.usesNodes = usesNodes;
    }

    public Set<AugmentationSchemaBuilder> getAugmentations() {
        return addedAugmentations;
    }

    @Override
    public void addAugmentation(AugmentationSchemaBuilder augment) {
        addedAugmentations.add(augment);
    }

    public void setAugmentations(final Set<AugmentationSchema> augmentations) {
        this.augmentations = augmentations;
    }

    public List<QName> getKeyDefinition() {
        return keyDefinition;
    }

    public void setKeyDefinition(final List<QName> keyDefinition) {
        if (keyDefinition != null) {
            this.keyDefinition = keyDefinition;
        }
    }

    public boolean isAugmenting() {
        return augmenting;
    }

    @Override
    public void setAugmenting(boolean augmenting) {
        this.augmenting = augmenting;
    }

    @Override
    public boolean isAddedByUses() {
        return addedByUses;
    }

    @Override
    public void setAddedByUses(final boolean addedByUses) {
        this.addedByUses = addedByUses;
    }

    public Boolean isConfiguration() {
        return configuration;
    }

    @Override
    public void setConfiguration(Boolean configuration) {
        this.configuration = configuration;
    }

    @Override
    public ConstraintsBuilder getConstraints() {
        return constraints;
    }

    public boolean isUserOrdered() {
        return userOrdered;
    }

    public void setUserOrdered(final boolean userOrdered) {
        this.userOrdered = userOrdered;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((schemaPath == null) ? 0 : schemaPath.hashCode());
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
        ListSchemaNodeBuilder other = (ListSchemaNodeBuilder) obj;
        if (schemaPath == null) {
            if (other.schemaPath != null) {
                return false;
            }
        } else if (!schemaPath.equals(other.schemaPath)) {
            return false;
        }
        if (parent == null) {
            if (other.parent != null) {
                return false;
            }
        } else if (!parent.equals(other.parent)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "list " + qname.getLocalName();
    }

    public final class ListSchemaNodeImpl implements ListSchemaNode {
        private final QName qname;
        private SchemaPath path;
        private String description;
        private String reference;
        private Status status = Status.CURRENT;
        private List<QName> keyDefinition = Collections.emptyList();
        private boolean augmenting;
        private boolean addedByUses;
        private boolean configuration;
        private ConstraintDefinition constraints;
        private Set<AugmentationSchema> augmentations = Collections.emptySet();
        private Map<QName, DataSchemaNode> childNodes = Collections.emptyMap();
        private Set<TypeDefinition<?>> typeDefinitions = Collections.emptySet();
        private Set<GroupingDefinition> groupings = Collections.emptySet();
        private Set<UsesNode> uses = Collections.emptySet();
        private boolean userOrdered;
        private List<UnknownSchemaNode> unknownNodes = Collections.emptyList();

        private ListSchemaNodeImpl(final QName qname) {
            this.qname = qname;
        }

        @Override
        public QName getQName() {
            return qname;
        }

        @Override
        public SchemaPath getPath() {
            return path;
        }

        private void setPath(final SchemaPath path) {
            this.path = path;
        }

        @Override
        public String getDescription() {
            return description;
        }

        private void setDescription(final String description) {
            this.description = description;
        }

        @Override
        public String getReference() {
            return reference;
        }

        private void setReference(final String reference) {
            this.reference = reference;
        }

        @Override
        public Status getStatus() {
            return status;
        }

        private void setStatus(Status status) {
            this.status = status;
        }

        @Override
        public List<QName> getKeyDefinition() {
            return keyDefinition;
        }

        private void setKeyDefinition(List<QName> keyDefinition) {
            if (keyDefinition != null) {
                this.keyDefinition = keyDefinition;
            }
        }

        @Override
        public boolean isAugmenting() {
            return augmenting;
        }

        private void setAugmenting(boolean augmenting) {
            this.augmenting = augmenting;
        }

        @Override
        public boolean isAddedByUses() {
            return addedByUses;
        }

        private void setAddedByUses(final boolean addedByUses) {
            this.addedByUses = addedByUses;
        }

        @Override
        public boolean isConfiguration() {
            return configuration;
        }

        private void setConfiguration(boolean configuration) {
            this.configuration = configuration;
        }

        @Override
        public ConstraintDefinition getConstraints() {
            return constraints;
        }

        private void setConstraints(ConstraintDefinition constraints) {
            this.constraints = constraints;
        }

        @Override
        public Set<AugmentationSchema> getAvailableAugmentations() {
            return augmentations;
        }

        private void setAvailableAugmentations(Set<AugmentationSchema> augmentations) {
            if (augmentations != null) {
                this.augmentations = augmentations;
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
        public Set<TypeDefinition<?>> getTypeDefinitions() {
            return typeDefinitions;
        }

        private void setTypeDefinitions(Set<TypeDefinition<?>> typeDefinitions) {
            if (typeDefinitions != null) {
                this.typeDefinitions = typeDefinitions;
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
        public boolean isUserOrdered() {
            return userOrdered;
        }

        private void setUserOrdered(boolean userOrdered) {
            this.userOrdered = userOrdered;
        }

        @Override
        public List<UnknownSchemaNode> getUnknownSchemaNodes() {
            return unknownNodes;
        }

        private void setUnknownSchemaNodes(List<UnknownSchemaNode> unknownNodes) {
            if (unknownNodes != null) {
                this.unknownNodes = unknownNodes;
            }
        }

        public ListSchemaNodeBuilder toBuilder() {
            return ListSchemaNodeBuilder.this;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((qname == null) ? 0 : qname.hashCode());
            result = prime * result + ((path == null) ? 0 : path.hashCode());
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
            final ListSchemaNodeImpl other = (ListSchemaNodeImpl) obj;
            if (qname == null) {
                if (other.qname != null) {
                    return false;
                }
            } else if (!qname.equals(other.qname)) {
                return false;
            }
            if (path == null) {
                if (other.path != null) {
                    return false;
                }
            } else if (!path.equals(other.path)) {
                return false;
            }
            return true;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder(ListSchemaNodeImpl.class.getSimpleName());
            sb.append("[");
            sb.append("qname=" + qname);
            sb.append(", path=" + path);
            sb.append(", keyDefinition=" + keyDefinition);
            sb.append("]");
            return sb.toString();
        }
    }

}
