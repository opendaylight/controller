/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.rest.impl;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Preconditions;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.activation.UnsupportedDataTypeException;
import org.opendaylight.controller.md.sal.dom.api.DOMMountPoint;
import org.opendaylight.controller.sal.restconf.impl.ControllerContext;
import org.opendaylight.controller.sal.restconf.impl.IdentityValuesDTO;
import org.opendaylight.controller.sal.restconf.impl.IdentityValuesDTO.IdentityValue;
import org.opendaylight.controller.sal.restconf.impl.IdentityValuesDTO.Predicate;
import org.opendaylight.controller.sal.restconf.impl.RestCodec;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.Node;
import org.opendaylight.yangtools.yang.data.api.SimpleNode;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.model.api.AnyXmlSchemaNode;
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

class JsonMapper {
    private static final Logger LOG = LoggerFactory.getLogger(JsonMapper.class);
    private final DOMMountPoint mountPoint;
    private static final Map<Character,String> charMapper;

    static {
        charMapper = new HashMap<Character, String>();
        charMapper.put('\\', "\\\\");
        charMapper.put('\"', "\\\"");
        charMapper.put('\n', "\\n");
        charMapper.put('\r', "\\r");
        charMapper.put('\b', "\\b");
        charMapper.put('\f', "\\f");
    }

    public JsonMapper(final DOMMountPoint mountPoint) {
        this.mountPoint = mountPoint;
    }

    public void write(final JsonWriter writer, final CompositeNode data, final DataNodeContainer schema)
            throws IOException {
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
    }

    private void writeChildrenOfParent(final JsonWriter writer, final CompositeNode parent,
            final DataNodeContainer parentSchema) throws IOException {
        checkNotNull(parent);

        final Set<QName> foundLists = new HashSet<>();

        Collection<DataSchemaNode> parentSchemaChildNodes = parentSchema == null ? Collections.<DataSchemaNode> emptySet()
                : parentSchema.getChildNodes();

        for (Node<?> child : parent.getValue()) {
            DataSchemaNode childSchema = findFirstSchemaForNode(child, parentSchemaChildNodes);

            if (childSchema == null) {
                // Node may not conform to schema or allows "anyxml" - we'll process it.

                LOG.debug("No schema found for data node \"{}\"", child.getNodeType());

                if (!foundLists.contains(child.getNodeType())) {
                    handleNoSchemaFound(writer, child, parent);

                    // Since we don't have a schema, we don't know which nodes are supposed to be
                    // lists so treat every one as a potential list to avoid outputting duplicates.

                    foundLists.add(child.getNodeType());
                }
            } else if (childSchema instanceof ContainerSchemaNode) {
                Preconditions.checkState(child instanceof CompositeNode,
                        "Data representation of Container should be CompositeNode - %s", child.getNodeType());
                writeContainer(writer, (CompositeNode) child, (ContainerSchemaNode) childSchema);
            } else if (childSchema instanceof ListSchemaNode) {
                if (!foundLists.contains(child.getNodeType())) {
                    Preconditions.checkState(child instanceof CompositeNode,
                            "Data representation of List should be CompositeNode - %s", child.getNodeType());
                    foundLists.add(child.getNodeType());
                    writeList(writer, parent, (CompositeNode) child, (ListSchemaNode) childSchema);
                }
            } else if (childSchema instanceof LeafListSchemaNode) {
                if (!foundLists.contains(child.getNodeType())) {
                    Preconditions.checkState(child instanceof SimpleNode<?>,
                            "Data representation of LeafList should be SimpleNode - %s", child.getNodeType());
                    foundLists.add(child.getNodeType());
                    writeLeafList(writer, parent, (SimpleNode<?>) child, (LeafListSchemaNode) childSchema);
                }
            } else if (childSchema instanceof LeafSchemaNode) {
                Preconditions.checkState(child instanceof SimpleNode<?>,
                        "Data representation of LeafList should be SimpleNode - %s", child.getNodeType());
                writeLeaf(writer, (SimpleNode<?>) child, (LeafSchemaNode) childSchema);
            } else if (childSchema instanceof AnyXmlSchemaNode) {
                if (child instanceof CompositeNode) {
                    writeContainer(writer, (CompositeNode) child, null);
                } else {
                    handleNoSchemaFound(writer, child, parent);
                }
            } else {
                throw new UnsupportedDataTypeException("Schema can be ContainerSchemaNode, ListSchemaNode, "
                        + "LeafListSchemaNode, or LeafSchemaNode. Other types are not supported yet.");
            }
        }
    }

    private static void writeValue(final JsonWriter writer, final Object value) throws IOException {
        writer.value(value == null ? "" : String.valueOf(value));
    }

    private void handleNoSchemaFound(final JsonWriter writer, final Node<?> node, final CompositeNode parent)
            throws IOException {
        if (node instanceof SimpleNode<?>) {
            List<SimpleNode<?>> nodeLeafList = parent.getSimpleNodesByName(node.getNodeType());
            if (nodeLeafList.size() == 1) {
                writeName(node, null, writer);
                writeValue(writer, node.getValue());
            } else { // more than 1, write as a json array
                writeName(node, null, writer);
                writer.beginArray();
                for (SimpleNode<?> leafNode : nodeLeafList) {
                    writeValue(writer, leafNode.getValue());
                }

                writer.endArray();
            }
        } else { // CompositeNode
            Preconditions.checkState(node instanceof CompositeNode,
                    "Data representation of Container should be CompositeNode - %s", node.getNodeType());

            List<CompositeNode> nodeList = parent.getCompositesByName(node.getNodeType());
            if (nodeList.size() == 1) {
                writeContainer(writer, (CompositeNode) node, null);
            } else { // more than 1, write as a json array
                writeList(writer, parent, (CompositeNode) node, null);
            }
        }
    }

    private static DataSchemaNode findFirstSchemaForNode(final Node<?> node, final Iterable<DataSchemaNode> dataSchemaNode) {
        for (DataSchemaNode dsn : dataSchemaNode) {
            if (node.getNodeType().equals(dsn.getQName())) {
                return dsn;
            }
            if (dsn instanceof ChoiceNode) {
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

    private void writeContainer(final JsonWriter writer, final CompositeNode node, final ContainerSchemaNode schema)
            throws IOException {
        writeName(node, schema, writer);
        writer.beginObject();
        writeChildrenOfParent(writer, node, schema);
        writer.endObject();
    }

    private void writeList(final JsonWriter writer, final CompositeNode nodeParent, final CompositeNode node,
            final ListSchemaNode schema) throws IOException {
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

    private void writeLeafList(final JsonWriter writer, final CompositeNode nodeParent, final SimpleNode<?> node,
            final LeafListSchemaNode schema) throws IOException {
        writeName(node, schema, writer);
        writer.beginArray();

        List<SimpleNode<?>> nodeLeafLists = nodeParent.getSimpleNodesByName(node.getNodeType());
        for (SimpleNode<?> nodeLeafList : nodeLeafLists) {
            writeValueOfNodeByType(writer, nodeLeafList, schema.getType(), schema);
        }
        writer.endArray();
    }

    private void writeLeaf(final JsonWriter writer, final SimpleNode<?> node, final LeafSchemaNode schema)
            throws IOException {
        writeName(node, schema, writer);
        writeValueOfNodeByType(writer, node, schema.getType(), schema);
    }

    private void writeValueOfNodeByType(final JsonWriter writer, final SimpleNode<?> node,
            final TypeDefinition<?> type, final DataSchemaNode schema) throws IOException {

        TypeDefinition<?> baseType = RestUtil.resolveBaseTypeFrom(type);

        if (node.getValue() == null && !(baseType instanceof EmptyTypeDefinition)) {
            LOG.debug("While generationg JSON output null value was found for type {}.", baseType.getClass()
                    .getSimpleName());
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
            if (node.getValue() instanceof YangInstanceIdentifier) {
                IdentityValuesDTO valueDTO = (IdentityValuesDTO) RestCodec.from(baseType, mountPoint).serialize(
                        node.getValue());
                writeIdentityValuesDTOToJson(writer, valueDTO);
            } else {
                writeStringRepresentation(writer, node, baseType, YangInstanceIdentifier.class);
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
            value = escapeIllegalCharacters(value);
            writer.value(value.equals("null") ? "" : value);
        }
    }

    private static void writeIdentityValuesDTOToJson(final JsonWriter writer, final IdentityValuesDTO valueDTO)
            throws IOException {
        StringBuilder result = new StringBuilder();
        for (IdentityValue identityValue : valueDTO.getValuesWithNamespaces()) {
            result.append('/');

            writeModuleNameAndIdentifier(result, identityValue);
            if (identityValue.getPredicates() != null && !identityValue.getPredicates().isEmpty()) {
                for (Predicate predicate : identityValue.getPredicates()) {
                    IdentityValue identityValuePredicate = predicate.getName();
                    result.append('[');
                    if (identityValuePredicate == null) {
                        result.append('.');
                    } else {
                        writeModuleNameAndIdentifier(result, identityValuePredicate);
                    }
                    result.append("='");
                    result.append(predicate.getValue());
                    result.append("']");
                }
            }
        }

        writer.value(result.toString());
    }

    private static void writeModuleNameAndIdentifier(final StringBuilder result, final IdentityValue identityValue) {
        String moduleName = ControllerContext.getInstance().findModuleNameByNamespace(
                URI.create(identityValue.getNamespace()));
        if (moduleName != null && !moduleName.isEmpty()) {
            result.append(moduleName);
            result.append(':');
        }
        result.append(identityValue.getValue());
    }

    private static void writeStringRepresentation(final JsonWriter writer, final SimpleNode<?> node,
            final TypeDefinition<?> baseType, final Class<?> requiredType) throws IOException {
        Object value = node.getValue();
        LOG.debug("Value of {}:{} is not instance of {} but is {}", baseType.getQName().getNamespace(), baseType
                .getQName().getLocalName(), requiredType.getClass(), node.getValue().getClass());
        if (value == null) {
            writer.value("");
        } else {
            writer.value(String.valueOf(value));
        }
    }

    private void writeEmptyDataTypeToJson(final JsonWriter writer) throws IOException {
        writer.beginArray();
        writer.nullValue();
        writer.endArray();
    }

    private void writeName(final Node<?> node, final DataSchemaNode schema, final JsonWriter writer) throws IOException {
        String nameForOutput = node.getNodeType().getLocalName();
        if (schema != null && schema.isAugmenting()) {
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
                LOG.info("Module '{}' was not found in schema from mount point", schema.getQName());
            }
        }
        writer.name(nameForOutput);
    }

    private static String escapeIllegalCharacters(final String input) {
        final StringBuilder result = new StringBuilder();

        for (int i = 0; i < input.length(); i++) {
            final char c = input.charAt(i);
            String placeholder = charMapper.get(c);
            if (placeholder != null) {
                result.append(placeholder);
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }

    private static final class NumberForJsonWriter extends Number {

        private static final long serialVersionUID = -3147729419814417666L;
        private final String value;

        public NumberForJsonWriter(final String value) {
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
