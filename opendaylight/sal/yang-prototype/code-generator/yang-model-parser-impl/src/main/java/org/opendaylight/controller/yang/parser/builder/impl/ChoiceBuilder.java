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
import java.util.Set;

import org.opendaylight.controller.yang.common.QName;
import org.opendaylight.controller.yang.model.api.AugmentationSchema;
import org.opendaylight.controller.yang.model.api.ChoiceCaseNode;
import org.opendaylight.controller.yang.model.api.ChoiceNode;
import org.opendaylight.controller.yang.model.api.ConstraintDefinition;
import org.opendaylight.controller.yang.model.api.SchemaPath;
import org.opendaylight.controller.yang.model.api.Status;
import org.opendaylight.controller.yang.model.api.UnknownSchemaNode;
import org.opendaylight.controller.yang.parser.builder.api.AugmentationSchemaBuilder;
import org.opendaylight.controller.yang.parser.builder.api.AugmentationTargetBuilder;
import org.opendaylight.controller.yang.parser.builder.api.DataSchemaNodeBuilder;

public final class ChoiceBuilder implements DataSchemaNodeBuilder, AugmentationTargetBuilder {
    private boolean built;
    private final ChoiceNodeImpl instance;
    private final int line;
    // SchemaNode args
    private final QName qname;
    private SchemaPath schemaPath;
    private String description;
    private String reference;
    private Status status = Status.CURRENT;
    private final List<UnknownSchemaNodeBuilder> addedUnknownNodes = new ArrayList<UnknownSchemaNodeBuilder>();
    // DataSchemaNode args
    private boolean augmenting;
    private boolean configuration;
    private final ConstraintsBuilder constraints;
    // AugmentationTarget args
    private final Set<AugmentationSchemaBuilder> addedAugmentations = new HashSet<AugmentationSchemaBuilder>();
    // ChoiceNode args
    private final Set<ChoiceCaseBuilder> cases = new HashSet<ChoiceCaseBuilder>();
    private String defaultCase;

    public ChoiceBuilder(final QName qname, final int line) {
        this.qname = qname;
        this.line = line;
        instance = new ChoiceNodeImpl(qname);
        constraints = new ConstraintsBuilder(line);
    }

    @Override
    public ChoiceNode build() {
        if (!built) {
            instance.setPath(schemaPath);
            instance.setDescription(description);
            instance.setReference(reference);
            instance.setStatus(status);
            instance.setAugmenting(augmenting);
            instance.setConfiguration(configuration);
            instance.setConstraints(constraints.build());
            instance.setDefaultCase(defaultCase);

            // CASES
            final Set<ChoiceCaseNode> choiceCases = new HashSet<ChoiceCaseNode>();
            for (ChoiceCaseBuilder caseBuilder : cases) {
                choiceCases.add(caseBuilder.build());
            }
            instance.setCases(choiceCases);

            // AUGMENTATIONS
            final Set<AugmentationSchema> augmentations = new HashSet<AugmentationSchema>();
            for (AugmentationSchemaBuilder builder : addedAugmentations) {
                augmentations.add(builder.build());
            }
            instance.setAvailableAugmentations(augmentations);

            // UNKNOWN NODES
            final List<UnknownSchemaNode> unknownNodes = new ArrayList<UnknownSchemaNode>();
            for (UnknownSchemaNodeBuilder b : addedUnknownNodes) {
                unknownNodes.add(b.build());
            }
            instance.setUnknownSchemaNodes(unknownNodes);

            built = true;
        }
        return instance;
    }

    @Override
    public int getLine() {
        return line;
    }

    public Set<ChoiceCaseBuilder> getCases() {
        return cases;
    }

    public void addChildNode(DataSchemaNodeBuilder childNode) {
        if (!(childNode instanceof ChoiceCaseBuilder)) {
            ChoiceCaseBuilder caseBuilder = new ChoiceCaseBuilder(childNode.getQName(), childNode.getLine());
            caseBuilder.addChildNode(childNode);
            cases.add(caseBuilder);
        } else {
            cases.add((ChoiceCaseBuilder) childNode);
        }
    }

    @Override
    public QName getQName() {
        return qname;
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

    public List<UnknownSchemaNodeBuilder> getUnknownNodes() {
        return addedUnknownNodes;
    }

    public Set<AugmentationSchemaBuilder> getAugmentations() {
        return addedAugmentations;
    }

    @Override
    public void addAugmentation(AugmentationSchemaBuilder augment) {
        addedAugmentations.add(augment);
    }

    @Override
    public void addUnknownSchemaNode(UnknownSchemaNodeBuilder unknownNode) {
        addedUnknownNodes.add(unknownNode);
    }

    public String getDefaultCase() {
        return defaultCase;
    }

    public void setDefaultCase(String defaultCase) {
        this.defaultCase = defaultCase;
    }

    private final class ChoiceNodeImpl implements ChoiceNode {
        private final QName qname;
        private SchemaPath path;
        private String description;
        private String reference;
        private Status status = Status.CURRENT;
        private boolean augmenting;
        private boolean configuration;
        private ConstraintDefinition constraints;
        private Set<ChoiceCaseNode> cases = Collections.emptySet();
        private Set<AugmentationSchema> augmentations = Collections.emptySet();
        private List<UnknownSchemaNode> unknownNodes = Collections.emptyList();
        private String defaultCase;

        private ChoiceNodeImpl(QName qname) {
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
            if (status != null) {
                this.status = status;
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

        private void setAvailableAugmentations(Set<AugmentationSchema> availableAugmentations) {
            if (availableAugmentations != null) {
                this.augmentations = availableAugmentations;
            }
        }

        @Override
        public List<UnknownSchemaNode> getUnknownSchemaNodes() {
            return unknownNodes;
        }

        private void setUnknownSchemaNodes(List<UnknownSchemaNode> unknownSchemaNodes) {
            if (unknownSchemaNodes != null) {
                this.unknownNodes = unknownSchemaNodes;
            }
        }

        @Override
        public Set<ChoiceCaseNode> getCases() {
            return cases;
        }

        private void setCases(Set<ChoiceCaseNode> cases) {
            if (cases != null) {
                this.cases = cases;
            }
        }

        @Override
        public String getDefaultCase() {
            return defaultCase;
        }

        private void setDefaultCase(String defaultCase) {
            this.defaultCase = defaultCase;
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
            ChoiceNodeImpl other = (ChoiceNodeImpl) obj;
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
            StringBuilder sb = new StringBuilder(ChoiceNodeImpl.class.getSimpleName());
            sb.append("[");
            sb.append("qname=" + qname);
            sb.append("]");
            return sb.toString();
        }
    }

}
