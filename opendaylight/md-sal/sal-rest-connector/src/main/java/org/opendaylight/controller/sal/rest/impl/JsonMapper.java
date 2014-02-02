/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.rest.impl;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.activation.UnsupportedDataTypeException;

import org.opendaylight.controller.sal.core.api.mount.MountInstance;
import org.opendaylight.controller.sal.restconf.impl.ControllerContext;
import org.opendaylight.controller.sal.restconf.impl.IdentityValuesDTO;
import org.opendaylight.controller.sal.restconf.impl.IdentityValuesDTO.IdentityValue;
import org.opendaylight.controller.sal.restconf.impl.IdentityValuesDTO.Predicate;
import org.opendaylight.controller.sal.restconf.impl.RestCodec;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.Node;
import org.opendaylight.yangtools.yang.data.api.SimpleNode;
import org.opendaylight.yangtools.yang.model.api.ChoiceCaseNode;
import org.opendaylight.yangtools.yang.model.api.ChoiceNode;
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
import org.opendaylight.yangtools.yang.model.api.type.IdentityrefTypeDefinition;
import org.opendaylight.yangtools.yang.model.api.type.InstanceIdentifierTypeDefinition;
import org.opendaylight.yangtools.yang.model.api.type.IntegerTypeDefinition;
import org.opendaylight.yangtools.yang.model.api.type.UnsignedIntegerTypeDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.gson.stream.JsonWriter;

class JsonMapper {

    private final Set<LeafListSchemaNode> foundLeafLists = new HashSet<>();
    private final Set<ListSchemaNode> foundLists = new HashSet<>();
    private MountInstance mountPoint;
    private final Logger logger = LoggerFactory.getLogger(JsonMapper.class);

    public void write(JsonWriter writer, CompositeNode data, DataNodeContainer schema, MountInstance mountPoint)
            throws IOException {
        Preconditions.checkNotNull(writer);
        Preconditions.checkNotNull(data);
        Preconditions.checkNotNull(schema);
        this.mountPoint = mountPoint;

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
            if (node.getNodeType().equals(dsn.getQName())) {
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

        if (node.getValue() == null && !(baseType instanceof EmptyTypeDefinition)) {
            logger.debug("While generationg JSON output null value was found for type "
                    + baseType.getClass().getSimpleName() + ".");
        }

        if (baseType instanceof IdentityrefTypeDefinition) {
            if (node.getValue() instanceof QName) {
                IdentityValuesDTO valueDTO = (IdentityValuesDTO) RestCodec.from(baseType, mountPoint).serialize(
                        node.getValue());
                IdentityValue valueFromDTO = valueDTO.getValuesWithNamespaces().get(0);
                String moduleName;
                if (mountPoint != null) {
                    moduleName = ControllerContext.getInstance().findModuleNameByNamespace(mountPoint,
                            URI.create(valueFromDTO.getNamespace()));
                } else {
                    moduleName = ControllerContext.getInstance().findModuleNameByNamespace(
                            URI.create(valueFromDTO.getNamespace()));
                }
                writer.value(moduleName + ":" + valueFromDTO.getValue());
            } else {
                writeStringRepresentation(writer, node, baseType, QName.class);
            }
        } else if (baseType instanceof InstanceIdentifierTypeDefinition) {
            if (node.getValue() instanceof InstanceIdentifier) {
                IdentityValuesDTO valueDTO = (IdentityValuesDTO) RestCodec.from(baseType, mountPoint).serialize(
                        node.getValue());
                writeIdentityValuesDTOToJson(writer, valueDTO);
            } else {
                writeStringRepresentation(writer, node, baseType, InstanceIdentifier.class);
            }
        } else if (baseType instanceof DecimalTypeDefinition || baseType instanceof IntegerTypeDefinition
                || baseType instanceof UnsignedIntegerTypeDefinition) {
            writer.value(new NumberForJsonWriter((String) RestCodec.from(baseType, mountPoint).serialize(
                    node.getValue())));
        } else if (baseType instanceof BooleanTypeDefinition) {
            writer.value(Boolean.parseBoolean((String) RestCodec.from(baseType, mountPoint).serialize(node.getValue())));
        } else if (baseType instanceof EmptyTypeDefinition) {
            writeEmptyDataTypeToJson(writer);
        } else {
            String value = String.valueOf(RestCodec.from(baseType, mountPoint).serialize(node.getValue()));
            if (value == null) {
                value = String.valueOf(node.getValue());
            }
            writer.value(value.equals("null") ? "" : value);
        }
    }

    private void writeIdentityValuesDTOToJson(JsonWriter writer, IdentityValuesDTO valueDTO) throws IOException {
        StringBuilder result = new StringBuilder();
        for (IdentityValue identityValue : valueDTO.getValuesWithNamespaces()) {
            result.append("/");

            writeModuleNameAndIdentifier(result, identityValue);
            if (identityValue.getPredicates() != null && !identityValue.getPredicates().isEmpty()) {
                for (Predicate predicate : identityValue.getPredicates()) {
                    IdentityValue identityValuePredicate = predicate.getName();
                    result.append("[");
                    if (identityValuePredicate == null) {
                        result.append(".");
                    } else {
                        writeModuleNameAndIdentifier(result, identityValuePredicate);
                    }
                    result.append("='");
                    result.append(predicate.getValue());
                    result.append("'");
                    result.append("]");
                }
            }
        }

        writer.value(result.toString());
    }

    private void writeModuleNameAndIdentifier(StringBuilder result, IdentityValue identityValue) {
        String moduleName = ControllerContext.getInstance().findModuleNameByNamespace(
                URI.create(identityValue.getNamespace()));
        if (moduleName != null && !moduleName.isEmpty()) {
            result.append(moduleName);
            result.append(":");
        }
        result.append(identityValue.getValue());
    }

    private void writeStringRepresentation(JsonWriter writer, SimpleNode<?> node, TypeDefinition<?> baseType,
            Class<?> requiredType) throws IOException {
        Object value = node.getValue();
        logger.debug("Value of " + baseType.getQName().getNamespace() + ":" + baseType.getQName().getLocalName()
                + " is not instance of " + requiredType.getClass() + " but is " + node.getValue().getClass());
        if (value == null) {
            writer.value("");
        } else {
            writer.value(String.valueOf(value));
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
            CharSequence moduleName = null;
            if (mountPoint == null) {
                moduleName = contContext.toRestconfIdentifier(schema.getQName());
            } else {
                moduleName = contContext.toRestconfIdentifier(mountPoint, schema.getQName());
            }
            if (moduleName != null) {
                nameForOutput = moduleName.toString();
            } else {
                logger.info("Module '{}' was not found in schema from mount point", schema.getQName());
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
