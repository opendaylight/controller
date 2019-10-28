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
import com.google.common.collect.Sets;
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
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeStreamWriter;
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
abstract class AbstractLithiumDataInput extends ForwardingDataInput implements NormalizedNodeDataInput {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractLithiumDataInput.class);

    private final @NonNull DataInput input;

    private final List<String> codedStringMap = new ArrayList<>();

    private QName lastLeafSetQName;

    AbstractLithiumDataInput(final DataInput input) {
        this.input = requireNonNull(input);
    }

    @Override
    final DataInput delegate() {
        return input;
    }

    @Override
    public final void streamNormalizedNode(final NormalizedNodeStreamWriter writer) throws IOException {
        streamNormalizedNode(requireNonNull(writer), input.readByte());
    }

    private void streamNormalizedNode(final NormalizedNodeStreamWriter writer, final byte nodeType) throws IOException {
        switch (nodeType) {
            case NodeTypes.ANY_XML_NODE:
                streamAnyxml(writer);
                break;
            case NodeTypes.AUGMENTATION_NODE:
                streamAugmentation(writer);
                break;
            case NodeTypes.CHOICE_NODE:
                streamChoice(writer);
                break;
            case NodeTypes.CONTAINER_NODE:
                streamContainer(writer);
                break;
            case NodeTypes.LEAF_NODE:
                streamLeaf(writer);
                break;
            case NodeTypes.LEAF_SET:
                streamLeafSet(writer);
                break;
            case NodeTypes.ORDERED_LEAF_SET:
                streamOrderedLeafSet(writer);
                break;
            case NodeTypes.LEAF_SET_ENTRY_NODE:
                streamLeafSetEntry(writer);
                break;
            case NodeTypes.MAP_ENTRY_NODE:
                streamMapEntry(writer);
                break;
            case NodeTypes.MAP_NODE:
                streamMap(writer);
                break;
            case NodeTypes.ORDERED_MAP_NODE:
                streamOrderedMap(writer);
                break;
            case NodeTypes.UNKEYED_LIST:
                streamUnkeyedList(writer);
                break;
            case NodeTypes.UNKEYED_LIST_ITEM:
                streamUnkeyedListItem(writer);
                break;
            default:
                throw new InvalidNormalizedNodeStreamException("Unexpected node " + nodeType);
        }
    }

    private void streamAnyxml(final NormalizedNodeStreamWriter writer) throws IOException {
        final NodeIdentifier identifier = readNodeIdentifier();
        LOG.trace("Streaming anyxml node {}", identifier);
        writer.anyxmlNode(identifier, readDOMSource());
    }

    private void streamAugmentation(final NormalizedNodeStreamWriter writer) throws IOException {
        final AugmentationIdentifier augIdentifier = readAugmentationIdentifier();
        LOG.trace("Streaming augmentation node {}", augIdentifier);
        writer.startAugmentationNode(augIdentifier);
        commonStreamContainer(writer);
    }

    private void streamChoice(final NormalizedNodeStreamWriter writer) throws IOException {
        final NodeIdentifier identifier = readNodeIdentifier();
        LOG.trace("Streaming choice node {}", identifier);
        writer.startChoiceNode(identifier, NormalizedNodeStreamWriter.UNKNOWN_SIZE);
        commonStreamContainer(writer);
    }

    private void streamContainer(final NormalizedNodeStreamWriter writer) throws IOException {
        final NodeIdentifier identifier = readNodeIdentifier();
        LOG.trace("Streaming container node {}", identifier);
        writer.startContainerNode(identifier, NormalizedNodeStreamWriter.UNKNOWN_SIZE);
        commonStreamContainer(writer);
    }

    private void streamLeaf(final NormalizedNodeStreamWriter writer) throws IOException {
        final NodeIdentifier identifier = readNodeIdentifier();
        LOG.trace("Streaming leaf node {}", identifier);
        writer.leafNode(identifier, readObject());
    }

    private void streamLeafSet(final NormalizedNodeStreamWriter writer) throws IOException {
        final NodeIdentifier identifier = readNodeIdentifier();
        LOG.trace("Streaming leaf set node {}", identifier);
        writer.startLeafSet(identifier, NormalizedNodeStreamWriter.UNKNOWN_SIZE);
        commonStreamLeafSet(writer, identifier);
    }

    private void streamOrderedLeafSet(final NormalizedNodeStreamWriter writer) throws IOException {
        final NodeIdentifier identifier = readNodeIdentifier();
        LOG.trace("Streaming ordered leaf set node {}", identifier);
        writer.startOrderedLeafSet(identifier, NormalizedNodeStreamWriter.UNKNOWN_SIZE);
        commonStreamLeafSet(writer, identifier);
    }

    private void commonStreamLeafSet(final NormalizedNodeStreamWriter writer, final NodeIdentifier identifier)
            throws IOException {
        lastLeafSetQName = identifier.getNodeType();
        try {
            commonStreamContainer(writer);
        } finally {
            // Make sure we never leak this
            lastLeafSetQName = null;
        }
    }

    private void streamLeafSetEntry(final NormalizedNodeStreamWriter writer) throws IOException {
        final QName name = lastLeafSetQName != null ? lastLeafSetQName : readQName();
        final Object value = readObject();
        LOG.trace("Streaming leaf set entry node {}, value {}", name, value);
        writer.leafSetEntryNode(name, value);
    }

    private void streamMap(final NormalizedNodeStreamWriter writer) throws IOException {
        final NodeIdentifier identifier = readNodeIdentifier();
        LOG.trace("Streaming map node {}", identifier);
        writer.startMapNode(identifier, NormalizedNodeStreamWriter.UNKNOWN_SIZE);
        commonStreamContainer(writer);
    }

    private void streamOrderedMap(final NormalizedNodeStreamWriter writer) throws IOException {
        final NodeIdentifier identifier = readNodeIdentifier();
        LOG.trace("Streaming ordered map node {}", identifier);
        writer.startOrderedMapNode(identifier, NormalizedNodeStreamWriter.UNKNOWN_SIZE);
        commonStreamContainer(writer);
    }

    private void streamMapEntry(final NormalizedNodeStreamWriter writer) throws IOException {
        final NodeIdentifierWithPredicates entryIdentifier = readNormalizedNodeWithPredicates();
        LOG.trace("Streaming map entry node {}", entryIdentifier);
        writer.startMapEntryNode(entryIdentifier, NormalizedNodeStreamWriter.UNKNOWN_SIZE);
        commonStreamContainer(writer);
    }

    private void streamUnkeyedList(final NormalizedNodeStreamWriter writer) throws IOException {
        final NodeIdentifier identifier = readNodeIdentifier();
        LOG.trace("Streaming unkeyed list node {}", identifier);
        writer.startUnkeyedList(identifier, NormalizedNodeStreamWriter.UNKNOWN_SIZE);
        commonStreamContainer(writer);
    }

    private void streamUnkeyedListItem(final NormalizedNodeStreamWriter writer) throws IOException {
        final NodeIdentifier identifier = readNodeIdentifier();
        LOG.trace("Streaming unkeyed list item node {}", identifier);
        writer.startUnkeyedListItem(identifier, NormalizedNodeStreamWriter.UNKNOWN_SIZE);
        commonStreamContainer(writer);
    }

    private void commonStreamContainer(final NormalizedNodeStreamWriter writer) throws IOException {
        for (byte nodeType = input.readByte(); nodeType != NodeTypes.END_NODE; nodeType = input.readByte()) {
            streamNormalizedNode(writer, nodeType);
        }
        writer.endNode();
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

    final QName defaultReadQName() throws IOException {
        // Read in the same sequence of writing
        String localName = readCodedString();
        String namespace = readCodedString();
        String revision = Strings.emptyToNull(readCodedString());

        return QNameFactory.create(localName, namespace, revision);
    }

    final String readCodedString() throws IOException {
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
        final int count = input.readInt();
        final Set<QName> children = Sets.newHashSetWithExpectedSize(count);
        for (int i = 0; i < count; i++) {
            children.add(readQName());
        }
        return children;
    }

    abstract AugmentationIdentifier readAugmentationIdentifier() throws IOException;

    abstract NodeIdentifier readNodeIdentifier() throws IOException;

    abstract QName readQName() throws IOException;

    final AugmentationIdentifier defaultReadAugmentationIdentifier() throws IOException {
        return AugmentationIdentifier.create(readQNameSet());
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
    public final SchemaPath readSchemaPath() throws IOException {
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
}
