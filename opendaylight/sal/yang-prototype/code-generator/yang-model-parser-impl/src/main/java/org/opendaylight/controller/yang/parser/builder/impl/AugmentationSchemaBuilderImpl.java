/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.yang.parser.builder.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.opendaylight.controller.yang.common.QName;
import org.opendaylight.controller.yang.model.api.AugmentationSchema;
import org.opendaylight.controller.yang.model.api.DataSchemaNode;
import org.opendaylight.controller.yang.model.api.GroupingDefinition;
import org.opendaylight.controller.yang.model.api.RevisionAwareXPath;
import org.opendaylight.controller.yang.model.api.SchemaPath;
import org.opendaylight.controller.yang.model.api.Status;
import org.opendaylight.controller.yang.model.api.TypeDefinition;
import org.opendaylight.controller.yang.model.api.UsesNode;
import org.opendaylight.controller.yang.model.util.RevisionAwareXPathImpl;
import org.opendaylight.controller.yang.parser.builder.api.AugmentationSchemaBuilder;
import org.opendaylight.controller.yang.parser.builder.api.DataSchemaNodeBuilder;
import org.opendaylight.controller.yang.parser.builder.api.GroupingBuilder;
import org.opendaylight.controller.yang.parser.builder.api.TypeDefinitionBuilder;
import org.opendaylight.controller.yang.parser.builder.api.UsesNodeBuilder;
import org.opendaylight.controller.yang.parser.util.YangModelBuilderUtil;

public class AugmentationSchemaBuilderImpl implements AugmentationSchemaBuilder {
    private final AugmentationSchemaImpl instance;
    private final int line;
    private final String augmentTargetStr;
    private SchemaPath augmentTarget;
    private SchemaPath finalAugmentTarget;
    private String whenCondition;
    private final Set<DataSchemaNodeBuilder> childNodes = new HashSet<DataSchemaNodeBuilder>();
    private final Set<GroupingBuilder> groupings = new HashSet<GroupingBuilder>();
    private final Set<UsesNodeBuilder> usesNodes = new HashSet<UsesNodeBuilder>();
    private boolean resolved;

    AugmentationSchemaBuilderImpl(final String augmentTargetStr, final int line) {
        this.augmentTargetStr = augmentTargetStr;
        this.line = line;
        final SchemaPath targetPath = YangModelBuilderUtil
                .parseAugmentPath(augmentTargetStr);
        augmentTarget = targetPath;
        instance = new AugmentationSchemaImpl(targetPath);
    }

    @Override
    public int getLine() {
        return line;
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

    /**
     * Always returns null.
     */
    @Override
    public SchemaPath getPath() {
        return null;
    }

    @Override
    public AugmentationSchema build() {
        instance.setTargetPath(finalAugmentTarget);

        RevisionAwareXPath whenStmt;
        if (whenCondition == null) {
            whenStmt = null;
        } else {
            whenStmt = new RevisionAwareXPathImpl(whenCondition, false);
        }
        instance.setWhenCondition(whenStmt);

        // CHILD NODES
        final Map<QName, DataSchemaNode> childs = new HashMap<QName, DataSchemaNode>();
        for (DataSchemaNodeBuilder node : childNodes) {
            childs.put(node.getQName(), node.build());
        }
        instance.setChildNodes(childs);

        // GROUPINGS
        final Set<GroupingDefinition> groupingDefinitions = new HashSet<GroupingDefinition>();
        for (GroupingBuilder builder : groupings) {
            groupingDefinitions.add(builder.build());
        }
        instance.setGroupings(groupingDefinitions);

        // USES
        final Set<UsesNode> usesNodeDefinitions = new HashSet<UsesNode>();
        for (UsesNodeBuilder builder : usesNodes) {
            usesNodeDefinitions.add(builder.build());
        }
        instance.setUses(usesNodeDefinitions);

        return instance;
    }

    @Override
    public boolean isResolved() {
        return resolved;
    }

    @Override
    public void setResolved(boolean resolved) {
        this.resolved = resolved;
    }

    public String getWhenCondition() {
        return whenCondition;
    }

    public void addWhenCondition(String whenCondition) {
        this.whenCondition = whenCondition;
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

    @Override
    public void setTargetPath(SchemaPath path) {
        this.finalAugmentTarget = path;
    }

    @Override
    public String getTargetPathAsString() {
        return augmentTargetStr;
    }

    @Override
    public int hashCode() {
        final int prime = 17;
        int result = 1;
        result = prime * result
                + ((augmentTargetStr == null) ? 0 : augmentTargetStr.hashCode());
        result = prime * result
                + ((whenCondition == null) ? 0 : whenCondition.hashCode());
        result = prime * result
                + ((childNodes == null) ? 0 : childNodes.hashCode());
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
        AugmentationSchemaBuilderImpl other = (AugmentationSchemaBuilderImpl) obj;
        if (augmentTargetStr == null) {
            if (other.augmentTargetStr != null) {
                return false;
            }
        } else if (!augmentTargetStr.equals(other.augmentTargetStr)) {
            return false;
        }
        if (whenCondition == null) {
            if (other.whenCondition != null) {
                return false;
            }
        } else if (!whenCondition.equals(other.whenCondition)) {
            return false;
        }
        if (childNodes == null) {
            if (other.childNodes != null) {
                return false;
            }
        } else if (!childNodes.equals(other.childNodes)) {
            return false;
        }
        return true;
    }


    private static class AugmentationSchemaImpl implements AugmentationSchema {
        private SchemaPath targetPath;
        private RevisionAwareXPath whenCondition;
        private Map<QName, DataSchemaNode> childNodes = Collections.emptyMap();
        private Set<GroupingDefinition> groupings = Collections.emptySet();
        private Set<UsesNode> uses = Collections.emptySet();

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

        private void setTargetPath(SchemaPath path) {
            this.targetPath = path;
        }

        @Override
        public RevisionAwareXPath getWhenCondition() {
            return whenCondition;
        }

        private void setWhenCondition(RevisionAwareXPath whenCondition) {
            this.whenCondition = whenCondition;
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
                    + ((whenCondition == null) ? 0 : whenCondition.hashCode());
            result = prime * result
                    + ((childNodes == null) ? 0 : childNodes.hashCode());
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
            if (whenCondition == null) {
                if (other.whenCondition != null) {
                    return false;
                }
            } else if (!whenCondition.equals(other.whenCondition)) {
                return false;
            }
            if (childNodes == null) {
                if (other.childNodes != null) {
                    return false;
                }
            } else if (!childNodes.equals(other.childNodes)) {
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
