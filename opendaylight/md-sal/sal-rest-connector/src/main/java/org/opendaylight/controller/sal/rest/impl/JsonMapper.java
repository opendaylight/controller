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
import org.opendaylight.yangtools.yang.model.util.*;

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
            writeList(writer, data, (ListSchemaNode) schema);
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
                    writeList(writer, (CompositeNode) child, (ListSchemaNode) childSchema);
                }
            } else if (childSchema instanceof LeafListSchemaNode) {
                if (!foundLeafLists.contains(childSchema)) {
                    Preconditions.checkState(child instanceof SimpleNode<?>,
                            "Data representation of LeafList should be SimpleNode - " + child.getNodeType());
                    foundLeafLists.add((LeafListSchemaNode) childSchema);
                    writeLeafList(writer, (SimpleNode<?>) child, (LeafListSchemaNode) childSchema);
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

    private void writeValueOfNodeByType(JsonWriter writer, SimpleNode<?> node, TypeDefinition<?> type)
            throws IOException {
        if (!(node.getValue() instanceof String)) {
            throw new IllegalStateException("Value in SimpleNode should be type String");
        }

        String value = (String) node.getValue();
        // TODO check Leafref, InstanceIdentifierTypeDefinition,
        // IdentityrefTypeDefinition, UnionTypeDefinition
        if (type.getBaseType() != null) {
            writeValueOfNodeByType(writer, node, type.getBaseType());
        } else if (type instanceof InstanceIdentifierTypeDefinition) {
            writer.value(((InstanceIdentifierTypeDefinition) type).getPathStatement().toString());
        } else if (type instanceof UnionTypeDefinition) {
            processTypeIsUnionType(writer, type, value);
        } else if (type instanceof DecimalTypeDefinition || type instanceof IntegerTypeDefinition
                || type instanceof UnsignedIntegerTypeDefinition) {
            writer.value(new NumberForJsonWriter(value));
        } else if (type instanceof BooleanTypeDefinition) {
            writer.value(Boolean.parseBoolean(value));
        } else if (type instanceof EmptyTypeDefinition) {
            writeEmptyDataTypeToJson(writer);
        } else {
            writer.value(value != null ? value : "");
        }
    }

    private void processTypeIsUnionType(JsonWriter writer, TypeDefinition<?> type, String value) throws IOException {
        if ((value.equals("")) && (containsType(type, EmptyType.class))) {
            writeEmptyDataTypeToJson(writer);
        } else if ((isIntegerNumber(value) || isDecimalNumber(value)) 
                && containsType(type, Int8.class, Int16.class, Int32.class, Int64.class, Uint8.class, Uint16.class,
                        Uint32.class, Uint64.class, Decimal64.class)) {
            writer.value(new NumberForJsonWriter(value));
        } else if (isBoolean(value) && containsType(type, BooleanType.class)) {
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

    private boolean isDecimalNumber(String value) {
        try {
            Double.valueOf(value);
        } catch (NumberFormatException e) {
            return false;
        }
        return true;
    }

    private boolean isIntegerNumber(String value) {
        try {
            Long.valueOf(value);
        } catch (NumberFormatException e) {
            return false;
        }
        return true;
    }

    private boolean containsType(TypeDefinition<?> inputType, Class<?>... searchedTypes) {
        if (!(inputType instanceof UnionTypeDefinition)) {
            throw new IllegalArgumentException("Input type should be of type UnionTypeDefinition");
        }

        List<TypeDefinition<?>> allUnionSubtypes = resolveAllUnionSubtypesFrom((UnionTypeDefinition) inputType);

        for (TypeDefinition<?> unionSubtype : allUnionSubtypes) {
            for (Class<?> searchedType : searchedTypes) {
                if (unionSubtype.getClass().equals(searchedType)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Searches all basic subtypes of {@code inputType}
     * 
     * @param inputType
     * @return list of all subtypes of union type including subtypes of other
     *         union subtype.
     */
    private List<TypeDefinition<?>> resolveAllUnionSubtypesFrom(UnionTypeDefinition inputType) {
        List<TypeDefinition<?>> result = new ArrayList<>();
        for (TypeDefinition<?> subtype : inputType.getTypes()) {
            TypeDefinition<?> resolvedSubtype = subtype;
            if (subtype instanceof ExtendedType) {
                resolvedSubtype = resolveBaseTypeFrom((ExtendedType) subtype);
            }
            if (resolvedSubtype instanceof UnionTypeDefinition) {
                List<TypeDefinition<?>> subtypesFromRecursion = resolveAllUnionSubtypesFrom((UnionTypeDefinition) resolvedSubtype);
                result.addAll(subtypesFromRecursion);
            } else {
                result.add(resolvedSubtype);
            }
        }

        return result;
    }

    /**
     * Finds base type of extended type
     * 
     * @param subtype
     * @return base type for specified {@code subtype} extended type
     */
    private TypeDefinition<?> resolveBaseTypeFrom(ExtendedType subtype) {
        TypeDefinition<?> baseType = subtype.getBaseType();
        if (baseType instanceof ExtendedType) {
            return resolveBaseTypeFrom((ExtendedType) baseType);
        } else {
            return baseType;
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
