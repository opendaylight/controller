package org.opendaylight.controller.sal.rest.impl;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.util.*;

import javax.activation.UnsupportedDataTypeException;

import org.opendaylight.controller.sal.restconf.impl.ControllerContext;
import org.opendaylight.yangtools.yang.data.api.*;
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

        List<String> longestPathToElementViaChoiceCase = new ArrayList<>();
        for (Node<?> child : parent.getChildren()) {
            Deque<String> choiceCasePathStack = new ArrayDeque<>(longestPathToElementViaChoiceCase);
            SchemaLocation schemaLocation = findFirstSchemaForNode(child, parentSchema.getChildNodes(),
                    choiceCasePathStack);

            if (schemaLocation == null) {
                if (!choiceCasePathStack.isEmpty()) {
                    throw new UnsupportedDataTypeException("On choice-case path " + choiceCasePathStack
                            + " wasn't found data schema for " + child.getNodeType().getLocalName());
                } else {
                    throw new UnsupportedDataTypeException("Probably the data node \""
                            + child.getNodeType().getLocalName() + "\" is not conform to schema");
                }
            }

            longestPathToElementViaChoiceCase = resolveLongerPath(longestPathToElementViaChoiceCase,
                    schemaLocation.getLocation());

            DataSchemaNode childSchema = schemaLocation.getSchema();

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
            SchemaLocation schemaLocation = findFirstSchemaForNode(child, parentSchema.getChildNodes(),
                    new ArrayDeque<>(longestPathToElementViaChoiceCase));

            DataSchemaNode childSchema = schemaLocation.getSchema();
            if (childSchema instanceof LeafListSchemaNode) {
                foundLeafLists.remove((LeafListSchemaNode) childSchema);
            } else if (childSchema instanceof ListSchemaNode) {
                foundLists.remove((ListSchemaNode) childSchema);
            }
        }
    }

    private List<String> resolveLongerPath(List<String> l1, List<String> l2) {
        return l1.size() > l2.size() ? l1 : l2;
    }

    private SchemaLocation findFirstSchemaForNode(Node<?> node, Set<DataSchemaNode> dataSchemaNode,
            Deque<String> pathIterator) {
        Map<String, ChoiceNode> choiceSubnodes = new HashMap<>();
        for (DataSchemaNode dsn : dataSchemaNode) {
            if (dsn instanceof ChoiceNode) {
                choiceSubnodes.put(dsn.getQName().getLocalName(), (ChoiceNode) dsn);
            } else if (node.getNodeType().getLocalName().equals(dsn.getQName().getLocalName())) {
                return new SchemaLocation(dsn);
            }
        }

        for (ChoiceNode choiceSubnode : choiceSubnodes.values()) {
            if ((!pathIterator.isEmpty() && pathIterator.peekLast().equals(choiceSubnode.getQName().getLocalName()))
                    || pathIterator.isEmpty()) {
                String pathPartChoice = pathIterator.pollLast();
                for (ChoiceCaseNode concreteCase : choiceSubnode.getCases()) {
                    if ((!pathIterator.isEmpty() && pathIterator.peekLast().equals(
                            concreteCase.getQName().getLocalName()))
                            || pathIterator.isEmpty()) {
                        String pathPartCase = pathIterator.pollLast();
                        SchemaLocation schemaLocation = findFirstSchemaForNode(node, concreteCase.getChildNodes(),
                                pathIterator);
                        if (schemaLocation != null) {
                            schemaLocation.addPathPart(concreteCase.getQName().getLocalName());
                            schemaLocation.addPathPart(choiceSubnode.getQName().getLocalName());
                            return schemaLocation;
                        }
                        if (pathPartCase != null) {
                            pathIterator.addLast(pathPartCase);
                        }
                    }
                }
                if (pathPartChoice != null) {
                    pathIterator.addLast(pathPartChoice);
                }
            }
        }
        return null;
    }

    private void writeContainer(JsonWriter writer, CompositeNode node, ContainerSchemaNode schema) throws IOException {
        writeName(node, schema, writer);
        writer.beginObject();
        writeChildrenOfParent(writer, node, schema);
        writer.endObject();
    }

    private void writeList(JsonWriter writer, CompositeNode nodeParent, CompositeNode node, ListSchemaNode schema)
            throws IOException {
        writeName(node, schema, writer);
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

    private void writeLeafList(JsonWriter writer, CompositeNode nodeParent, SimpleNode<?> node,
            LeafListSchemaNode schema) throws IOException {
        writeName(node, schema, writer);
        writer.beginArray();

        List<SimpleNode<?>> nodeLeafLists = nodeParent.getSimpleNodesByName(node.getNodeType());
        for (SimpleNode<?> nodeLeafList : nodeLeafLists) {
            writeValueOfNodeByType(writer, nodeLeafList, schema.getType());
        }

        writer.endArray();
    }

    private void writeLeaf(JsonWriter writer, SimpleNode<?> node, LeafSchemaNode schema) throws IOException {
        writeName(node, schema, writer);
        writeValueOfNodeByType(writer, node, schema.getType());
    }

    private void writeValueOfNodeByType(JsonWriter writer, SimpleNode<?> node, TypeDefinition<?> type)
            throws IOException {

        String value = String.valueOf(node.getValue());
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
            writer.value(value.equals("null") ? "" : value);
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

    private void writeName(Node<?> node, DataSchemaNode schema, JsonWriter writer) throws IOException {
        String nameForOutput = node.getNodeType().getLocalName();
        if (schema.isAugmenting()) {
            ControllerContext contContext = ControllerContext.getInstance();
            CharSequence moduleName;
            moduleName = contContext.toRestconfIdentifier(schema.getQName());
            if (moduleName != null) {
                nameForOutput = moduleName.toString();
            }
        }
        writer.name(nameForOutput);
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
