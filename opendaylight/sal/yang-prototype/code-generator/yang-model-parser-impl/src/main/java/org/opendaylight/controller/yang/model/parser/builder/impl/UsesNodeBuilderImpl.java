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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.opendaylight.controller.yang.common.QName;
import org.opendaylight.controller.yang.model.api.AugmentationSchema;
import org.opendaylight.controller.yang.model.api.SchemaNode;
import org.opendaylight.controller.yang.model.api.SchemaPath;
import org.opendaylight.controller.yang.model.api.UsesNode;
import org.opendaylight.controller.yang.model.parser.builder.api.AugmentationSchemaBuilder;
import org.opendaylight.controller.yang.model.parser.builder.api.Builder;
import org.opendaylight.controller.yang.model.parser.builder.api.UsesNodeBuilder;

public class UsesNodeBuilderImpl implements UsesNodeBuilder, Builder {

    private final UsesNodeImpl instance;
    private final Set<AugmentationSchemaBuilder> addedAugments = new HashSet<AugmentationSchemaBuilder>();

    UsesNodeBuilderImpl(String groupingPathStr) {
        SchemaPath groupingPath = parseUsesPath(groupingPathStr);
        instance = new UsesNodeImpl(groupingPath);
    }

    @Override
    public UsesNode build() {
        // AUGMENTATIONS
        final Set<AugmentationSchema> augments = new HashSet<AugmentationSchema>();
        for (AugmentationSchemaBuilder builder : addedAugments) {
            augments.add(builder.build());
        }
        instance.setAugmentations(augments);

        return instance;
    }

    @Override
    public void addAugment(AugmentationSchemaBuilder augmentBuilder) {
        addedAugments.add(augmentBuilder);
    }

    @Override
    public void setAugmenting(boolean augmenting) {
        instance.setAugmenting(augmenting);
    }

    private SchemaPath parseUsesPath(String augmentPath) {
        String[] splittedPath = augmentPath.split("/");
        List<QName> path = new ArrayList<QName>();
        QName name;
        for (String pathElement : splittedPath) {
            String[] splittedElement = pathElement.split(":");
            if (splittedElement.length == 1) {
                name = new QName(null, null, null, splittedElement[0]);
            } else {
                name = new QName(null, null, splittedElement[0],
                        splittedElement[1]);
            }
            path.add(name);
        }
        final boolean absolute = augmentPath.startsWith("/");
        return new SchemaPath(path, absolute);
    }

    private static class UsesNodeImpl implements UsesNode {

        private final SchemaPath groupingPath;
        private Set<AugmentationSchema> augmentations = Collections.emptySet();
        private boolean augmenting;

        private UsesNodeImpl(SchemaPath groupingPath) {
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

        private void setAugmentations(Set<AugmentationSchema> augmentations) {
            if (augmentations != null) {
                this.augmentations = augmentations;
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
        public Map<SchemaPath, SchemaNode> getRefines() {
            // TODO Auto-generated method stub
            return null;
        }
        
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((groupingPath == null) ? 0 : groupingPath.hashCode());
            result = prime * result + ((augmentations == null) ? 0 : augmentations.hashCode());
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
            UsesNodeImpl other = (UsesNodeImpl) obj;
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
            sb.append("[groupingPath=" + groupingPath +"]");
            return sb.toString();
        }
    }
}
