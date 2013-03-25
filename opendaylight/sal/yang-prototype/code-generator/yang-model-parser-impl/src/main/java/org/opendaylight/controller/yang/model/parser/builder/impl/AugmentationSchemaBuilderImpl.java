/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.yang.model.parser.builder.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.opendaylight.controller.yang.common.QName;
import org.opendaylight.controller.yang.model.api.AugmentationSchema;
import org.opendaylight.controller.yang.model.api.DataSchemaNode;
import org.opendaylight.controller.yang.model.api.GroupingDefinition;
import org.opendaylight.controller.yang.model.api.SchemaPath;
import org.opendaylight.controller.yang.model.api.Status;
import org.opendaylight.controller.yang.model.api.TypeDefinition;
import org.opendaylight.controller.yang.model.api.UsesNode;
import org.opendaylight.controller.yang.model.parser.builder.api.AugmentationSchemaBuilder;
import org.opendaylight.controller.yang.model.parser.builder.api.DataSchemaNodeBuilder;
import org.opendaylight.controller.yang.model.parser.builder.api.GroupingBuilder;
import org.opendaylight.controller.yang.model.parser.builder.api.TypeDefinitionBuilder;
import org.opendaylight.controller.yang.model.parser.builder.api.UsesNodeBuilder;
import org.opendaylight.controller.yang.model.parser.util.YangModelBuilderUtil;

public class AugmentationSchemaBuilderImpl implements AugmentationSchemaBuilder {

    private final AugmentationSchemaImpl instance;
    private final SchemaPath augmentTarget;
    final Set<DataSchemaNodeBuilder> childNodes = new HashSet<DataSchemaNodeBuilder>();
    final Set<GroupingBuilder> groupings = new HashSet<GroupingBuilder>();
    private final Set<UsesNodeBuilder> usesNodes = new HashSet<UsesNodeBuilder>();

    AugmentationSchemaBuilderImpl(String augmentPath) {
        SchemaPath targetPath = YangModelBuilderUtil.parseAugmentPath(augmentPath);
        augmentTarget = targetPath;
        instance = new AugmentationSchemaImpl(targetPath);
    }

    @Override
    public void addChildNode(DataSchemaNodeBuilder childNode) {
        childNodes.add(childNode);
    }

    @Override
    public Set<DataSchemaNodeBuilder> getChildNodes() {
        return childNodes;
    }

    @Override
    public void addGrouping(GroupingBuilder grouping) {
        groupings.add(grouping);
    }

    @Override
    public void addUsesNode(UsesNodeBuilder usesBuilder) {
        usesNodes.add(usesBuilder);
    }

    /**
     * Always returns null.
     */
    @Override
    public QName getQName() {
        return null;
    }

    @Override
    public AugmentationSchema build() {

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

        // USES
        Set<UsesNode> usesNodeDefinitions = new HashSet<UsesNode>();
        for (UsesNodeBuilder builder : usesNodes) {
            usesNodeDefinitions.add(builder.build());
        }
        instance.setUses(usesNodeDefinitions);

        return instance;
    }

    @Override
    public void addTypedef(TypeDefinitionBuilder type) {
        throw new UnsupportedOperationException(
                "Augmentation can not contains type definitions");
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
    public SchemaPath getTargetPath() {
        return augmentTarget;
    }

    private static class AugmentationSchemaImpl implements AugmentationSchema {

        private final SchemaPath targetPath;
        private Map<QName, DataSchemaNode> childNodes;
        private Set<GroupingDefinition> groupings;
        private Set<UsesNode> uses;

        private String description;
        private String reference;
        private Status status;

        private AugmentationSchemaImpl(SchemaPath targetPath) {
            this.targetPath = targetPath;
        }

        @Override
        public SchemaPath getTargetPath() {
            return targetPath;
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

        /**
         * Always returns an empty set, because augmentation can not contains
         * type definitions.
         */
        @Override
        public Set<TypeDefinition<?>> getTypeDefinitions() {
            return Collections.emptySet();
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
            final int prime = 17;
            int result = 1;
            result = prime * result
                    + ((targetPath == null) ? 0 : targetPath.hashCode());
            result = prime * result
                    + ((childNodes == null) ? 0 : childNodes.hashCode());
            result = prime * result
                    + ((groupings == null) ? 0 : groupings.hashCode());
            result = prime * result + ((uses == null) ? 0 : uses.hashCode());
            result = prime * result
                    + ((description == null) ? 0 : description.hashCode());
            result = prime * result
                    + ((reference == null) ? 0 : reference.hashCode());
            result = prime * result
                    + ((status == null) ? 0 : status.hashCode());
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
            AugmentationSchemaImpl other = (AugmentationSchemaImpl) obj;
            if (targetPath == null) {
                if (other.targetPath != null) {
                    return false;
                }
            } else if (!targetPath.equals(other.targetPath)) {
                return false;
            }
            if (childNodes == null) {
                if (other.childNodes != null) {
                    return false;
                }
            } else if (!childNodes.equals(other.childNodes)) {
                return false;
            }
            if (groupings == null) {
                if (other.groupings != null) {
                    return false;
                }
            } else if (!groupings.equals(other.groupings)) {
                return false;
            }
            if (uses == null) {
                if (other.uses != null) {
                    return false;
                }
            } else if (!uses.equals(other.uses)) {
                return false;
            }
            if (description == null) {
                if (other.description != null) {
                    return false;
                }
            } else if (!description.equals(other.description)) {
                return false;
            }
            if (reference == null) {
                if (other.reference != null) {
                    return false;
                }
            } else if (!reference.equals(other.reference)) {
                return false;
            }
            if (status == null) {
                if (other.status != null) {
                    return false;
                }
            } else if (!status.equals(other.status)) {
                return false;
            }
            return true;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder(
                    AugmentationSchemaImpl.class.getSimpleName());
            sb.append("[");
            sb.append("targetPath=" + targetPath);
            sb.append(", childNodes=" + childNodes.values());
            sb.append(", groupings=" + groupings);
            sb.append(", uses=" + uses);
            sb.append("]");
            return sb.toString();
        }
    }

}
