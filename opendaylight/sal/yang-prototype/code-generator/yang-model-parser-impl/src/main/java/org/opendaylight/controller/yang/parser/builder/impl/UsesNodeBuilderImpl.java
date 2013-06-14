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

import org.opendaylight.controller.yang.model.api.AugmentationSchema;
import org.opendaylight.controller.yang.model.api.SchemaNode;
import org.opendaylight.controller.yang.model.api.SchemaPath;
import org.opendaylight.controller.yang.model.api.UsesNode;
import org.opendaylight.controller.yang.parser.builder.api.AugmentationSchemaBuilder;
import org.opendaylight.controller.yang.parser.builder.api.SchemaNodeBuilder;
import org.opendaylight.controller.yang.parser.builder.api.UsesNodeBuilder;
import org.opendaylight.controller.yang.parser.util.RefineHolder;

public final class UsesNodeBuilderImpl implements UsesNodeBuilder {
    private boolean isBuilt;
    private UsesNodeImpl instance;
    private final int line;
    private SchemaPath schemaPath;
    private final String groupingName;
    private SchemaPath groupingPath;
    private boolean augmenting;
    private final Set<AugmentationSchemaBuilder> addedAugments = new HashSet<AugmentationSchemaBuilder>();
    private List<SchemaNodeBuilder> refineBuilders = new ArrayList<SchemaNodeBuilder>();
    private List<RefineHolder> refines = new ArrayList<RefineHolder>();

    public UsesNodeBuilderImpl(final String groupingName, final int line) {
        this.groupingName = groupingName;
        this.line = line;
    }

    @Override
    public UsesNode build() {
        if (!isBuilt) {
            instance = new UsesNodeImpl(groupingPath);
            instance.setAugmenting(augmenting);

            // AUGMENTATIONS
            final Set<AugmentationSchema> augments = new HashSet<AugmentationSchema>();
            for (AugmentationSchemaBuilder builder : addedAugments) {
                augments.add(builder.build());
            }
            instance.setAugmentations(augments);

            // REFINES
            final Map<SchemaPath, SchemaNode> refineNodes = new HashMap<SchemaPath, SchemaNode>();
            for (SchemaNodeBuilder refineBuilder : refineBuilders) {
                SchemaNode refineNode = refineBuilder.build();
                refineNodes.put(refineNode.getPath(), refineNode);
            }
            instance.setRefines(refineNodes);

            isBuilt = true;
        }
        return instance;
    }

    @Override
    public int getLine() {
        return line;
    }

    @Override
    public void setGroupingPath(SchemaPath groupingPath) {
        this.groupingPath = groupingPath;
    }

    @Override
    public SchemaPath getPath() {
        return schemaPath;
    }

    @Override
    public void setPath(SchemaPath path) {
        this.schemaPath = path;
    }

    @Override
    public String getGroupingName() {
        return groupingName;
    }

    @Override
    public Set<AugmentationSchemaBuilder> getAugmentations() {
        return addedAugments;
    }

    @Override
    public void addAugment(final AugmentationSchemaBuilder augmentBuilder) {
        addedAugments.add(augmentBuilder);
    }

    @Override
    public boolean isAugmenting() {
        return augmenting;
    }

    @Override
    public void setAugmenting(final boolean augmenting) {
        this.augmenting = augmenting;
    }

    @Override
    public List<SchemaNodeBuilder> getRefineNodes() {
        return refineBuilders;
    }

    @Override
    public void addRefineNode(SchemaNodeBuilder refineNode) {
        refineBuilders.add(refineNode);
    }

    @Override
    public List<RefineHolder> getRefines() {
        return refines;
    }

    @Override
    public void addRefine(RefineHolder refine) {
        refines.add(refine);
    }


    private final class UsesNodeImpl implements UsesNode {
        private final SchemaPath groupingPath;
        private Set<AugmentationSchema> augmentations = Collections.emptySet();
        private boolean augmenting;
        private Map<SchemaPath, SchemaNode> refines = Collections.emptyMap();

        private UsesNodeImpl(final SchemaPath groupingPath) {
            this.groupingPath = groupingPath;
        }

        @Override
        public SchemaPath getGroupingPath() {
            return groupingPath;
        }

        @Override
        public Set<AugmentationSchema> getAugmentations() {
            return augmentations;
        }

        private void setAugmentations(
                final Set<AugmentationSchema> augmentations) {
            if (augmentations != null) {
                this.augmentations = augmentations;
            }
        }

        @Override
        public boolean isAugmenting() {
            return augmenting;
        }

        private void setAugmenting(final boolean augmenting) {
            this.augmenting = augmenting;
        }

        @Override
        public Map<SchemaPath, SchemaNode> getRefines() {
            return refines;
        }

        private void setRefines(Map<SchemaPath, SchemaNode> refines) {
            if (refines != null) {
                this.refines = refines;
            }
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result
                    + ((groupingPath == null) ? 0 : groupingPath.hashCode());
            result = prime * result
                    + ((augmentations == null) ? 0 : augmentations.hashCode());
            result = prime * result + (augmenting ? 1231 : 1237);
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
            final UsesNodeImpl other = (UsesNodeImpl) obj;
            if (groupingPath == null) {
                if (other.groupingPath != null) {
                    return false;
                }
            } else if (!groupingPath.equals(other.groupingPath)) {
                return false;
            }
            if (augmentations == null) {
                if (other.augmentations != null) {
                    return false;
                }
            } else if (!augmentations.equals(other.augmentations)) {
                return false;
            }
            if (augmenting != other.augmenting) {
                return false;
            }
            return true;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder(
                    UsesNodeImpl.class.getSimpleName());
            sb.append("[groupingPath=" + groupingPath + "]");
            return sb.toString();
        }
    }
}
