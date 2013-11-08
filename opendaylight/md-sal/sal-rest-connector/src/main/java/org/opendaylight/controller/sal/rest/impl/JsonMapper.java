package org.opendaylight.controller.sal.rest.impl;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.Node;
import org.opendaylight.yangtools.yang.data.api.SimpleNode;
import org.opendaylight.yangtools.yang.model.api.ContainerSchemaNode;
import org.opendaylight.yangtools.yang.model.api.DataNodeContainer;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.LeafListSchemaNode;
import org.opendaylight.yangtools.yang.model.api.LeafSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ListSchemaNode;
import org.opendaylight.yangtools.yang.model.api.TypeDefinition;
import org.opendaylight.yangtools.yang.model.api.type.BooleanTypeDefinition;
import org.opendaylight.yangtools.yang.model.api.type.DecimalTypeDefinition;
import org.opendaylight.yangtools.yang.model.api.type.EmptyTypeDefinition;
import org.opendaylight.yangtools.yang.model.api.type.InstanceIdentifierTypeDefinition;
import org.opendaylight.yangtools.yang.model.api.type.IntegerTypeDefinition;
import org.opendaylight.yangtools.yang.model.api.type.UnsignedIntegerTypeDefinition;

import com.google.gson.stream.JsonWriter;

class JsonMapper {
    
    private final Set<LeafListSchemaNode> foundLeafLists = new HashSet<>();
    private final Set<ListSchemaNode> foundLists = new HashSet<>();
    
    public void write(JsonWriter writer, CompositeNode data, DataNodeContainer schema) throws IOException {
        writer.beginObject();
        writeChildrenOfParent(writer, data, schema);
        writer.endObject();
        foundLeafLists.clear();
        foundLists.clear();
    }

    private void writeChildrenOfParent(JsonWriter writer, CompositeNode parent, DataNodeContainer parentSchema) throws IOException {
        checkNotNull(parent);
        checkNotNull(parentSchema);
        
        for (Node<?> child : parent.getChildren()) {
            DataSchemaNode childSchema = findSchemaForNode(child, parentSchema.getChildNodes());
            if (childSchema instanceof ContainerSchemaNode) {
                writeContainer(writer, (CompositeNode) child, (ContainerSchemaNode) childSchema);
            } else if (childSchema instanceof ListSchemaNode) {
                if (!foundLists.contains(childSchema)) {
                    foundLists.add((ListSchemaNode) childSchema);
                    writeList(writer, (CompositeNode) child, (ListSchemaNode) childSchema);
                }
            } else if (childSchema instanceof LeafListSchemaNode) {
                if (!foundLeafLists.contains(childSchema)) {
                    foundLeafLists.add((LeafListSchemaNode) childSchema);
                    writeLeafList(writer, (SimpleNode<?>) child, (LeafListSchemaNode) childSchema);
                }
            } else if (childSchema instanceof LeafSchemaNode) {
                writeLeaf(writer, (SimpleNode<?>) child, (LeafSchemaNode) childSchema);
            }
        }
        
        for (Node<?> child : parent.getChildren()) {
            DataSchemaNode childSchema = findSchemaForNode(child, parentSchema.getChildNodes());
            if (childSchema instanceof LeafListSchemaNode) {
                foundLeafLists.remove((LeafListSchemaNode) childSchema);
            } else if (childSchema instanceof ListSchemaNode) {
                foundLists.remove((ListSchemaNode) childSchema);
            }
        }
    }
    
    private DataSchemaNode findSchemaForNode(Node<?> node, Set<DataSchemaNode> dataSchemaNode) {
        for (DataSchemaNode dsn : dataSchemaNode) {
            if (node.getNodeType().getLocalName().equals(dsn.getQName().getLocalName())) {
                return dsn;
            }
        }
        return null;
    }
    
    private void writeContainer(JsonWriter writer, CompositeNode node, ContainerSchemaNode schema) throws IOException {
        writer.name(node.getNodeType().getLocalName());
        writer.beginObject();
        writeChildrenOfParent(writer, node, schema);
        writer.endObject();
    }
    
    private void writeList(JsonWriter writer, CompositeNode node, ListSchemaNode schema) throws IOException {
            writer.name(node.getNodeType().getLocalName());
            writer.beginArray();
            
            if (node.getParent() != null) {
                CompositeNode parent = node.getParent();
                List<CompositeNode> nodeLists = parent.getCompositesByName(node.getNodeType());
                for (CompositeNode nodeList : nodeLists) {
                    writer.beginObject();
                    writeChildrenOfParent(writer, nodeList, schema);
                    writer.endObject();
                }
            } else {
                writer.beginObject();
                writeChildrenOfParent(writer, node, schema);
                writer.endObject();
            }
            
            writer.endArray();
    }
    
    private void writeLeafList(JsonWriter writer, SimpleNode<?> node, LeafListSchemaNode schema) throws IOException {
            writer.name(node.getNodeType().getLocalName());
            writer.beginArray();
            
            CompositeNode parent = node.getParent();
            List<SimpleNode<?>> nodeLeafLists = parent.getSimpleNodesByName(node.getNodeType());
            for (SimpleNode<?> nodeLeafList : nodeLeafLists) {
                writeValueOfNodeByType(writer, nodeLeafList, schema.getType());
            }
            
            writer.endArray();
    }
    
    private void writeLeaf(JsonWriter writer, SimpleNode<?> node, LeafSchemaNode schema) throws IOException {
        writer.name(node.getNodeType().getLocalName());
        writeValueOfNodeByType(writer, node, schema.getType());
    }
    
    private void writeValueOfNodeByType(JsonWriter writer, SimpleNode<?> node, TypeDefinition<?> type) throws IOException {
        if (!(node.getValue() instanceof String)) {
            throw new IllegalStateException("Value in SimpleNode should be type String");
        }
        
        String value = (String) node.getValue();
        // TODO check Leafref, InstanceIdentifierTypeDefinition, IdentityrefTypeDefinition, UnionTypeDefinition
        if (type.getBaseType() != null) {
            writeValueOfNodeByType(writer, node, type.getBaseType());
        } else if (type instanceof InstanceIdentifierTypeDefinition) {
            writer.value(((InstanceIdentifierTypeDefinition) type).getPathStatement().toString());
        } else if (type instanceof DecimalTypeDefinition 
                || type instanceof IntegerTypeDefinition
                || type instanceof UnsignedIntegerTypeDefinition) {
            writer.value(new NumberForJsonWriter(value));
        } else if (type instanceof BooleanTypeDefinition) {
            writer.value(Boolean.parseBoolean(value));
        } else if (type instanceof EmptyTypeDefinition) {
            writer.value("[null]");
        } else {
            writer.value(value);
        }
    }
    
    private static final class NumberForJsonWriter extends Number {
        
        private static final long serialVersionUID = -3147729419814417666L;
        private final String value;
        
        public NumberForJsonWriter(String value) {
            this.value = value;
        }

        @Override
        public int intValue() {
            throw new IllegalStateException("Should not be invoked");
        }

        @Override
        public long longValue() {
            throw new IllegalStateException("Should not be invoked");
        }

        @Override
        public float floatValue() {
            throw new IllegalStateException("Should not be invoked");
        }

        @Override
        public double doubleValue() {
            throw new IllegalStateException("Should not be invoked");
        }

        @Override
        public String toString() {
            return value;
        }
        
    }

}
