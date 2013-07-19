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
import org.opendaylight.controller.yang.model.api.ContainerSchemaNode;
import org.opendaylight.controller.yang.model.api.GroupingDefinition;
import org.opendaylight.controller.yang.model.api.RpcDefinition;
import org.opendaylight.controller.yang.model.api.SchemaPath;
import org.opendaylight.controller.yang.model.api.Status;
import org.opendaylight.controller.yang.model.api.TypeDefinition;
import org.opendaylight.controller.yang.model.api.UnknownSchemaNode;
import org.opendaylight.controller.yang.parser.builder.api.AbstractSchemaNodeBuilder;
import org.opendaylight.controller.yang.parser.builder.api.GroupingBuilder;
import org.opendaylight.controller.yang.parser.builder.api.TypeDefinitionBuilder;
import org.opendaylight.controller.yang.parser.util.Comparators;

public final class RpcDefinitionBuilder extends AbstractSchemaNodeBuilder {
    private boolean isBuilt;
    private final RpcDefinitionImpl instance;
    private ContainerSchemaNodeBuilder inputBuilder;
    private ContainerSchemaNodeBuilder outputBuilder;
    private final Set<TypeDefinitionBuilder> addedTypedefs = new HashSet<TypeDefinitionBuilder>();
    private final Set<GroupingBuilder> addedGroupings = new HashSet<GroupingBuilder>();

    RpcDefinitionBuilder(final String moduleName, final int line, final QName qname) {
        super(moduleName, line, qname);
        this.instance = new RpcDefinitionImpl(qname);
    }

    @Override
    public RpcDefinition build() {
        if (!isBuilt) {
            instance.setDescription(description);
            instance.setReference(reference);
            instance.setStatus(status);

            final ContainerSchemaNode input = inputBuilder == null ? null : inputBuilder.build();
            final ContainerSchemaNode output = outputBuilder == null ? null : outputBuilder.build();
            instance.setInput(input);
            instance.setOutput(output);

            instance.setPath(schemaPath);

            // TYPEDEFS
            final Set<TypeDefinition<?>> typedefs = new TreeSet<TypeDefinition<?>>(Comparators.SCHEMA_NODE_COMP);
            for (TypeDefinitionBuilder entry : addedTypedefs) {
                typedefs.add(entry.build());
            }
            instance.setTypeDefinitions(typedefs);

            // GROUPINGS
            final Set<GroupingDefinition> groupings = new TreeSet<GroupingDefinition>(Comparators.SCHEMA_NODE_COMP);
            for (GroupingBuilder entry : addedGroupings) {
                groupings.add(entry.build());
            }
            instance.setGroupings(groupings);

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

    void setInput(final ContainerSchemaNodeBuilder inputBuilder) {
        this.inputBuilder = inputBuilder;
    }

    void setOutput(final ContainerSchemaNodeBuilder outputBuilder) {
        this.outputBuilder = outputBuilder;
    }

    public Set<TypeDefinitionBuilder> getTypeDefinitions() {
        return addedTypedefs;
    }

    public void addTypedef(final TypeDefinitionBuilder type) {
        addedTypedefs.add(type);
    }

    public Set<GroupingBuilder> getGroupings() {
        return addedGroupings;
    }

    public void addGrouping(GroupingBuilder grouping) {
        addedGroupings.add(grouping);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((qname == null) ? 0 : qname.hashCode());
        result = prime * result + ((schemaPath == null) ? 0 : schemaPath.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof RpcDefinitionBuilder)) {
            return false;
        }
        final RpcDefinitionBuilder other = (RpcDefinitionBuilder) obj;
        if (other.qname == null) {
            if (this.qname != null) {
                return false;
            }
        } else if (!other.qname.equals(this.qname)) {
            return false;
        }
        if (other.schemaPath == null) {
            if (this.schemaPath != null) {
                return false;
            }
        } else if (!other.schemaPath.equals(this.schemaPath)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "rpc " + qname.getLocalName();
    }

    private final class RpcDefinitionImpl implements RpcDefinition {
        private final QName qname;
        private SchemaPath path;
        private String description;
        private String reference;
        private Status status;
        private ContainerSchemaNode input;
        private ContainerSchemaNode output;
        private Set<TypeDefinition<?>> typeDefinitions;
        private Set<GroupingDefinition> groupings;
        private List<UnknownSchemaNode> unknownNodes = Collections.emptyList();

        private RpcDefinitionImpl(final QName qname) {
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
        public ContainerSchemaNode getInput() {
            return input;
        }

        private void setInput(ContainerSchemaNode input) {
            this.input = input;
        }

        @Override
        public ContainerSchemaNode getOutput() {
            return output;
        }

        private void setOutput(ContainerSchemaNode output) {
            this.output = output;
        }

        @Override
        public Set<TypeDefinition<?>> getTypeDefinitions() {
            return typeDefinitions;
        }

        private void setTypeDefinitions(Set<TypeDefinition<?>> typeDefinitions) {
            this.typeDefinitions = typeDefinitions;
        }

        @Override
        public Set<GroupingDefinition> getGroupings() {
            return groupings;
        }

        private void setGroupings(Set<GroupingDefinition> groupings) {
            this.groupings = groupings;
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
            final RpcDefinitionImpl other = (RpcDefinitionImpl) obj;
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
            StringBuilder sb = new StringBuilder(RpcDefinitionImpl.class.getSimpleName() + "[");
            sb.append("qname=" + qname);
            sb.append(", path=" + path);
            sb.append(", input=" + input);
            sb.append(", output=" + output + "]");
            return sb.toString();
        }
    }

}
