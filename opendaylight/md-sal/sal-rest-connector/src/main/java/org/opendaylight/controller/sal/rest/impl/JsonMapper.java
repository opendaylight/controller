package org.opendaylight.controller.sal.rest.impl;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.util.*;

import javax.activation.UnsupportedDataTypeException;

import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.Node;
import org.opendaylight.yangtools.yang.data.api.SimpleNode;
import org.opendaylight.yangtools.yang.model.api.*;
import org.opendaylight.yangtools.yang.model.api.type.*;

import com.google.common.base.Preconditions;
import com.google.gson.stream.JsonWriter;

class JsonMapper {

    private final Set<LeafListSchemaNode> foundLeafLists = new HashSet<>();
    private final Set<ListSchemaNode> foundLists = new HashSet<>();

    public void write(JsonWriter writer, CompositeNode data, DataNodeContainer schema) throws IOException {
        Preconditions.checkNotNull(writer);
        Preconditions.checkNotNull(data);
        Preconditions.checkNotNull(schema);

        writer.beginObject();

        if (schema instanceof ContainerSchemaNode) {
            writeContainer(writer, data, (ContainerSchemaNode) schema);
        } else if (schema instanceof ListSchemaNode) {
            writeList(writer, null, data, (ListSchemaNode) schema);
        } else {
            throw new UnsupportedDataTypeException(
                    "Schema can be ContainerSchemaNode or ListSchemaNode. Other types are not supported yet.");
        }

        writer.endObject();

        foundLeafLists.clear();
        foundLists.clear();
    }

    private void writeChildrenOfParent(JsonWriter writer, CompositeNode parent, DataNodeContainer parentSchema)
            throws IOException {
        checkNotNull(parent);
        checkNotNull(parentSchema);

        for (Node<?> child : parent.getChildren()) {
            DataSchemaNode childSchema = findFirstSchemaForNode(child, parentSchema.getChildNodes());
            if (childSchema == null) {
                throw new UnsupportedDataTypeException("Probably the data node \"" + child.getNodeType().getLocalName()
                        + "\" is not conform to schema");
            }

            if (childSchema instanceof ContainerSchemaNode) {
                Preconditions.checkState(child instanceof CompositeNode,
                        "Data representation of Container should be CompositeNode - " + child.getNodeType());
                writeContainer(writer, (CompositeNode) child, (ContainerSchemaNode) childSchema);
            } else if (childSchema instanceof ListSchemaNode) {
                if (!foundLists.contains(childSchema)) {
                    Preconditions.checkState(child instanceof CompositeNode,
                            "Data representation of List should be CompositeNode - " + child.getNodeType());
                    foundLists.add((ListSchemaNode) childSchema);
                    writeList(writer, parent, (CompositeNode) child, (ListSchemaNode) childSchema);
                }
            } else if (childSchema instanceof LeafListSchemaNode) {
                if (!foundLeafLists.contains(childSchema)) {
                    Preconditions.checkState(child instanceof SimpleNode<?>,
                            "Data representation of LeafList should be SimpleNode - " + child.getNodeType());
                    foundLeafLists.add((LeafListSchemaNode) childSchema);
                    writeLeafList(writer, parent, (SimpleNode<?>) child, (LeafListSchemaNode) childSchema);
                }
            } else if (childSchema instanceof LeafSchemaNode) {
                Preconditions.checkState(child instanceof SimpleNode<?>,
                        "Data representation of LeafList should be SimpleNode - " + child.getNodeType());
                writeLeaf(writer, (SimpleNode<?>) child, (LeafSchemaNode) childSchema);
            } else {
                throw new UnsupportedDataTypeException("Schema can be ContainerSchemaNode, ListSchemaNode, "
                        + "LeafListSchemaNode, or LeafSchemaNode. Other types are not supported yet.");
            }
        }

        for (Node<?> child : parent.getChildren()) {
            DataSchemaNode childSchema = findFirstSchemaForNode(child, parentSchema.getChildNodes());
            if (childSchema instanceof LeafListSchemaNode) {
                foundLeafLists.remove((LeafListSchemaNode) childSchema);
            } else if (childSchema instanceof ListSchemaNode) {
                foundLists.remove((ListSchemaNode) childSchema);
            }
        }
    }

    private DataSchemaNode findFirstSchemaForNode(Node<?> node, Set<DataSchemaNode> dataSchemaNode) {
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

    private void writeList(JsonWriter writer, CompositeNode nodeParent, CompositeNode node, ListSchemaNode schema) throws IOException {
        writer.name(node.getNodeType().getLocalName());
        writer.beginArray();

        if (nodeParent != null) {
            List<CompositeNode> nodeLists = nodeParent.getCompositesByName(node.getNodeType());
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

    private void writeLeafList(JsonWriter writer, CompositeNode nodeParent, SimpleNode<?> node, LeafListSchemaNode schema) throws IOException {
        writer.name(node.getNodeType().getLocalName());
        writer.beginArray();

        List<SimpleNode<?>> nodeLeafLists = nodeParent.getSimpleNodesByName(node.getNodeType());
        for (SimpleNode<?> nodeLeafList : nodeLeafLists) {
            writeValueOfNodeByType(writer, nodeLeafList, schema.getType());
        }

        writer.endArray();
    }

    private void writeLeaf(JsonWriter writer, SimpleNode<?> node, LeafSchemaNode schema) throws IOException {
        writer.name(node.getNodeType().getLocalName());
        writeValueOfNodeByType(writer, node, schema.getType());
    }

    private void writeValueOfNodeByType(JsonWriter writer, SimpleNode<?> node, TypeDefinition<?> type)
            throws IOException {
        if (!(node.getValue() instanceof String)) {
            throw new IllegalStateException("Value in SimpleNode should be type String");
        }

        String value = (String) node.getValue();
        // TODO check Leafref, InstanceIdentifierTypeDefinition,
        // IdentityrefTypeDefinition, UnionTypeDefinition
        TypeDefinition<?> baseType = resolveBaseTypeFrom(type);
        if (baseType instanceof InstanceIdentifierTypeDefinition) {
            writer.value(((InstanceIdentifierTypeDefinition) baseType).getPathStatement().toString());
        } else if (baseType instanceof UnionTypeDefinition) {
            processTypeIsUnionType(writer, (UnionTypeDefinition) baseType, value);
        } else if (baseType instanceof DecimalTypeDefinition || baseType instanceof IntegerTypeDefinition
                || baseType instanceof UnsignedIntegerTypeDefinition) {
            writer.value(new NumberForJsonWriter(value));
        } else if (baseType instanceof BooleanTypeDefinition) {
            writer.value(Boolean.parseBoolean(value));
        } else if (baseType instanceof EmptyTypeDefinition) {
            writeEmptyDataTypeToJson(writer);
        } else {
            writer.value(value != null ? value : "");
        }
    }

    private void processTypeIsUnionType(JsonWriter writer, UnionTypeDefinition unionType, String value)
            throws IOException {
        if (value == null) {
            writeEmptyDataTypeToJson(writer);
        } else if ((isNumber(value))
                && containsType(unionType, UnsignedIntegerTypeDefinition.class, IntegerTypeDefinition.class,
                        DecimalTypeDefinition.class)) {
            writer.value(new NumberForJsonWriter(value));
        } else if (isBoolean(value) && containsType(unionType, BooleanTypeDefinition.class)) {
            writer.value(Boolean.parseBoolean(value));
        } else {
            writer.value(value);
        }
    }

    private boolean isBoolean(String value) {
        if (value.equals("true") || value.equals("false")) {
            return true;
        }
        return false;
    }

    private void writeEmptyDataTypeToJson(JsonWriter writer) throws IOException {
        writer.beginArray();
        writer.nullValue();
        writer.endArray();
    }

    private boolean isNumber(String value) {
        try {
            Double.valueOf(value);
        } catch (NumberFormatException e) {
            return false;
        }
        return true;
    }

    private boolean containsType(UnionTypeDefinition unionType, Class<?>... searchedTypes) {
        List<TypeDefinition<?>> allUnionSubtypes = resolveAllUnionSubtypesFrom(unionType);

        for (TypeDefinition<?> unionSubtype : allUnionSubtypes) {
            for (Class<?> searchedType : searchedTypes) {
                if (searchedType.isInstance(unionSubtype)) {
                    return true;
                }
            }
        }
        return false;
    }

    private List<TypeDefinition<?>> resolveAllUnionSubtypesFrom(UnionTypeDefinition inputType) {
        List<TypeDefinition<?>> result = new ArrayList<>();
        for (TypeDefinition<?> subtype : inputType.getTypes()) {
            TypeDefinition<?> resolvedSubtype = subtype;

            resolvedSubtype = resolveBaseTypeFrom(subtype);

            if (resolvedSubtype instanceof UnionTypeDefinition) {
                List<TypeDefinition<?>> subtypesFromRecursion = resolveAllUnionSubtypesFrom((UnionTypeDefinition) resolvedSubtype);
                result.addAll(subtypesFromRecursion);
            } else {
                result.add(resolvedSubtype);
            }
        }

        return result;
    }

    private TypeDefinition<?> resolveBaseTypeFrom(TypeDefinition<?> type) {
        return type.getBaseType() != null ? resolveBaseTypeFrom(type.getBaseType()) : type;
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
