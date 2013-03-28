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
import org.opendaylight.controller.yang.model.parser.builder.api.AbstractChildNodeBuilder;
import org.opendaylight.controller.yang.model.parser.builder.api.AugmentationTargetBuilder;
import org.opendaylight.controller.yang.model.parser.builder.api.DataSchemaNodeBuilder;
import org.opendaylight.controller.yang.model.parser.builder.api.GroupingBuilder;
import org.opendaylight.controller.yang.model.parser.builder.api.SchemaNodeBuilder;
import org.opendaylight.controller.yang.model.parser.builder.api.TypeDefinitionAwareBuilder;
import org.opendaylight.controller.yang.model.parser.builder.api.TypeDefinitionBuilder;
import org.opendaylight.controller.yang.model.parser.builder.api.UsesNodeBuilder;

public class ListSchemaNodeBuilder extends AbstractChildNodeBuilder implements
        DataSchemaNodeBuilder, SchemaNodeBuilder, AugmentationTargetBuilder,
        TypeDefinitionAwareBuilder {

    private final ListSchemaNodeImpl instance;
    private final ConstraintsBuilder constraintsBuilder;

    private final Set<TypeDefinitionBuilder> addedTypedefs = new HashSet<TypeDefinitionBuilder>();
    private final Set<AugmentationSchema> augmentations = new HashSet<AugmentationSchema>();
    private final Set<UsesNodeBuilder> usesNodes = new HashSet<UsesNodeBuilder>();
    private final List<UnknownSchemaNodeBuilder> addedUnknownNodes = new ArrayList<UnknownSchemaNodeBuilder>();

    ListSchemaNodeBuilder(QName qname) {
        super(qname);
        instance = new ListSchemaNodeImpl(qname);
        constraintsBuilder = new ConstraintsBuilder();
    }

    @Override
    public ListSchemaNode build() {
        // CHILD NODES
        Map<QName, DataSchemaNode> childs = new HashMap<QName, DataSchemaNode>();
        for (DataSchemaNodeBuilder node : childNodes) {
            childs.put(node.getQName(), node.build());
        }
        instance.setChildNodes(childs);

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

        // GROUPINGS
        Set<GroupingDefinition> groupingDefinitions = new HashSet<GroupingDefinition>();
        for (GroupingBuilder builder : groupings) {
            groupingDefinitions.add(builder.build());
        }
        instance.setGroupings(groupingDefinitions);

        // UNKNOWN NODES
        final List<UnknownSchemaNode> unknownNodes = new ArrayList<UnknownSchemaNode>();
        for(UnknownSchemaNodeBuilder b : addedUnknownNodes) {
            unknownNodes.add(b.build());
        }
        instance.setUnknownSchemaNodes(unknownNodes);

        instance.setConstraints(constraintsBuilder.build());
        instance.setAvailableAugmentations(augmentations);

        return instance;
    }

    @Override
    public void addTypedef(TypeDefinitionBuilder type) {
        addedTypedefs.add(type);
    }

    @Override
    public void setPath(SchemaPath path) {
        instance.setPath(path);
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
        if(status != null) {
            instance.setStatus(status);
        }
    }

    @Override
    public void addUsesNode(UsesNodeBuilder usesBuilder) {
        usesNodes.add(usesBuilder);
    }

    @Override
    public void addAugmentation(AugmentationSchema augmentationSchema) {
        augmentations.add(augmentationSchema);
    }

    public void setKeyDefinition(List<QName> keyDefinition) {
        instance.setKeyDefinition(keyDefinition);
    }

    @Override
    public void setAugmenting(boolean augmenting) {
        instance.setAugmenting(augmenting);
    }

    @Override
    public void setConfiguration(boolean configuration) {
        instance.setConfiguration(configuration);
    }

    @Override
    public ConstraintsBuilder getConstraintsBuilder() {
        return constraintsBuilder;
    }

    public void setUserOrdered(boolean userOrdered) {
        instance.setUserOrdered(userOrdered);
    }

    @Override
    public void addUnknownSchemaNode(UnknownSchemaNodeBuilder unknownSchemaNodeBuilder) {
        addedUnknownNodes.add(unknownSchemaNodeBuilder);
    }

    private class ListSchemaNodeImpl implements ListSchemaNode {
        private final QName qname;
        private SchemaPath path;
        private String description;
        private String reference;
        private Status status = Status.CURRENT;
        private List<QName> keyDefinition = Collections.emptyList();
        private boolean augmenting;
        private boolean configuration;
        private ConstraintDefinition constraints;
        private Set<AugmentationSchema> augmentations = Collections.emptySet();
        private Map<QName, DataSchemaNode> childNodes = Collections.emptyMap();
        private Set<TypeDefinition<?>> typeDefinitions = Collections.emptySet();
        private Set<GroupingDefinition> groupings = Collections.emptySet();
        private Set<UsesNode> uses = Collections.emptySet();
        private boolean userOrdered;
        private List<UnknownSchemaNode> unknownSchemaNodes = Collections.emptyList();

        private ListSchemaNodeImpl(QName qname) {
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
        public List<QName> getKeyDefinition() {
            if(keyDefinition == null) {
                return Collections.emptyList();
            } else {
                return keyDefinition;
            }
        }

        private void setKeyDefinition(List<QName> keyDefinition) {
            if(keyDefinition != null) {
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

        private void setAvailableAugmentations(
                Set<AugmentationSchema> augmentations) {
            if(augmentations != null) {
                this.augmentations = augmentations;
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
        public Set<TypeDefinition<?>> getTypeDefinitions() {
            return typeDefinitions;
        }

        private void setTypeDefinitions(Set<TypeDefinition<?>> typeDefinitions) {
            if(typeDefinitions != null) {
                this.typeDefinitions = typeDefinitions;
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
            return unknownSchemaNodes;
        }

        private void setUnknownSchemaNodes(List<UnknownSchemaNode> unknownSchemaNodes) {
            if(unknownSchemaNodes != null) {
                this.unknownSchemaNodes = unknownSchemaNodes;
            }
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
            ListSchemaNodeImpl other = (ListSchemaNodeImpl) obj;
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
                    ListSchemaNodeImpl.class.getSimpleName());
            sb.append("[");
            sb.append("qname=" + qname);
            sb.append(", path=" + path);
            sb.append(", description=" + description);
            sb.append(", reference=" + reference);
            sb.append(", status=" + status);
            sb.append(", keyDefinition=" + keyDefinition);
            sb.append(", augmenting=" + augmenting);
            sb.append(", configuration=" + configuration);
            sb.append(", constraints=" + constraints);
            sb.append(", augmentations=" + augmentations);
            sb.append(", childNodes=" + childNodes.values());
            sb.append(", typedefinitions=" + typeDefinitions);
            sb.append(", groupings=" + groupings);
            sb.append(", uses=" + uses);
            sb.append(", userOrdered=" + userOrdered);
            sb.append("]");
            return sb.toString();
        }
    }

}
