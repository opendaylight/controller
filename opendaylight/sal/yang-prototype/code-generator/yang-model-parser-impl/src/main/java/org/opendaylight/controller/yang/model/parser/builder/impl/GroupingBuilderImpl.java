/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.yang.model.parser.builder.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.opendaylight.controller.yang.common.QName;
import org.opendaylight.controller.yang.model.api.DataSchemaNode;
import org.opendaylight.controller.yang.model.api.GroupingDefinition;
import org.opendaylight.controller.yang.model.api.SchemaPath;
import org.opendaylight.controller.yang.model.api.Status;
import org.opendaylight.controller.yang.model.api.TypeDefinition;
import org.opendaylight.controller.yang.model.api.UnknownSchemaNode;
import org.opendaylight.controller.yang.model.api.UsesNode;
import org.opendaylight.controller.yang.model.parser.builder.api.DataSchemaNodeBuilder;
import org.opendaylight.controller.yang.model.parser.builder.api.GroupingBuilder;
import org.opendaylight.controller.yang.model.parser.builder.api.TypeDefinitionBuilder;
import org.opendaylight.controller.yang.model.parser.builder.api.UsesNodeBuilder;

public class GroupingBuilderImpl implements GroupingBuilder {

    private final GroupingDefinitionImpl instance;
    private final Set<DataSchemaNodeBuilder> childNodes = new HashSet<DataSchemaNodeBuilder>();
    private final Set<GroupingBuilder> groupings = new HashSet<GroupingBuilder>();
    private final Set<TypeDefinitionBuilder> addedTypedefs = new HashSet<TypeDefinitionBuilder>();
    private final Set<UsesNodeBuilder> usesNodes = new HashSet<UsesNodeBuilder>();
    private final List<UnknownSchemaNodeBuilder> addedUnknownNodes = new ArrayList<UnknownSchemaNodeBuilder>();

    GroupingBuilderImpl(QName qname) {
        this.instance = new GroupingDefinitionImpl(qname);
    }

    @Override
    public GroupingDefinition build() {
        // CHILD NODES
        Map<QName, DataSchemaNode> childs = new HashMap<QName, DataSchemaNode>();
        for (DataSchemaNodeBuilder node : childNodes) {
            childs.put(node.getQName(), node.build());
        }
        instance.setChildNodes(childs);

        // GROUPINGS
        Set<GroupingDefinition> groupingDefinitions = new HashSet<GroupingDefinition>();
        for (GroupingBuilder builder : groupings) {
            groupingDefinitions.add(builder.build());
        }
        instance.setGroupings(groupingDefinitions);

        // TYPEDEFS
        Set<TypeDefinition<?>> typedefs = new HashSet<TypeDefinition<?>>();
        for (TypeDefinitionBuilder entry : addedTypedefs) {
            typedefs.add(entry.build());
        }
        instance.setTypeDefinitions(typedefs);

        // USES
        Set<UsesNode> usesNodeDefinitions = new HashSet<UsesNode>();
        for (UsesNodeBuilder builder : usesNodes) {
            usesNodeDefinitions.add(builder.build());
        }
        instance.setUses(usesNodeDefinitions);

        // UNKNOWN NODES
        final List<UnknownSchemaNode> unknownNodes = new ArrayList<UnknownSchemaNode>();
        for(UnknownSchemaNodeBuilder b : addedUnknownNodes) {
            unknownNodes.add(b.build());
        }
        instance.setUnknownSchemaNodes(unknownNodes);

        return instance;
    }

    /**
     * Always returns null.
     */
    @Override
    public QName getQName() {
        return null;
    }

    @Override
    public void addTypedef(TypeDefinitionBuilder type) {
        addedTypedefs.add(type);
    }

    @Override
    public void setPath(SchemaPath schemaPath) {
        instance.setPath(schemaPath);
    }

    @Override
    public void setDescription(String description) {
        instance.setDescription(description);
    }

    @Override
    public void setReference(String reference) {
        instance.setReference(reference);
    }

    @Override
    public void setStatus(Status status) {
        instance.setStatus(status);
    }

    @Override
    public void addChildNode(DataSchemaNodeBuilder childNode) {
        childNodes.add(childNode);
    }

    @Override
    public void addGrouping(GroupingBuilder grouping) {
        groupings.add(grouping);
    }

    @Override
    public void addUsesNode(UsesNodeBuilder usesBuilder) {
        usesNodes.add(usesBuilder);
    }

    @Override
    public void addUnknownSchemaNode(UnknownSchemaNodeBuilder unknownSchemaNodeBuilder) {
        addedUnknownNodes.add(unknownSchemaNodeBuilder);
    }

    private static class GroupingDefinitionImpl implements GroupingDefinition {
        private final QName qname;
        private SchemaPath path;
        private String description;
        private String reference;
        private Status status;
        private Map<QName, DataSchemaNode> childNodes = Collections.emptyMap();
        private Set<GroupingDefinition> groupings = Collections.emptySet();
        private Set<TypeDefinition<?>> typeDefinitions = Collections.emptySet();
        private Set<UsesNode> uses = Collections.emptySet();
        private List<UnknownSchemaNode> unknownSchemaNodes = Collections.emptyList();

        private GroupingDefinitionImpl(QName qname) {
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
        public Set<DataSchemaNode> getChildNodes() {
            return new HashSet<DataSchemaNode>(childNodes.values());
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
            return unknownSchemaNodes;
        }

        private void setUnknownSchemaNodes(List<UnknownSchemaNode> unknownSchemaNodes) {
            if(unknownSchemaNodes != null) {
                this.unknownSchemaNodes = unknownSchemaNodes;
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
            GroupingDefinitionImpl other = (GroupingDefinitionImpl) obj;
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
            StringBuilder sb = new StringBuilder(
                    GroupingDefinitionImpl.class.getSimpleName());
            sb.append("[");
            sb.append("qname=" + qname);
            sb.append("]");
            return sb.toString();
        }
    }

}
