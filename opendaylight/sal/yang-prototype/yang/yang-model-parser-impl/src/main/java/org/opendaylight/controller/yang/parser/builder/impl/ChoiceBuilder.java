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
import java.util.TreeSet;

import org.opendaylight.controller.yang.common.QName;
import org.opendaylight.controller.yang.model.api.AugmentationSchema;
import org.opendaylight.controller.yang.model.api.ChoiceCaseNode;
import org.opendaylight.controller.yang.model.api.ChoiceNode;
import org.opendaylight.controller.yang.model.api.ConstraintDefinition;
import org.opendaylight.controller.yang.model.api.SchemaPath;
import org.opendaylight.controller.yang.model.api.Status;
import org.opendaylight.controller.yang.model.api.UnknownSchemaNode;
import org.opendaylight.controller.yang.parser.builder.api.AbstractSchemaNodeBuilder;
import org.opendaylight.controller.yang.parser.builder.api.AugmentationSchemaBuilder;
import org.opendaylight.controller.yang.parser.builder.api.AugmentationTargetBuilder;
import org.opendaylight.controller.yang.parser.builder.api.DataSchemaNodeBuilder;
import org.opendaylight.controller.yang.parser.builder.api.GroupingMember;
import org.opendaylight.controller.yang.parser.util.Comparators;
import org.opendaylight.controller.yang.parser.util.ParserUtils;
import org.opendaylight.controller.yang.parser.util.YangParseException;

public final class ChoiceBuilder extends AbstractSchemaNodeBuilder implements DataSchemaNodeBuilder,
        AugmentationTargetBuilder, GroupingMember {
    private boolean isBuilt;
    private final ChoiceNodeImpl instance;
    // DataSchemaNode args
    private boolean augmenting;
    private boolean addedByUses;
    private Boolean configuration;
    private final ConstraintsBuilder constraints;
    // AugmentationTarget args
    private final Set<AugmentationSchemaBuilder> addedAugmentations = new HashSet<AugmentationSchemaBuilder>();
    // ChoiceNode args
    private Set<ChoiceCaseNode> cases;
    private final Set<ChoiceCaseBuilder> addedCases = new HashSet<ChoiceCaseBuilder>();
    private String defaultCase;

    public ChoiceBuilder(final String moduleName, final int line, final QName qname) {
        super(moduleName, line, qname);
        instance = new ChoiceNodeImpl(qname);
        constraints = new ConstraintsBuilder(moduleName, line);
    }

    public ChoiceBuilder(ChoiceBuilder b) {
        super(b.getModuleName(), b.getLine(), b.getQName());
        parent = b.getParent();
        instance = new ChoiceNodeImpl(qname);
        constraints = b.getConstraints();
        schemaPath = b.getPath();
        description = b.getDescription();
        reference = b.getReference();
        status = b.getStatus();
        unknownNodes = b.unknownNodes;
        addedUnknownNodes.addAll(b.getUnknownNodes());
        augmenting = b.isAugmenting();
        addedByUses = b.isAddedByUses();
        configuration = b.isConfiguration();
        addedAugmentations.addAll(b.getAugmentations());
        cases = b.cases;
        addedCases.addAll(b.getCases());
        defaultCase = b.getDefaultCase();
    }

    @Override
    public ChoiceNode build() {
        if (!isBuilt) {
            instance.setPath(schemaPath);
            instance.setDescription(description);
            instance.setReference(reference);
            instance.setStatus(status);
            instance.setAugmenting(augmenting);
            instance.setAddedByUses(addedByUses);
            instance.setConfiguration(configuration);
            instance.setConstraints(constraints.build());
            instance.setDefaultCase(defaultCase);

            // CASES
            if (cases == null) {
                cases = new TreeSet<ChoiceCaseNode>(Comparators.SCHEMA_NODE_COMP);
                for (ChoiceCaseBuilder caseBuilder : addedCases) {
                    cases.add(caseBuilder.build());
                }
            }
            instance.setCases(cases);

            // AUGMENTATIONS
            final Set<AugmentationSchema> augmentations = new HashSet<AugmentationSchema>();
            for (AugmentationSchemaBuilder builder : addedAugmentations) {
                augmentations.add(builder.build());
            }
            instance.setAvailableAugmentations(augmentations);

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
    public void rebuild() {
        isBuilt = false;
        build();
    }

    public Set<ChoiceCaseBuilder> getCases() {
        return addedCases;
    }

    /**
     * Add case node to this choice.
     *
     * If node is not declared with 'case' keyword, create new case builder and
     * make this node child of newly created case.
     *
     * @param caseNode
     *            case node
     */
    public void addCase(DataSchemaNodeBuilder caseNode) {
        QName caseQName = caseNode.getQName();
        String caseName = caseQName.getLocalName();
        for (ChoiceCaseBuilder addedCase : addedCases) {
            if (addedCase.getQName().getLocalName().equals(caseName)) {
                throw new YangParseException(caseNode.getModuleName(), caseNode.getLine(), "Can not add '" + caseNode
                        + "' to node '" + qname.getLocalName() + "' in module '" + moduleName
                        + "': case with same name already declared at line " + addedCase.getLine());
            }
        }

        if (caseNode instanceof ChoiceCaseBuilder) {
            addedCases.add((ChoiceCaseBuilder) caseNode);
        } else {
            ChoiceCaseBuilder caseBuilder = new ChoiceCaseBuilder(caseNode.getModuleName(), caseNode.getLine(),
                    caseQName);
            if (caseNode.isAugmenting()) {
                // if node is added by augmentation, set case builder augmenting
                // as true and node augmenting as false
                caseBuilder.setAugmenting(true);
                caseNode.setAugmenting(false);
            }
            caseBuilder.setPath(caseNode.getPath());
            SchemaPath newPath = ParserUtils.createSchemaPath(caseNode.getPath(), caseQName.getLocalName(),
                    caseQName.getNamespace(), caseQName.getRevision(), caseQName.getPrefix());
            caseNode.setPath(newPath);
            caseBuilder.addChildNode(caseNode);
            addedCases.add(caseBuilder);
        }
    }

    public void setCases(Set<ChoiceCaseNode> cases) {
        this.cases = cases;
    }

    @Override
    public boolean isAugmenting() {
        return augmenting;
    }

    @Override
    public void setAugmenting(boolean augmenting) {
        this.augmenting = augmenting;
    }

    @Override
    public boolean isAddedByUses() {
        return addedByUses;
    }

    @Override
    public void setAddedByUses(final boolean addedByUses) {
        this.addedByUses = addedByUses;
    }

    public Boolean isConfiguration() {
        return configuration;
    }

    @Override
    public void setConfiguration(Boolean configuration) {
        this.configuration = configuration;
    }

    @Override
    public ConstraintsBuilder getConstraints() {
        return constraints;
    }

    public Set<AugmentationSchemaBuilder> getAugmentations() {
        return addedAugmentations;
    }

    @Override
    public void addAugmentation(AugmentationSchemaBuilder augment) {
        addedAugmentations.add(augment);
    }

    public List<UnknownSchemaNodeBuilder> getUnknownNodes() {
        return addedUnknownNodes;
    }

    public String getDefaultCase() {
        return defaultCase;
    }

    public void setDefaultCase(String defaultCase) {
        this.defaultCase = defaultCase;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((schemaPath == null) ? 0 : schemaPath.hashCode());
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
        ChoiceBuilder other = (ChoiceBuilder) obj;
        if (schemaPath == null) {
            if (other.schemaPath != null) {
                return false;
            }
        } else if (!schemaPath.equals(other.schemaPath)) {
            return false;
        }
        if (parent == null) {
            if (other.parent != null) {
                return false;
            }
        } else if (!parent.equals(other.parent)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "choice " + qname.getLocalName();
    }

    public final class ChoiceNodeImpl implements ChoiceNode {
        private final QName qname;
        private SchemaPath path;
        private String description;
        private String reference;
        private Status status = Status.CURRENT;
        private boolean augmenting;
        private boolean addedByUses;
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
        public boolean isAddedByUses() {
            return addedByUses;
        }

        private void setAddedByUses(boolean addedByUses) {
            this.addedByUses = addedByUses;
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

        @Override
        public ChoiceCaseNode getCaseNodeByName(final QName name) {
            if (name == null) {
                throw new IllegalArgumentException("Choice Case QName cannot be NULL!");
            }
            for (final ChoiceCaseNode caseNode : cases) {
                if (caseNode != null) {
                    if (name.equals(caseNode.getQName())) {
                        return caseNode;
                    }
                }
            }
            return null;
        }

        @Override
        public ChoiceCaseNode getCaseNodeByName(final String name) {
            if (name == null) {
                throw new IllegalArgumentException("Choice Case string Name cannot be NULL!");
            }
            for (final ChoiceCaseNode caseNode : cases) {
                if (caseNode != null && (caseNode.getQName() != null)) {
                    if (name.equals(caseNode.getQName().getLocalName())) {
                        return caseNode;
                    }
                }
            }
            return null;
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

        public ChoiceBuilder toBuilder() {
            return ChoiceBuilder.this;
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
