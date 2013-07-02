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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.opendaylight.controller.yang.common.QName;
import org.opendaylight.controller.yang.model.api.DataSchemaNode;
import org.opendaylight.controller.yang.model.api.GroupingDefinition;
import org.opendaylight.controller.yang.model.api.SchemaPath;
import org.opendaylight.controller.yang.model.api.Status;
import org.opendaylight.controller.yang.model.api.TypeDefinition;
import org.opendaylight.controller.yang.model.api.UnknownSchemaNode;
import org.opendaylight.controller.yang.model.api.UsesNode;
import org.opendaylight.controller.yang.parser.builder.api.Builder;
import org.opendaylight.controller.yang.parser.builder.api.DataSchemaNodeBuilder;
import org.opendaylight.controller.yang.parser.builder.api.GroupingBuilder;
import org.opendaylight.controller.yang.parser.builder.api.TypeDefinitionBuilder;
import org.opendaylight.controller.yang.parser.builder.api.UsesNodeBuilder;
import org.opendaylight.controller.yang.parser.util.Comparators;

public final class GroupingBuilderImpl implements GroupingBuilder {
    private Builder parent;
    private boolean isBuilt;
    private final GroupingDefinitionImpl instance;
    private final int line;
    private final QName qname;
    private SchemaPath schemaPath;
    private String description;
    private String reference;
    private Status status = Status.CURRENT;
    private boolean addedByUses;

    private Set<DataSchemaNode> childNodes;
    private final Set<DataSchemaNodeBuilder> addedChildNodes = new HashSet<DataSchemaNodeBuilder>();

    private Set<GroupingDefinition> groupings;
    private final Set<GroupingBuilder> addedGroupings = new HashSet<GroupingBuilder>();

    private Set<TypeDefinition<?>> typedefs;
    private final Set<TypeDefinitionBuilder> addedTypedefs = new HashSet<TypeDefinitionBuilder>();

    private Set<UsesNode> usesNodes;
    private final Set<UsesNodeBuilder> addedUsesNodes = new HashSet<UsesNodeBuilder>();

    private List<UnknownSchemaNode> unknownNodes;
    private final List<UnknownSchemaNodeBuilder> addedUnknownNodes = new ArrayList<UnknownSchemaNodeBuilder>();

    public GroupingBuilderImpl(final QName qname, final int line) {
        this.qname = qname;
        instance = new GroupingDefinitionImpl(qname);
        this.line = line;
    }

    public GroupingBuilderImpl(GroupingBuilder builder) {
        qname = builder.getQName();
        parent = builder.getParent();
        instance = new GroupingDefinitionImpl(qname);
        line = builder.getLine();
        schemaPath = builder.getPath();
        description = builder.getDescription();
        reference = builder.getReference();
        status = builder.getStatus();
        addedByUses = builder.isAddedByUses();
        childNodes = builder.getChildNodes();
        addedChildNodes.addAll(builder.getChildNodeBuilders());
        groupings = builder.getGroupings();
        addedGroupings.addAll(builder.getGroupingBuilders());
        addedUsesNodes.addAll(builder.getUses());
        addedUnknownNodes.addAll(builder.getUnknownNodes());
    }

    @Override
    public GroupingDefinition build() {
        if (!isBuilt) {
            instance.setPath(schemaPath);
            instance.setDescription(description);
            instance.setReference(reference);
            instance.setStatus(status);
            instance.setAddedByUses(addedByUses);

            // CHILD NODES
            final Map<QName, DataSchemaNode> childs = new HashMap<QName, DataSchemaNode>();
            if (childNodes == null) {
                for (DataSchemaNodeBuilder node : addedChildNodes) {
                    childs.put(node.getQName(), node.build());
                }
            } else {
                for (DataSchemaNode node : childNodes) {
                    childs.put(node.getQName(), node);
                }
            }
            instance.setChildNodes(childs);

            // GROUPINGS
            if (groupings == null) {
                groupings = new TreeSet<GroupingDefinition>(Comparators.SCHEMA_NODE_COMP);
                for (GroupingBuilder builder : addedGroupings) {
                    groupings.add(builder.build());
                }
            }
            instance.setGroupings(groupings);

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

            // UNKNOWN NODES
            if (unknownNodes == null) {
                unknownNodes = new ArrayList<UnknownSchemaNode>();
                for (UnknownSchemaNodeBuilder b : addedUnknownNodes) {
                    unknownNodes.add(b.build());
                }
                Collections.sort(unknownNodes, Comparators.SCHEMA_NODE_COMP);
            }
            instance.setUnknownSchemaNodes(unknownNodes);

            isBuilt = true;
        }

        return instance;
    }

    @Override
    public int getLine() {
        return line;
    }

    @Override
    public Builder getParent() {
        return parent;
    }

    @Override
    public void setParent(final Builder parent) {
        this.parent = parent;
    }

    @Override
    public QName getQName() {
        return qname;
    }

    @Override
    public Set<TypeDefinitionBuilder> getTypeDefinitionBuilders() {
        return addedTypedefs;
    }

    @Override
    public void addTypedef(final TypeDefinitionBuilder type) {
        addedTypedefs.add(type);
    }

    public void setTypedefs(final Set<TypeDefinition<?>> typedefs) {
        this.typedefs = typedefs;
    }

    @Override
    public SchemaPath getPath() {
        return schemaPath;
    }

    @Override
    public void setPath(SchemaPath schemaPath) {
        this.schemaPath = schemaPath;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public void setDescription(final String description) {
        this.description = description;
    }

    @Override
    public String getReference() {
        return reference;
    }

    @Override
    public void setReference(final String reference) {
        this.reference = reference;
    }

    @Override
    public Status getStatus() {
        return status;
    }

    @Override
    public void setStatus(final Status status) {
        this.status = status;
    }

    @Override
    public boolean isAddedByUses() {
        return addedByUses;
    }

    @Override
    public void setAddedByUses(final boolean addedByUses) {
        this.addedByUses = addedByUses;
    }

    @Override
    public DataSchemaNodeBuilder getChildNode(String name) {
        DataSchemaNodeBuilder result = null;
        for (DataSchemaNodeBuilder node : addedChildNodes) {
            if (node.getQName().getLocalName().equals(name)) {
                result = node;
                break;
            }
        }
        return result;
    }

    @Override
    public Set<DataSchemaNode> getChildNodes() {
        return childNodes;
    }

    @Override
    public void addChildNode(final DataSchemaNodeBuilder childNode) {
        addedChildNodes.add(childNode);
    }

    @Override
    public Set<DataSchemaNodeBuilder> getChildNodeBuilders() {
        return addedChildNodes;
    }

    public void setChildNodes(final Set<DataSchemaNode> childNodes) {
        this.childNodes = childNodes;
    }

    @Override
    public Set<GroupingDefinition> getGroupings() {
        return Collections.emptySet();
    }

    @Override
    public Set<GroupingBuilder> getGroupingBuilders() {
        return addedGroupings;
    }

    @Override
    public void addGrouping(final GroupingBuilder grouping) {
        addedGroupings.add(grouping);
    }

    public void setGroupings(final Set<GroupingDefinition> groupings) {
        this.groupings = groupings;
    }

    @Override
    public Set<UsesNodeBuilder> getUses() {
        return addedUsesNodes;
    }

    @Override
    public void addUsesNode(final UsesNodeBuilder usesBuilder) {
        addedUsesNodes.add(usesBuilder);
    }

    public void setUsesnodes(final Set<UsesNode> usesNodes) {
        this.usesNodes = usesNodes;
    }

    @Override
    public List<UnknownSchemaNodeBuilder> getUnknownNodes() {
        return addedUnknownNodes;
    }

    @Override
    public void addUnknownSchemaNode(final UnknownSchemaNodeBuilder unknownNode) {
        addedUnknownNodes.add(unknownNode);
    }

    public void setUnknownNodes(List<UnknownSchemaNode> unknownNodes) {
        this.unknownNodes = unknownNodes;
    }

    private final class GroupingDefinitionImpl implements GroupingDefinition {
        private final QName qname;
        private SchemaPath path;
        private String description;
        private String reference;
        private Status status;
        private boolean addedByUses;
        private Map<QName, DataSchemaNode> childNodes = Collections.emptyMap();
        private Set<GroupingDefinition> groupings = Collections.emptySet();
        private Set<TypeDefinition<?>> typeDefinitions = Collections.emptySet();
        private Set<UsesNode> uses = Collections.emptySet();
        private List<UnknownSchemaNode> unknownNodes = Collections.emptyList();

        private GroupingDefinitionImpl(final QName qname) {
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

        private void setPath(SchemaPath path) {
            this.path = path;
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
        public Status getStatus() {
            return status;
        }

        private void setStatus(Status status) {
            this.status = status;
        }

        @Override
        public boolean isAddedByUses() {
            return addedByUses;
        }

        private void setAddedByUses(final boolean addedByUses) {
            this.addedByUses = addedByUses;
        }

        @Override
        public Set<DataSchemaNode> getChildNodes() {
            final Set<DataSchemaNode> result = new TreeSet<DataSchemaNode>(Comparators.SCHEMA_NODE_COMP);
            result.addAll(childNodes.values());
            return result;
        }

        private void setChildNodes(Map<QName, DataSchemaNode> childNodes) {
            this.childNodes = childNodes;
        }

        @Override
        public Set<GroupingDefinition> getGroupings() {
            return groupings;
        }

        private void setGroupings(Set<GroupingDefinition> groupings) {
            this.groupings = groupings;
        }

        @Override
        public Set<UsesNode> getUses() {
            return uses;
        }

        private void setUses(Set<UsesNode> uses) {
            this.uses = uses;
        }

        @Override
        public Set<TypeDefinition<?>> getTypeDefinitions() {
            return typeDefinitions;
        }

        private void setTypeDefinitions(Set<TypeDefinition<?>> typeDefinitions) {
            this.typeDefinitions = typeDefinitions;
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
            final GroupingDefinitionImpl other = (GroupingDefinitionImpl) obj;
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
            StringBuilder sb = new StringBuilder(GroupingDefinitionImpl.class.getSimpleName());
            sb.append("[");
            sb.append("qname=" + qname);
            sb.append("]");
            return sb.toString();
        }
    }

}
