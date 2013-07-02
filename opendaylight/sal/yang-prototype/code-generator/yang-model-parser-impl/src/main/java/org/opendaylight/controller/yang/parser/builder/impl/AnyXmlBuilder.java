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
import java.util.List;

import org.opendaylight.controller.yang.common.QName;
import org.opendaylight.controller.yang.model.api.AnyXmlSchemaNode;
import org.opendaylight.controller.yang.model.api.ConstraintDefinition;
import org.opendaylight.controller.yang.model.api.SchemaPath;
import org.opendaylight.controller.yang.model.api.Status;
import org.opendaylight.controller.yang.model.api.UnknownSchemaNode;
import org.opendaylight.controller.yang.parser.builder.api.AbstractSchemaNodeBuilder;
import org.opendaylight.controller.yang.parser.builder.api.ConfigNode;
import org.opendaylight.controller.yang.parser.builder.api.DataSchemaNodeBuilder;
import org.opendaylight.controller.yang.parser.builder.api.GroupingMember;
import org.opendaylight.controller.yang.parser.util.Comparators;

public final class AnyXmlBuilder extends AbstractSchemaNodeBuilder implements DataSchemaNodeBuilder, GroupingMember,
        ConfigNode {
    private boolean built;
    private final AnyXmlSchemaNodeImpl instance;
    private final ConstraintsBuilder constraints;
    private List<UnknownSchemaNode> unknownNodes;

    private Boolean configuration;
    private boolean augmenting;
    private boolean addedByUses;

    public AnyXmlBuilder(final int line, final QName qname, final SchemaPath schemaPath) {
        super(line, qname);
        this.schemaPath = schemaPath;
        instance = new AnyXmlSchemaNodeImpl(qname);
        constraints = new ConstraintsBuilder(line);
    }

    public AnyXmlBuilder(final AnyXmlBuilder builder) {
        super(builder.getLine(), builder.getQName());
        parent = builder.getParent();
        instance = new AnyXmlSchemaNodeImpl(qname);
        constraints = builder.getConstraints();
        schemaPath = builder.getPath();
        unknownNodes = builder.unknownNodes;
        addedUnknownNodes.addAll(builder.getUnknownNodes());
        description = builder.getDescription();
        reference = builder.getReference();
        status = builder.getStatus();
        configuration = builder.isConfiguration();
        augmenting = builder.isAugmenting();
        addedByUses = builder.isAddedByUses();
    }

    @Override
    public AnyXmlSchemaNode build() {
        if (!built) {
            instance.setPath(schemaPath);
            instance.setConstraints(constraints.build());
            instance.setDescription(description);
            instance.setReference(reference);
            instance.setStatus(status);
            instance.setConfiguration(configuration);
            instance.setAugmenting(augmenting);
            instance.setAddedByUses(addedByUses);

            // UNKNOWN NODES
            if (unknownNodes == null) {
                unknownNodes = new ArrayList<UnknownSchemaNode>();
                for (UnknownSchemaNodeBuilder b : addedUnknownNodes) {
                    unknownNodes.add(b.build());
                }
            }
            Collections.sort(unknownNodes, Comparators.SCHEMA_NODE_COMP);
            instance.setUnknownSchemaNodes(unknownNodes);

            built = true;
        }
        return instance;
    }

    @Override
    public ConstraintsBuilder getConstraints() {
        return constraints;
    }

    public List<UnknownSchemaNodeBuilder> getUnknownNodes() {
        return addedUnknownNodes;
    }

    public void setUnknownNodes(List<UnknownSchemaNode> unknownNodes) {
        this.unknownNodes = unknownNodes;
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
    public boolean isAddedByUses() {
        return addedByUses;
    }

    @Override
    public void setAddedByUses(final boolean addedByUses) {
        this.addedByUses = addedByUses;
    }

    @Override
    public Boolean isConfiguration() {
        return configuration;
    }

    @Override
    public void setConfiguration(final Boolean configuration) {
        this.configuration = configuration;
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
        AnyXmlBuilder other = (AnyXmlBuilder) obj;
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
        return "anyxml " + qname.getLocalName();
    }

    private final class AnyXmlSchemaNodeImpl implements AnyXmlSchemaNode {
        private final QName qname;
        private SchemaPath path;
        private String description;
        private String reference;
        private Status status = Status.CURRENT;
        private boolean configuration;
        private ConstraintDefinition constraintsDef;
        private boolean augmenting;
        private boolean addedByUses;
        private List<UnknownSchemaNode> unknownNodes = Collections.emptyList();

        private AnyXmlSchemaNodeImpl(final QName qname) {
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
            return constraintsDef;
        }

        private void setConstraints(ConstraintDefinition constraintsDef) {
            this.constraintsDef = constraintsDef;
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
            AnyXmlSchemaNodeImpl other = (AnyXmlSchemaNodeImpl) obj;
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
            StringBuilder sb = new StringBuilder(AnyXmlSchemaNodeImpl.class.getSimpleName());
            sb.append("[");
            sb.append("qname=" + qname);
            sb.append(", path=" + path);
            sb.append("]");
            return sb.toString();
        }
    }

}
