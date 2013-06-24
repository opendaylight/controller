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
import org.opendaylight.controller.yang.parser.builder.api.TypeDefinitionAwareBuilder;
import org.opendaylight.controller.yang.parser.builder.api.TypeDefinitionBuilder;
import org.opendaylight.controller.yang.parser.builder.api.UsesNodeBuilder;

public final class ListSchemaNodeBuilder extends AbstractDataNodeContainerBuilder
        implements DataSchemaNodeBuilder, AugmentationTargetBuilder,
        TypeDefinitionAwareBuilder {
    private boolean isBuilt;
    private final ListSchemaNodeImpl instance;
    private final int line;
    // SchemaNode args
    private SchemaPath schemaPath;
    private String description;
    private String reference;
    private Status status = Status.CURRENT;
    private List<UnknownSchemaNode> unknownNodes;
    private final List<UnknownSchemaNodeBuilder> addedUnknownNodes = new ArrayList<UnknownSchemaNodeBuilder>();
    // DataSchemaNode args
    private boolean augmenting;
    private boolean configuration;
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

    public ListSchemaNodeBuilder(final QName qname, final int line) {
        super(qname);
        this.line = line;
        instance = new ListSchemaNodeImpl(qname);
        constraints = new ConstraintsBuilder(line);
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
            instance.setConfiguration(configuration);
            instance.setUserOrdered(userOrdered);

            // CHILD NODES
            final Map<QName, DataSchemaNode> childs = new HashMap<QName, DataSchemaNode>();
            if(childNodes == null) {
                for (DataSchemaNodeBuilder node : addedChildNodes) {
                    childs.put(node.getQName(), node.build());
                }
            } else {
                for(DataSchemaNode node : childNodes) {
                    childs.put(node.getQName(), node);
                }
            }
            instance.setChildNodes(childs);

            // TYPEDEFS
            if(typedefs == null) {
                typedefs = new HashSet<TypeDefinition<?>>();
                for (TypeDefinitionBuilder entry : addedTypedefs) {
                    typedefs.add(entry.build());
                }
            }
            instance.setTypeDefinitions(typedefs);

            // USES
            if(usesNodes == null) {
                usesNodes = new HashSet<UsesNode>();
                for (UsesNodeBuilder builder : addedUsesNodes) {
                    usesNodes.add(builder.build());
                }
            }
            instance.setUses(usesNodes);

            // GROUPINGS
            if(groupings == null) {
                groupings = new HashSet<GroupingDefinition>();
                for (GroupingBuilder builder : addedGroupings) {
                    groupings.add(builder.build());
                }
            }
            instance.setGroupings(groupings);

            // AUGMENTATIONS
            if(augmentations == null) {
                augmentations = new HashSet<AugmentationSchema>();
                for (AugmentationSchemaBuilder builder : addedAugmentations) {
                    augmentations.add(builder.build());
                }
            }
            instance.setAvailableAugmentations(augmentations);

            // UNKNOWN NODES
            if(unknownNodes == null) {
                unknownNodes = new ArrayList<UnknownSchemaNode>();
                for (UnknownSchemaNodeBuilder b : addedUnknownNodes) {
                    unknownNodes.add(b.build());
                }
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
    public int getLine() {
        return line;
    }

    @Override
    public Set<TypeDefinitionBuilder> getTypeDefinitions() {
        return addedTypedefs;
    }

    @Override
    public void addTypedef(final TypeDefinitionBuilder type) {
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

    public boolean isConfiguration() {
        return configuration;
    }

    @Override
    public void setConfiguration(boolean configuration) {
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


    public final class ListSchemaNodeImpl implements ListSchemaNode {
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
            StringBuilder sb = new StringBuilder(
                    ListSchemaNodeImpl.class.getSimpleName());
            sb.append("[");
            sb.append("qname=" + qname);
            sb.append(", path=" + path);
            sb.append(", keyDefinition=" + keyDefinition);
            sb.append("]");
            return sb.toString();
        }
    }

}
