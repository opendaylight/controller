package org.opendaylight.controller.sal.rest.impl;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.net.URI;
import java.util.*;

import javax.activation.UnsupportedDataTypeException;

import org.opendaylight.controller.sal.restconf.impl.*;
import org.opendaylight.controller.sal.restconf.impl.IdentityValuesDTO.IdentityValue;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.*;
import org.opendaylight.yangtools.yang.model.api.*;
import org.opendaylight.yangtools.yang.model.api.type.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.gson.stream.JsonWriter;

class JsonMapper {

    private final Set<LeafListSchemaNode> foundLeafLists = new HashSet<>();
    private final Set<ListSchemaNode> foundLists = new HashSet<>();
    private final Logger logger = LoggerFactory.getLogger(JsonMapper.class);

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
            } else if (dsn instanceof ChoiceNode) {
                for (ChoiceCaseNode choiceCase : ((ChoiceNode) dsn).getCases()) {
                    DataSchemaNode foundDsn = findFirstSchemaForNode(node, choiceCase.getChildNodes());
                    if (foundDsn != null) {
                        return foundDsn;
                    }
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
            writeValueOfNodeByType(writer, nodeLeafList, schema.getType(), schema);
        }
        writer.endArray();
    }

    private void writeLeaf(JsonWriter writer, SimpleNode<?> node, LeafSchemaNode schema) throws IOException {
        writeName(node, schema, writer);
        writeValueOfNodeByType(writer, node, schema.getType(), schema);
    }

    private void writeValueOfNodeByType(JsonWriter writer, SimpleNode<?> node, TypeDefinition<?> type,
            DataSchemaNode schema) throws IOException {

        TypeDefinition<?> baseType = RestUtil.resolveBaseTypeFrom(type);

        // TODO check InstanceIdentifierTypeDefinition
        if (baseType instanceof IdentityrefTypeDefinition) {
            if (node.getValue() instanceof QName) {
                IdentityValuesDTO valueDTO = (IdentityValuesDTO) RestCodec.from(baseType).serialize(node.getValue());
                IdentityValue valueFromDTO = valueDTO.getValuesWithNamespaces().get(0);
                String moduleName = ControllerContext.getInstance().findModuleByNamespace(
                        URI.create(valueFromDTO.getNamespace()));
                writer.value(moduleName + ":" + valueFromDTO.getValue());
            } else {
                logger.debug("Value of " + baseType.getQName().getNamespace() + ":"
                        + baseType.getQName().getLocalName() + " is not instance of " + QName.class + " but is "
                        + node.getValue().getClass());
                writer.value(String.valueOf(node.getValue()));
            }
        } else if (baseType instanceof DecimalTypeDefinition || baseType instanceof IntegerTypeDefinition
                || baseType instanceof UnsignedIntegerTypeDefinition) {
            writer.value(new NumberForJsonWriter((String) RestCodec.from(baseType).serialize(node.getValue())));
        } else if (baseType instanceof BooleanTypeDefinition) {
            writer.value(Boolean.parseBoolean((String) RestCodec.from(baseType).serialize(node.getValue())));
        } else if (baseType instanceof EmptyTypeDefinition) {
            writeEmptyDataTypeToJson(writer);
        } else {
            String value = String.valueOf(RestCodec.from(baseType).serialize(node.getValue()));
            if (value == null) {
                value = String.valueOf(node.getValue());
            }
            writer.value(value.equals("null") ? "" : value);
        }
    }

    private void writeEmptyDataTypeToJson(JsonWriter writer) throws IOException {
        writer.beginArray();
        writer.nullValue();
        writer.endArray();
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
