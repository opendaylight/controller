/*
 * Copyright (c) 2014, 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.node.utils.stream;

import static java.util.Objects.requireNonNull;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import java.io.DataInput;
import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.dom.DOMSource;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.controller.cluster.datastore.node.utils.QNameFactory;
import org.opendaylight.yangtools.util.ImmutableOffsetMapTemplate;
import org.opendaylight.yangtools.yang.common.Empty;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.AugmentationIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeWithValue;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.LeafNode;
import org.opendaylight.yangtools.yang.data.api.schema.LeafSetEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.ListNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.NormalizedNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.NormalizedNodeContainerBuilder;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * NormalizedNodeInputStreamReader reads the byte stream and constructs the normalized node including its children
 * nodes. This process goes in recursive manner, where each NodeTypes object signifies the start of the object, except
 * END_NODE. If a node can have children, then that node's end is calculated based on appearance of END_NODE.
 */
class LithiumNormalizedNodeInputStreamReader extends ForwardingDataInput implements NormalizedNodeDataInput {

    private static final Logger LOG = LoggerFactory.getLogger(LithiumNormalizedNodeInputStreamReader.class);

    private final @NonNull DataInput input;

    private final List<String> codedStringMap = new ArrayList<>();

    private QName lastLeafSetQName;

    private NormalizedNodeBuilder<NodeIdentifier, Object, LeafNode<Object>> leafBuilder;

    @SuppressWarnings("rawtypes")
    private NormalizedNodeBuilder<NodeWithValue, Object, LeafSetEntryNode<Object>> leafSetEntryBuilder;

    LithiumNormalizedNodeInputStreamReader(final DataInput input) {
        this.input = requireNonNull(input);
    }

    @Override
    final DataInput delegate() {
        return input;
    }

    @Override
    public NormalizedNodeStreamVersion getVersion() throws IOException {
        return NormalizedNodeStreamVersion.LITHIUM;
    }

    @Override
    public NormalizedNode<?, ?> readNormalizedNode() throws IOException {
        return readNormalizedNodeInternal();
    }

    private NormalizedNode<?, ?> readNormalizedNodeInternal() throws IOException {
        // each node should start with a byte
        byte nodeType = input.readByte();

        if (nodeType == NodeTypes.END_NODE) {
            LOG.trace("End node reached. return");
            lastLeafSetQName = null;
            return null;
        }

        switch (nodeType) {
            case NodeTypes.AUGMENTATION_NODE:
                AugmentationIdentifier augIdentifier = readAugmentationIdentifier();
                LOG.trace("Reading augmentation node {} ", augIdentifier);
                return addDataContainerChildren(Builders.augmentationBuilder().withNodeIdentifier(augIdentifier))
                        .build();

            case NodeTypes.LEAF_SET_ENTRY_NODE:
                final QName name = lastLeafSetQName != null ? lastLeafSetQName : readQName();
                final Object value = readObject();
                final NodeWithValue<Object> leafIdentifier = new NodeWithValue<>(name, value);
                LOG.trace("Reading leaf set entry node {}, value {}", leafIdentifier, value);
                return leafSetEntryBuilder().withNodeIdentifier(leafIdentifier).withValue(value).build();

            case NodeTypes.MAP_ENTRY_NODE:
                final NodeIdentifierWithPredicates entryIdentifier = readNormalizedNodeWithPredicates();
                LOG.trace("Reading map entry node {} ", entryIdentifier);
                return addDataContainerChildren(Builders.mapEntryBuilder().withNodeIdentifier(entryIdentifier))
                        .build();

            default:
                return readNodeIdentifierDependentNode(nodeType, readNodeIdentifier());
        }
    }

    private NormalizedNodeBuilder<NodeIdentifier, Object, LeafNode<Object>> leafBuilder() {
        if (leafBuilder == null) {
            leafBuilder = Builders.leafBuilder();
        }

        return leafBuilder;
    }

    @SuppressWarnings("rawtypes")
    private NormalizedNodeBuilder<NodeWithValue, Object, LeafSetEntryNode<Object>> leafSetEntryBuilder() {
        if (leafSetEntryBuilder == null) {
            leafSetEntryBuilder = Builders.leafSetEntryBuilder();
        }

        return leafSetEntryBuilder;
    }

    private NormalizedNode<?, ?> readNodeIdentifierDependentNode(final byte nodeType, final NodeIdentifier identifier)
        throws IOException {

        switch (nodeType) {
            case NodeTypes.LEAF_NODE:
                LOG.trace("Read leaf node {}", identifier);
                // Read the object value
                return leafBuilder().withNodeIdentifier(identifier).withValue(readObject()).build();

            case NodeTypes.ANY_XML_NODE:
                LOG.trace("Read xml node");
                return Builders.anyXmlBuilder().withNodeIdentifier(identifier).withValue(readDOMSource()).build();

            case NodeTypes.MAP_NODE:
                LOG.trace("Read map node {}", identifier);
                return addDataContainerChildren(Builders.mapBuilder().withNodeIdentifier(identifier)).build();

            case NodeTypes.CHOICE_NODE:
                LOG.trace("Read choice node {}", identifier);
                return addDataContainerChildren(Builders.choiceBuilder().withNodeIdentifier(identifier)).build();

            case NodeTypes.ORDERED_MAP_NODE:
                LOG.trace("Reading ordered map node {}", identifier);
                return addDataContainerChildren(Builders.orderedMapBuilder().withNodeIdentifier(identifier)).build();

            case NodeTypes.UNKEYED_LIST:
                LOG.trace("Read unkeyed list node {}", identifier);
                return addDataContainerChildren(Builders.unkeyedListBuilder().withNodeIdentifier(identifier)).build();

            case NodeTypes.UNKEYED_LIST_ITEM:
                LOG.trace("Read unkeyed list item node {}", identifier);
                return addDataContainerChildren(Builders.unkeyedListEntryBuilder()
                        .withNodeIdentifier(identifier)).build();

            case NodeTypes.CONTAINER_NODE:
                LOG.trace("Read container node {}", identifier);
                return addDataContainerChildren(Builders.containerBuilder().withNodeIdentifier(identifier)).build();

            case NodeTypes.LEAF_SET:
                LOG.trace("Read leaf set node {}", identifier);
                return addLeafSetChildren(identifier.getNodeType(),
                        Builders.leafSetBuilder().withNodeIdentifier(identifier)).build();

            case NodeTypes.ORDERED_LEAF_SET:
                LOG.trace("Read ordered leaf set node {}", identifier);
                return addLeafSetChildren(identifier.getNodeType(),
                        Builders.orderedLeafSetBuilder().withNodeIdentifier(identifier)).build();

            default:
                return null;
        }
    }

    private DOMSource readDOMSource() throws IOException {
        String xml = readObject().toString();
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            Element node = factory.newDocumentBuilder().parse(
                    new InputSource(new StringReader(xml))).getDocumentElement();
            return new DOMSource(node);
        } catch (SAXException | ParserConfigurationException e) {
            throw new IOException("Error parsing XML: " + xml, e);
        }
    }

    QName readQName() throws IOException {
        // Read in the same sequence of writing
        String localName = readCodedString();
        String namespace = readCodedString();
        String revision = Strings.emptyToNull(readCodedString());

        return QNameFactory.create(new QNameFactory.Key(localName, namespace, revision));
    }


    private String readCodedString() throws IOException {
        final byte valueType = input.readByte();
        switch (valueType) {
            case TokenTypes.IS_NULL_VALUE:
                return null;
            case TokenTypes.IS_CODE_VALUE:
                final int code = input.readInt();
                try {
                    return codedStringMap.get(code);
                } catch (IndexOutOfBoundsException e) {
                    throw new IOException("String code " + code + " was not found", e);
                }
            case TokenTypes.IS_STRING_VALUE:
                final String value = input.readUTF().intern();
                codedStringMap.add(value);
                return value;
            default:
                throw new IOException("Unhandled string value type " + valueType);
        }
    }

    private Set<QName> readQNameSet() throws IOException {
        // Read the children count
        int count = input.readInt();
        Set<QName> children = new HashSet<>(count);
        for (int i = 0; i < count; i++) {
            children.add(readQName());
        }
        return children;
    }

    AugmentationIdentifier readAugmentationIdentifier() throws IOException {
        return new AugmentationIdentifier(readQNameSet());
    }

    NodeIdentifier readNodeIdentifier() throws IOException {
        return new NodeIdentifier(readQName());
    }

    private NodeIdentifierWithPredicates readNormalizedNodeWithPredicates() throws IOException {
        final QName qname = readQName();
        final int count = input.readInt();
        switch (count) {
            case 0:
                return new NodeIdentifierWithPredicates(qname);
            case 1:
                return new NodeIdentifierWithPredicates(qname, readQName(), readObject());
            default:
                // ImmutableList is used by ImmutableOffsetMapTemplate for lookups, hence we use that.
                final Builder<QName> keys = ImmutableList.builderWithExpectedSize(count);
                final Object[] values = new Object[count];
                for (int i = 0; i < count; i++) {
                    keys.add(readQName());
                    values[i] = readObject();
                }

                return new NodeIdentifierWithPredicates(qname, ImmutableOffsetMapTemplate.ordered(keys.build())
                    .instantiateWithValues(values));
        }
    }

    private Object readObject() throws IOException {
        byte objectType = input.readByte();
        switch (objectType) {
            case ValueTypes.BITS_TYPE:
                return readObjSet();

            case ValueTypes.BOOL_TYPE:
                return input.readBoolean();

            case ValueTypes.BYTE_TYPE:
                return input.readByte();

            case ValueTypes.INT_TYPE:
                return input.readInt();

            case ValueTypes.LONG_TYPE:
                return input.readLong();

            case ValueTypes.QNAME_TYPE:
                return readQName();

            case ValueTypes.SHORT_TYPE:
                return input.readShort();

            case ValueTypes.STRING_TYPE:
                return input.readUTF();

            case ValueTypes.STRING_BYTES_TYPE:
                return readStringBytes();

            case ValueTypes.BIG_DECIMAL_TYPE:
                return new BigDecimal(input.readUTF());

            case ValueTypes.BIG_INTEGER_TYPE:
                return new BigInteger(input.readUTF());

            case ValueTypes.BINARY_TYPE:
                byte[] bytes = new byte[input.readInt()];
                input.readFully(bytes);
                return bytes;

            case ValueTypes.YANG_IDENTIFIER_TYPE:
                return readYangInstanceIdentifierInternal();

            case ValueTypes.EMPTY_TYPE:
            // Leaf nodes no longer allow null values and thus we no longer emit null values. Previously, the "empty"
            // yang type was represented as null so we translate an incoming null value to Empty. It was possible for
            // a BI user to set a string leaf to null and we're rolling the dice here but the chances for that are
            // very low. We'd have to know the yang type but, even if we did, we can't let a null value pass upstream
            // so we'd have to drop the leaf which might cause other issues.
            case ValueTypes.NULL_TYPE:
                return Empty.getInstance();

            default:
                return null;
        }
    }

    private String readStringBytes() throws IOException {
        byte[] bytes = new byte[input.readInt()];
        input.readFully(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    @Override
    public SchemaPath readSchemaPath() throws IOException {
        final boolean absolute = input.readBoolean();
        final int size = input.readInt();

        final Builder<QName> qnames = ImmutableList.builderWithExpectedSize(size);
        for (int i = 0; i < size; ++i) {
            qnames.add(readQName());
        }
        return SchemaPath.create(qnames.build(), absolute);
    }

    @Override
    public YangInstanceIdentifier readYangInstanceIdentifier() throws IOException {
        return readYangInstanceIdentifierInternal();
    }

    private YangInstanceIdentifier readYangInstanceIdentifierInternal() throws IOException {
        int size = input.readInt();
        final Builder<PathArgument> pathArguments = ImmutableList.builderWithExpectedSize(size);
        for (int i = 0; i < size; i++) {
            pathArguments.add(readPathArgument());
        }
        return YangInstanceIdentifier.create(pathArguments.build());
    }

    private Set<String> readObjSet() throws IOException {
        int count = input.readInt();
        Set<String> children = new HashSet<>(count);
        for (int i = 0; i < count; i++) {
            children.add(readCodedString());
        }
        return children;
    }

    @Override
    public PathArgument readPathArgument() throws IOException {
        // read Type
        int type = input.readByte();

        switch (type) {
            case PathArgumentTypes.AUGMENTATION_IDENTIFIER:
                return readAugmentationIdentifier();
            case PathArgumentTypes.NODE_IDENTIFIER:
                return readNodeIdentifier();
            case PathArgumentTypes.NODE_IDENTIFIER_WITH_PREDICATES:
                return readNormalizedNodeWithPredicates();
            case PathArgumentTypes.NODE_IDENTIFIER_WITH_VALUE:
                return new NodeWithValue<>(readQName(), readObject());
            default:
                // FIXME: throw hard error
                return null;
        }
    }

    @SuppressWarnings("unchecked")
    private ListNodeBuilder<Object, LeafSetEntryNode<Object>> addLeafSetChildren(final QName nodeType,
            final ListNodeBuilder<Object, LeafSetEntryNode<Object>> builder) throws IOException {

        LOG.trace("Reading children of leaf set");

        lastLeafSetQName = nodeType;

        LeafSetEntryNode<Object> child = (LeafSetEntryNode<Object>)readNormalizedNodeInternal();

        while (child != null) {
            builder.withChild(child);
            child = (LeafSetEntryNode<Object>)readNormalizedNodeInternal();
        }
        return builder;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private NormalizedNodeContainerBuilder addDataContainerChildren(
            final NormalizedNodeContainerBuilder builder) throws IOException {
        LOG.trace("Reading data container (leaf nodes) nodes");

        NormalizedNode<?, ?> child = readNormalizedNodeInternal();

        while (child != null) {
            builder.addChild(child);
            child = readNormalizedNodeInternal();
        }
        return builder;
    }
}
