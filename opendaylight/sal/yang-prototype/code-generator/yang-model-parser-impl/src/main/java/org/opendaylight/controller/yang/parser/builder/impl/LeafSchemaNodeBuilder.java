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
import org.opendaylight.controller.yang.model.api.ConstraintDefinition;
import org.opendaylight.controller.yang.model.api.LeafSchemaNode;
import org.opendaylight.controller.yang.model.api.SchemaPath;
import org.opendaylight.controller.yang.model.api.Status;
import org.opendaylight.controller.yang.model.api.TypeDefinition;
import org.opendaylight.controller.yang.model.api.UnknownSchemaNode;
import org.opendaylight.controller.yang.parser.builder.api.AbstractTypeAwareBuilder;
import org.opendaylight.controller.yang.parser.builder.api.DataSchemaNodeBuilder;
import org.opendaylight.controller.yang.parser.builder.api.SchemaNodeBuilder;

public class LeafSchemaNodeBuilder extends AbstractTypeAwareBuilder implements
        DataSchemaNodeBuilder, SchemaNodeBuilder {
    private boolean built;
    private final LeafSchemaNodeImpl instance;
    private final int line;
    // SchemaNode args
    private final QName qname;
    private SchemaPath path;
    private String description;
    private String reference;
    private Status status = Status.CURRENT;
    private final List<UnknownSchemaNodeBuilder> addedUnknownNodes = new ArrayList<UnknownSchemaNodeBuilder>();
    // DataSchemaNode args
    private boolean augmenting;
    private boolean configuration;
    private final ConstraintsBuilder constraints;
    // leaf args
    private String defaultStr;
    private String unitsStr;

    public LeafSchemaNodeBuilder(final QName qname, final int line) {
        this.qname = qname;
        this.line = line;
        instance = new LeafSchemaNodeImpl(qname);
        constraints = new ConstraintsBuilder(line);
    }

    @Override
    public LeafSchemaNode build() {
        if(!built) {
            instance.setPath(path);
            instance.setConstraints(constraints.build());
            instance.setDescription(description);
            instance.setReference(reference);
            instance.setStatus(status);
            instance.setAugmenting(augmenting);
            instance.setConfiguration(configuration);
            instance.setDefault(defaultStr);
            instance.setUnits(unitsStr);

            // TYPE
            if (type == null) {
                instance.setType(typedef.build());
            } else {
                instance.setType(type);
            }

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

    @Override
    public QName getQName() {
        return qname;
    }

    public SchemaPath getPath() {
        return path;
    }

    @Override
    public void setPath(final SchemaPath path) {
        this.path = path;
    }

    @Override
    public ConstraintsBuilder getConstraints() {
        return constraints;
    }

    @Override
    public void addUnknownSchemaNode(final UnknownSchemaNodeBuilder unknownNode) {
        addedUnknownNodes.add(unknownNode);
    }

    public List<UnknownSchemaNodeBuilder> getUnknownNodes() {
        return addedUnknownNodes;
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
    public void setReference(final String reference) {
        this.reference = reference;
    }

    public Status getStatus() {
        return status;
    }

    @Override
    public void setStatus(final Status status) {
        if (status != null) {
            this.status = status;
        }
    }

    public boolean isAugmenting() {
        return augmenting;
    }

    @Override
    public void setAugmenting(final boolean augmenting) {
        this.augmenting = augmenting;
    }



    public boolean isConfiguration() {
        return configuration;
    }

    @Override
    public void setConfiguration(final boolean configuration) {
        instance.setConfiguration(configuration);
    }

    public String getDefaultStr() {
        return defaultStr;
    }

    public void setDefaultStr(String defaultStr) {
        this.defaultStr = defaultStr;
    }

    public String getUnits() {
        return unitsStr;
    }

    public void setUnits(String unitsStr) {
        this.unitsStr = unitsStr;
    }

    private class LeafSchemaNodeImpl implements LeafSchemaNode {
        private final QName qname;
        private SchemaPath path;
        private String description;
        private String reference;
        private Status status = Status.CURRENT;
        private boolean augmenting;
        private boolean configuration;
        private ConstraintDefinition constraintsDef;
        private TypeDefinition<?> type;
        private List<UnknownSchemaNode> unknownNodes = Collections.emptyList();
        private String defaultStr;
        private String unitsStr;

        private LeafSchemaNodeImpl(final QName qname) {
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
        public TypeDefinition<?> getType() {
            return type;
        }

        private void setType(TypeDefinition<? extends TypeDefinition<?>> type) {
            this.type = type;
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

        public String getDefault() {
            return defaultStr;
        }

        private void setDefault(String defaultStr) {
            this.defaultStr = defaultStr;
        }

        public String getUnits() {
            return unitsStr;
        }

        public void setUnits(String unitsStr) {
            this.unitsStr = unitsStr;
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
            LeafSchemaNodeImpl other = (LeafSchemaNodeImpl) obj;
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
                    LeafSchemaNodeImpl.class.getSimpleName());
            sb.append("[");
            sb.append("qname=" + qname);
            sb.append(", path=" + path);
            sb.append("]");
            return sb.toString();
        }
    }

}
