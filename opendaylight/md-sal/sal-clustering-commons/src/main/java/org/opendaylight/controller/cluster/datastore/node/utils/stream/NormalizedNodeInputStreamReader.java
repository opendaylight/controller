/*
 * Copyright (c) 2014, 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.node.utils.stream;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.xml.transform.dom.DOMSource;
import org.opendaylight.controller.cluster.datastore.node.utils.QNameFactory;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeWithValue;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.LeafNode;
import org.opendaylight.yangtools.yang.data.api.schema.LeafSetEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.ListNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.NormalizedNodeAttrBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.NormalizedNodeContainerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * NormalizedNodeInputStreamReader reads the byte stream and constructs the normalized node including its children nodes.
 * This process goes in recursive manner, where each NodeTypes object signifies the start of the object, except END_NODE.
 * If a node can have children, then that node's end is calculated based on appearance of END_NODE.
 */
public class NormalizedNodeInputStreamReader extends AbstractNormalizedNodeStream<StreamReaderDictionary>
        implements NormalizedNodeDataInput, NormalizedNodeStreamReader {

    private static final Logger LOG = LoggerFactory.getLogger(NormalizedNodeInputStreamReader.class);

    private static final String REVISION_ARG = "?revision=";

    private final DataInput input;

    private QName lastLeafSetQName;

    private NormalizedNodeAttrBuilder<YangInstanceIdentifier.NodeIdentifier,
                                      Object, LeafNode<Object>> leafBuilder;

    private NormalizedNodeAttrBuilder<NodeWithValue, Object,
                                      LeafSetEntryNode<Object>> leafSetEntryBuilder;

    private final StringBuilder reusableStringBuilder = new StringBuilder(50);

    private boolean readSignatureMarker = true;

    /**
     * @deprecated Use {@link #NormalizedNodeInputStreamReader(DataInput)} instead.
     */
    @Deprecated
    public NormalizedNodeInputStreamReader(final InputStream stream) throws IOException {
        this((DataInput) new DataInputStream(Preconditions.checkNotNull(stream)));
    }

    public NormalizedNodeInputStreamReader(final DataInput input) {
        this(input, new StreamReaderDictionary());
    }

    NormalizedNodeInputStreamReader(final DataInput input, final StreamReaderDictionary dictionary) {
        super(dictionary);
        this.input = Preconditions.checkNotNull(input);
    }

    @Override
    public NormalizedNode<?, ?> readNormalizedNode() throws IOException {
        readSignatureMarkerAndVersionIfNeeded();
        return readNormalizedNodeInternal();
    }

    private void readSignatureMarkerAndVersionIfNeeded() throws IOException {
        if(readSignatureMarker) {
            readSignatureMarker = false;

            final byte marker = input.readByte();
            if(marker != NormalizedNodeOutputStreamWriter.SIGNATURE_MARKER) {
                throw new InvalidNormalizedNodeStreamException(String.format(
                        "Invalid signature marker: %d", marker));
            }

            final short version = input.readShort();
            if (version != NormalizedNodeOutputStreamWriter.CURRENT_VERSION) {
                throw new InvalidNormalizedNodeStreamException(String.format("Unhandled stream version %s", version));
            }
        }
    }

    private NormalizedNode<?, ?> readNormalizedNodeInternal() throws IOException {
        // each node should start with a byte
        byte nodeType = input.readByte();

        if(nodeType == NodeTypes.END_NODE) {
            LOG.debug("End node reached. return");
            return null;
        }

        switch(nodeType) {
            case NodeTypes.AUGMENTATION_NODE :
                YangInstanceIdentifier.AugmentationIdentifier augIdentifier =
                    new YangInstanceIdentifier.AugmentationIdentifier(readQNameSet());

                LOG.debug("Reading augmentation node {} ", augIdentifier);

                return addDataContainerChildren(Builders.augmentationBuilder().
                        withNodeIdentifier(augIdentifier)).build();

            case NodeTypes.LEAF_SET_ENTRY_NODE :
                Object value = readObject();
                NodeWithValue leafIdentifier = new NodeWithValue(lastLeafSetQName, value);

                LOG.debug("Reading leaf set entry node {}, value {}", leafIdentifier, value);

                return leafSetEntryBuilder().withNodeIdentifier(leafIdentifier).withValue(value).build();

            case NodeTypes.MAP_ENTRY_NODE :
                NodeIdentifierWithPredicates entryIdentifier = new NodeIdentifierWithPredicates(
                        readQName(), readKeyValueMap());

                LOG.debug("Reading map entry node {} ", entryIdentifier);

                return addDataContainerChildren(Builders.mapEntryBuilder().
                        withNodeIdentifier(entryIdentifier)).build();

            default :
                return readNodeIdentifierDependentNode(nodeType, new NodeIdentifier(readQName()));
        }
    }

    private NormalizedNodeAttrBuilder<YangInstanceIdentifier.NodeIdentifier,
                                      Object, LeafNode<Object>> leafBuilder() {
        if(leafBuilder == null) {
            leafBuilder = Builders.leafBuilder();
        }

        return leafBuilder;
    }

    private NormalizedNodeAttrBuilder<NodeWithValue, Object,
                                      LeafSetEntryNode<Object>> leafSetEntryBuilder() {
        if(leafSetEntryBuilder == null) {
            leafSetEntryBuilder = Builders.leafSetEntryBuilder();
        }

        return leafSetEntryBuilder;
    }

    private NormalizedNode<?, ?> readNodeIdentifierDependentNode(final byte nodeType, final NodeIdentifier identifier)
        throws IOException {

        switch(nodeType) {
            case NodeTypes.LEAF_NODE :
                LOG.debug("Read leaf node {}", identifier);
                // Read the object value
                return leafBuilder().withNodeIdentifier(identifier).withValue(readObject()).build();

            case NodeTypes.ANY_XML_NODE :
                LOG.debug("Read xml node");
                return Builders.anyXmlBuilder().withValue((DOMSource) readObject()).build();

            case NodeTypes.MAP_NODE :
                LOG.debug("Read map node {}", identifier);
                return addDataContainerChildren(Builders.mapBuilder().
                        withNodeIdentifier(identifier)).build();

            case NodeTypes.CHOICE_NODE :
                LOG.debug("Read choice node {}", identifier);
                return addDataContainerChildren(Builders.choiceBuilder().
                        withNodeIdentifier(identifier)).build();

            case NodeTypes.ORDERED_MAP_NODE :
                LOG.debug("Reading ordered map node {}", identifier);
                return addDataContainerChildren(Builders.orderedMapBuilder().
                        withNodeIdentifier(identifier)).build();

            case NodeTypes.UNKEYED_LIST :
                LOG.debug("Read unkeyed list node {}", identifier);
                return addDataContainerChildren(Builders.unkeyedListBuilder().
                        withNodeIdentifier(identifier)).build();

            case NodeTypes.UNKEYED_LIST_ITEM :
                LOG.debug("Read unkeyed list item node {}", identifier);
                return addDataContainerChildren(Builders.unkeyedListEntryBuilder().
                        withNodeIdentifier(identifier)).build();

            case NodeTypes.CONTAINER_NODE :
                LOG.debug("Read container node {}", identifier);
                return addDataContainerChildren(Builders.containerBuilder().
                        withNodeIdentifier(identifier)).build();

            case NodeTypes.LEAF_SET :
                LOG.debug("Read leaf set node {}", identifier);
                return addLeafSetChildren(identifier.getNodeType(),
                        Builders.leafSetBuilder().withNodeIdentifier(identifier)).build();

            default :
                return null;
        }
    }

    private QName readQName() throws IOException {
        // Read in the same sequence of writing
        String localName = readCodedString();
        String namespace = readCodedString();
        String revision = readCodedString();

        String qName;
        if(!Strings.isNullOrEmpty(revision)) {
            qName = reusableStringBuilder.append('(').append(namespace).append(REVISION_ARG).
                        append(revision).append(')').append(localName).toString();
        } else {
            qName = reusableStringBuilder.append('(').append(namespace).append(')').
                        append(localName).toString();
        }

        reusableStringBuilder.delete(0, reusableStringBuilder.length());
        return QNameFactory.create(qName);
    }

    private String readCodedString() throws IOException {
        final byte type = input.readByte();
        switch (type) {
            case NormalizedNodeOutputStreamWriter.IS_CODE_VALUE:
                final int code = input.readInt();
                final String string = dictionary().lookupString(code);
                if (string == null) {
                    throw new InvalidNormalizedNodeStreamException("String code " + code + " not found in dictionary");
                }
                return string;
            case NormalizedNodeOutputStreamWriter.IS_NULL_VALUE:
                return null;
            case NormalizedNodeOutputStreamWriter.IS_STRING_VALUE:
                // TODO: do we really want to intern here?
                final String value = input.readUTF().intern();
                dictionary().storeString(value);
                return value;
            default:
                throw new InvalidNormalizedNodeStreamException("Unhandled string type " + type);
        }
    }

    private Set<QName> readQNameSet() throws IOException{
        // Read the children count
        int count = input.readInt();
        Set<QName> children = new HashSet<>(count);
        for(int i = 0; i < count; i++) {
            children.add(readQName());
        }
        return children;
    }

    private Map<QName, Object> readKeyValueMap() throws IOException {
        int count = input.readInt();
        Map<QName, Object> keyValueMap = new HashMap<>(count);

        for(int i = 0; i < count; i++) {
            keyValueMap.put(readQName(), readObject());
        }

        return keyValueMap;
    }

    private Object readObject() throws IOException {
        byte objectType = input.readByte();
        switch(objectType) {
            case ValueTypes.BITS_TYPE:
                return readObjSet();

            case ValueTypes.BOOL_TYPE :
                return Boolean.valueOf(input.readBoolean());

            case ValueTypes.BYTE_TYPE :
                return Byte.valueOf(input.readByte());

            case ValueTypes.INT_TYPE :
                return Integer.valueOf(input.readInt());

            case ValueTypes.LONG_TYPE :
                return Long.valueOf(input.readLong());

            case ValueTypes.QNAME_TYPE :
                return readQName();

            case ValueTypes.SHORT_TYPE :
                return Short.valueOf(input.readShort());

            case ValueTypes.STRING_TYPE :
                return input.readUTF();

            case ValueTypes.STRING_BYTES_TYPE:
                return readStringBytes();

            case ValueTypes.BIG_DECIMAL_TYPE :
                return new BigDecimal(input.readUTF());

            case ValueTypes.BIG_INTEGER_TYPE :
                return new BigInteger(input.readUTF());

            case ValueTypes.BINARY_TYPE :
                byte[] bytes = new byte[input.readInt()];
                input.readFully(bytes);
                return bytes;

            case ValueTypes.YANG_IDENTIFIER_TYPE :
                return readYangInstanceIdentifierInternal();

            default :
                return null;
        }
    }

    private String readStringBytes() throws IOException {
        byte[] bytes = new byte[input.readInt()];
        input.readFully(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    @Override
    public YangInstanceIdentifier readYangInstanceIdentifier() throws IOException {
        readSignatureMarkerAndVersionIfNeeded();
        return readYangInstanceIdentifierInternal();
    }

    private YangInstanceIdentifier readYangInstanceIdentifierInternal() throws IOException {
        int size = input.readInt();

        List<PathArgument> pathArguments = new ArrayList<>(size);

        for(int i = 0; i < size; i++) {
            pathArguments.add(readPathArgument());
        }
        return YangInstanceIdentifier.create(pathArguments);
    }

    private Set<String> readObjSet() throws IOException {
        int count = input.readInt();
        Set<String> children = new HashSet<>(count);
        for(int i = 0; i < count; i++) {
            children.add(readCodedString());
        }
        return children;
    }

    @Override
    public PathArgument readPathArgument() throws IOException {
        // read Type
        int type = input.readByte();

        switch(type) {

            case PathArgumentTypes.AUGMENTATION_IDENTIFIER :
                return new YangInstanceIdentifier.AugmentationIdentifier(readQNameSet());

            case PathArgumentTypes.NODE_IDENTIFIER :
                return new NodeIdentifier(readQName());

            case PathArgumentTypes.NODE_IDENTIFIER_WITH_PREDICATES :
                return new NodeIdentifierWithPredicates(readQName(), readKeyValueMap());

            case PathArgumentTypes.NODE_IDENTIFIER_WITH_VALUE :
                return new NodeWithValue(readQName(), readObject());

            default :
                return null;
        }
    }

    @SuppressWarnings("unchecked")
    private ListNodeBuilder<Object, LeafSetEntryNode<Object>> addLeafSetChildren(final QName nodeType,
            final ListNodeBuilder<Object, LeafSetEntryNode<Object>> builder) throws IOException {

        LOG.debug("Reading children of leaf set");

        lastLeafSetQName = nodeType;

        LeafSetEntryNode<Object> child = (LeafSetEntryNode<Object>)readNormalizedNodeInternal();

        while(child != null) {
            builder.withChild(child);
            child = (LeafSetEntryNode<Object>)readNormalizedNodeInternal();
        }
        return builder;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private NormalizedNodeContainerBuilder addDataContainerChildren(
            final NormalizedNodeContainerBuilder builder) throws IOException {
        LOG.debug("Reading data container (leaf nodes) nodes");

        NormalizedNode<?, ?> child = readNormalizedNodeInternal();

        while(child != null) {
            builder.addChild(child);
            child = readNormalizedNodeInternal();
        }
        return builder;
    }

    @Override
    public void readFully(byte[] b) throws IOException {
        readSignatureMarkerAndVersionIfNeeded();
        input.readFully(b);
    }

    @Override
    public void readFully(byte[] b, int off, int len) throws IOException {
        readSignatureMarkerAndVersionIfNeeded();
        input.readFully(b, off, len);
    }

    @Override
    public int skipBytes(int n) throws IOException {
        readSignatureMarkerAndVersionIfNeeded();
        return input.skipBytes(n);
    }

    @Override
    public boolean readBoolean() throws IOException {
        readSignatureMarkerAndVersionIfNeeded();
        return input.readBoolean();
    }

    @Override
    public byte readByte() throws IOException {
        readSignatureMarkerAndVersionIfNeeded();
        return input.readByte();
    }

    @Override
    public int readUnsignedByte() throws IOException {
        readSignatureMarkerAndVersionIfNeeded();
        return input.readUnsignedByte();
    }

    @Override
    public short readShort() throws IOException {
        readSignatureMarkerAndVersionIfNeeded();
        return input.readShort();
    }

    @Override
    public int readUnsignedShort() throws IOException {
        readSignatureMarkerAndVersionIfNeeded();
        return input.readUnsignedShort();
    }

    @Override
    public char readChar() throws IOException {
        readSignatureMarkerAndVersionIfNeeded();
        return input.readChar();
    }

    @Override
    public int readInt() throws IOException {
        readSignatureMarkerAndVersionIfNeeded();
        return input.readInt();
    }

    @Override
    public long readLong() throws IOException {
        readSignatureMarkerAndVersionIfNeeded();
        return input.readLong();
    }

    @Override
    public float readFloat() throws IOException {
        readSignatureMarkerAndVersionIfNeeded();
        return input.readFloat();
    }

    @Override
    public double readDouble() throws IOException {
        readSignatureMarkerAndVersionIfNeeded();
        return input.readDouble();
    }

    @Override
    public String readLine() throws IOException {
        readSignatureMarkerAndVersionIfNeeded();
        return input.readLine();
    }

    @Override
    public String readUTF() throws IOException {
        readSignatureMarkerAndVersionIfNeeded();
        return input.readUTF();
    }
}
