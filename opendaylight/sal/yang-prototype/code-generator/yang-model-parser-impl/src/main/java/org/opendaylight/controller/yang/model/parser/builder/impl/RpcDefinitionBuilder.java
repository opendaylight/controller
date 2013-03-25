/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.yang.model.parser.builder.impl;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.opendaylight.controller.yang.common.QName;
import org.opendaylight.controller.yang.model.api.ContainerSchemaNode;
import org.opendaylight.controller.yang.model.api.GroupingDefinition;
import org.opendaylight.controller.yang.model.api.RpcDefinition;
import org.opendaylight.controller.yang.model.api.SchemaPath;
import org.opendaylight.controller.yang.model.api.Status;
import org.opendaylight.controller.yang.model.api.TypeDefinition;
import org.opendaylight.controller.yang.model.api.UnknownSchemaNode;
import org.opendaylight.controller.yang.model.parser.builder.api.ChildNodeBuilder;
import org.opendaylight.controller.yang.model.parser.builder.api.DataSchemaNodeBuilder;
import org.opendaylight.controller.yang.model.parser.builder.api.GroupingBuilder;
import org.opendaylight.controller.yang.model.parser.builder.api.SchemaNodeBuilder;
import org.opendaylight.controller.yang.model.parser.builder.api.TypeDefinitionAwareBuilder;
import org.opendaylight.controller.yang.model.parser.builder.api.TypeDefinitionBuilder;
import org.opendaylight.controller.yang.model.parser.builder.api.UsesNodeBuilder;

public class RpcDefinitionBuilder implements ChildNodeBuilder,
        SchemaNodeBuilder, TypeDefinitionAwareBuilder {

    private final RpcDefinitionImpl instance;
    private final QName qname;
    private ContainerSchemaNodeBuilder inputBuilder;
    private ContainerSchemaNodeBuilder outputBuilder;
    private final Set<TypeDefinitionBuilder> addedTypedefs = new HashSet<TypeDefinitionBuilder>();
    private final Set<GroupingBuilder> addedGroupings = new HashSet<GroupingBuilder>();

    RpcDefinitionBuilder(QName qname) {
        this.qname = qname;
        this.instance = new RpcDefinitionImpl(qname);
    }

    @Override
    public RpcDefinition build() {
        final ContainerSchemaNode input = inputBuilder.build();
        final ContainerSchemaNode output = outputBuilder.build();
        instance.setInput(input);
        instance.setOutput(output);

        // TYPEDEFS
        Set<TypeDefinition<?>> typedefs = new HashSet<TypeDefinition<?>>();
        for (TypeDefinitionBuilder entry : addedTypedefs) {
            typedefs.add(entry.build());
        }
        instance.setTypeDefinitions(typedefs);

        // GROUPINGS
        final Set<GroupingDefinition> groupings = new HashSet<GroupingDefinition>();
        for (GroupingBuilder entry : addedGroupings) {
            groupings.add(entry.build());
        }
        instance.setGroupings(groupings);

        return instance;
    }

    void setInput(ContainerSchemaNodeBuilder inputBuilder) {
        this.inputBuilder = inputBuilder;
    }

    void setOutput(ContainerSchemaNodeBuilder outputBuilder) {
        this.outputBuilder = outputBuilder;
    }

    @Override
    public void addTypedef(TypeDefinitionBuilder type) {
        addedTypedefs.add(type);
    }

    @Override
    public void setPath(SchemaPath schemaPath) {
        instance.setPath(schemaPath);
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
    public QName getQName() {
        return null;
    }

    @Override
    public void addChildNode(DataSchemaNodeBuilder childNode) {
        throw new UnsupportedOperationException(
                "Can not add child node to rpc definition: rpc can not contains child nodes.");
    }

    @Override
    public void addGrouping(GroupingBuilder grouping) {
        addedGroupings.add(grouping);
    }

    @Override
    public void addUsesNode(UsesNodeBuilder usesBuilder) {
        throw new UnsupportedOperationException(
                "Can not add uses node to rpc definition: rpc can not contains uses nodes.");
    }

    @Override
    public int hashCode() {
        return qname.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof RpcDefinitionBuilder)) {
            return false;
        }
        RpcDefinitionBuilder other = (RpcDefinitionBuilder) obj;
        if (other.qname == null) {
            if (this.qname != null) {
                return false;
            }
        } else if (!other.qname.equals(this.qname)) {
            return false;
        }
        return true;
    }

    private static class RpcDefinitionImpl implements RpcDefinition {

        private final QName qname;
        private SchemaPath path;
        private String description;
        private String reference;
        private Status status;
        private ContainerSchemaNode input;
        private ContainerSchemaNode output;
        private Set<TypeDefinition<?>> typeDefinitions;
        private Set<GroupingDefinition> groupings;
        private List<UnknownSchemaNode> unknownSchemaNodes = Collections.emptyList();

        private RpcDefinitionImpl(QName qname) {
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
            return unknownSchemaNodes;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((qname == null) ? 0 : qname.hashCode());
            result = prime * result + ((path == null) ? 0 : path.hashCode());
            result = prime * result
                    + ((description == null) ? 0 : description.hashCode());
            result = prime * result
                    + ((reference == null) ? 0 : reference.hashCode());
            result = prime * result
                    + ((status == null) ? 0 : status.hashCode());
            result = prime * result
                    + ((input == null) ? 0 : input.hashCode());
            result = prime * result
                    + ((output == null) ? 0 : output.hashCode());
            result = prime * result
                    + ((typeDefinitions == null) ? 0 : typeDefinitions.hashCode());
            result = prime * result
                    + ((groupings == null) ? 0 : groupings.hashCode());
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
            RpcDefinitionImpl other = (RpcDefinitionImpl) obj;
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
            if (input == null) {
                if (other.input != null) {
                    return false;
                }
            } else if (!input.equals(other.input)) {
                return false;
            }
            if (output == null) {
                if (other.output != null) {
                    return false;
                }
            } else if (!output.equals(other.output)) {
                return false;
            }
            if (typeDefinitions == null) {
                if (other.typeDefinitions != null) {
                    return false;
                }
            } else if (!typeDefinitions.equals(other.typeDefinitions)) {
                return false;
            }
            if (groupings == null) {
                if (other.groupings != null) {
                    return false;
                }
            } else if (!groupings.equals(other.groupings)) {
                return false;
            }
            return true;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder(
                    RpcDefinitionImpl.class.getSimpleName() + "[");
            sb.append("qname=" + qname);
            sb.append(", path=" + path);
            sb.append(", description=" + description);
            sb.append(", reference=" + reference);
            sb.append(", status=" + status);
            sb.append(", input=" + input);
            sb.append(", output=" + output + "]");
            return sb.toString();
        }
    }

}
