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
import org.opendaylight.controller.yang.model.api.NotificationDefinition;
import org.opendaylight.controller.yang.model.api.SchemaNode;
import org.opendaylight.controller.yang.model.api.SchemaPath;
import org.opendaylight.controller.yang.model.api.Status;
import org.opendaylight.controller.yang.model.api.TypeDefinition;
import org.opendaylight.controller.yang.model.api.UnknownSchemaNode;
import org.opendaylight.controller.yang.model.api.UsesNode;
import org.opendaylight.controller.yang.model.parser.builder.api.AbstractChildNodeBuilder;
import org.opendaylight.controller.yang.model.parser.builder.api.DataSchemaNodeBuilder;
import org.opendaylight.controller.yang.model.parser.builder.api.GroupingBuilder;
import org.opendaylight.controller.yang.model.parser.builder.api.SchemaNodeBuilder;
import org.opendaylight.controller.yang.model.parser.builder.api.TypeDefinitionAwareBuilder;
import org.opendaylight.controller.yang.model.parser.builder.api.TypeDefinitionBuilder;
import org.opendaylight.controller.yang.model.parser.builder.api.UsesNodeBuilder;

public class NotificationBuilder extends AbstractChildNodeBuilder implements
        TypeDefinitionAwareBuilder, SchemaNodeBuilder {
    private final NotificationDefinitionImpl instance;
    private final int line;
    private SchemaPath schemaPath;
    private final Set<TypeDefinitionBuilder> addedTypedefs = new HashSet<TypeDefinitionBuilder>();
    private final Set<UsesNodeBuilder> addedUsesNodes = new HashSet<UsesNodeBuilder>();
    private final List<UnknownSchemaNodeBuilder> addedUnknownNodes = new ArrayList<UnknownSchemaNodeBuilder>();

    NotificationBuilder(final QName qname, final int line) {
        super(qname);
        this.line = line;
        instance = new NotificationDefinitionImpl(qname);
    }

    @Override
    public SchemaNode build() {
        instance.setPath(schemaPath);

        // CHILD NODES
        final Map<QName, DataSchemaNode> childs = new HashMap<QName, DataSchemaNode>();
        for (DataSchemaNodeBuilder node : childNodes) {
            childs.put(node.getQName(), node.build());
        }
        instance.setChildNodes(childs);

        // GROUPINGS
        final Set<GroupingDefinition> groupingDefs = new HashSet<GroupingDefinition>();
        for (GroupingBuilder builder : groupings) {
            groupingDefs.add(builder.build());
        }
        instance.setGroupings(groupingDefs);

        // TYPEDEFS
        final Set<TypeDefinition<?>> typedefs = new HashSet<TypeDefinition<?>>();
        for (TypeDefinitionBuilder entry : addedTypedefs) {
            typedefs.add(entry.build());
        }
        instance.setTypeDefinitions(typedefs);

        // USES
        final Set<UsesNode> uses = new HashSet<UsesNode>();
        for (UsesNodeBuilder builder : addedUsesNodes) {
            uses.add(builder.build());
        }
        instance.setUses(uses);

        // UNKNOWN NODES
        final List<UnknownSchemaNode> unknownNodes = new ArrayList<UnknownSchemaNode>();
        for (UnknownSchemaNodeBuilder b : addedUnknownNodes) {
            unknownNodes.add(b.build());
        }
        instance.setUnknownSchemaNodes(unknownNodes);

        return instance;
    }

    @Override
    public int getLine() {
        return line;
    }

    @Override
    public void addTypedef(final TypeDefinitionBuilder type) {
        addedTypedefs.add(type);
    }

    @Override
    public void addUsesNode(final UsesNodeBuilder usesNodeBuilder) {
        addedUsesNodes.add(usesNodeBuilder);
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
    public void setDescription(final String description) {
        instance.setDescription(description);
    }

    @Override
    public void setReference(final String reference) {
        instance.setReference(reference);
    }

    @Override
    public void setStatus(final Status status) {
        instance.setStatus(status);
    }

    @Override
    public void addUnknownSchemaNode(final UnknownSchemaNodeBuilder unknownNode) {
        addedUnknownNodes.add(unknownNode);
    }

    private class NotificationDefinitionImpl implements NotificationDefinition {
        private final QName qname;
        private SchemaPath path;
        private String description;
        private String reference;
        private Status status = Status.CURRENT;
        private Map<QName, DataSchemaNode> childNodes = Collections.emptyMap();
        private Set<GroupingDefinition> groupings = Collections.emptySet();
        private Set<TypeDefinition<?>> typeDefinitions = Collections.emptySet();
        private Set<UsesNode> uses = Collections.emptySet();
        private List<UnknownSchemaNode> unknownNodes = Collections.emptyList();

        private NotificationDefinitionImpl(final QName qname) {
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

        private void setReference(String reference) {
            this.reference = reference;
        }

        @Override
        public Status getStatus() {
            return status;
        }

        private void setStatus(Status status) {
            if (status != null) {
                this.status = status;
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
        public Set<TypeDefinition<?>> getTypeDefinitions() {
            return typeDefinitions;
        }

        private void setTypeDefinitions(
                final Set<TypeDefinition<?>> typeDefinitions) {
            if (typeDefinitions != null) {
                this.typeDefinitions = typeDefinitions;
            }
        }

        @Override
        public List<UnknownSchemaNode> getUnknownSchemaNodes() {
            return unknownNodes;
        }

        private void setUnknownSchemaNodes(
                final List<UnknownSchemaNode> unknownNodes) {
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
            final NotificationDefinitionImpl other = (NotificationDefinitionImpl) obj;
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
                    NotificationDefinitionImpl.class.getSimpleName());
            sb.append("[qname=" + qname + ", path=" + path + "]");
            return sb.toString();
        }
    }

}
