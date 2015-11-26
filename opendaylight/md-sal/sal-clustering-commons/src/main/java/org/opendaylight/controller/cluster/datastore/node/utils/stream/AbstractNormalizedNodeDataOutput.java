/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.node.utils.stream;

import com.google.common.base.Preconditions;
import java.io.DataOutput;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class AbstractNormalizedNodeDataOutput extends AbstractDictionaryAware<NormalizedNodeOutputDictionary>
        implements DictionaryNormalizedNodeDataOutput, NormalizedNodeStreamWriter {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractNormalizedNodeDataOutput.class);
    private final DataOutput output;

    private NormalizedNodeWriter normalizedNodeWriter;
    private boolean headerWritten;

    AbstractNormalizedNodeDataOutput(final DataOutput output, final NormalizedNodeOutputDictionary dictionary) {
        super(dictionary);
        this.output = Preconditions.checkNotNull(output);
    }

    protected abstract short streamVersion();

    void writeQName(final QName qname) throws IOException {
        writeString(qname.getLocalName());
        writeString(qname.getNamespace().toString());
        writeString(qname.getFormattedRevision());
    }

    final void writeString(final String string) throws IOException {
        if (string != null) {
            final Integer value = dictionary().lookupString(string);
            if (value == null) {
                dictionary().storeString(string);
                writeByte(TokenTypes.IS_STRING_VALUE);
                writeUTF(string);
            } else {
                writeByte(TokenTypes.IS_CODE_VALUE);
                writeInt(value);
            }
        } else {
            writeByte(TokenTypes.IS_NULL_VALUE);
        }
    }

    private void ensureHeaderWritten() throws IOException {
        if (!headerWritten) {
            output.writeByte(TokenTypes.SIGNATURE_MARKER);
            output.writeShort(streamVersion());
            headerWritten = true;
        }
    }

    @Override
    public final void write(final int b) throws IOException {
        ensureHeaderWritten();
        output.write(b);
    }

    @Override
    public final void write(final byte[] b) throws IOException {
        ensureHeaderWritten();
        output.write(b);
    }

    @Override
    public final void write(final byte[] b, final int off, final int len) throws IOException {
        ensureHeaderWritten();
        output.write(b, off, len);
    }

    @Override
    public final void writeBoolean(final boolean v) throws IOException {
        ensureHeaderWritten();
        output.writeBoolean(v);
    }

    @Override
    public final void writeByte(final int v) throws IOException {
        ensureHeaderWritten();
        output.writeByte(v);
    }

    @Override
    public final void writeShort(final int v) throws IOException {
        ensureHeaderWritten();
        output.writeShort(v);
    }

    @Override
    public final void writeChar(final int v) throws IOException {
        ensureHeaderWritten();
        output.writeChar(v);
    }

    @Override
    public final void writeInt(final int v) throws IOException {
        ensureHeaderWritten();
        output.writeInt(v);
    }

    @Override
    public final void writeLong(final long v) throws IOException {
        ensureHeaderWritten();
        output.writeLong(v);
    }

    @Override
    public final void writeFloat(final float v) throws IOException {
        ensureHeaderWritten();
        output.writeFloat(v);
    }

    @Override
    public final void writeDouble(final double v) throws IOException {
        ensureHeaderWritten();
        output.writeDouble(v);
    }

    @Override
    public final void writeBytes(final String s) throws IOException {
        ensureHeaderWritten();
        output.writeBytes(s);
    }

    @Override
    public final void writeChars(final String s) throws IOException {
        ensureHeaderWritten();
        output.writeChars(s);
    }

    @Override
    public final void writeUTF(final String s) throws IOException {
        ensureHeaderWritten();
        output.writeUTF(s);
    }

    private NormalizedNodeWriter normalizedNodeWriter() {
        if(normalizedNodeWriter == null) {
            normalizedNodeWriter = NormalizedNodeWriter.forStreamWriter(this);
        }

        return normalizedNodeWriter;
    }

    @Override
    public void writeNormalizedNode(final NormalizedNode<?, ?> node) throws IOException {
        ensureHeaderWritten();
        normalizedNodeWriter().write(node);
    }

    @Override
    public void leafNode(final YangInstanceIdentifier.NodeIdentifier name, final Object value) throws IOException, IllegalArgumentException {
        Preconditions.checkNotNull(name, "Node identifier should not be null");
        LOG.debug("Writing a new leaf node");
        startNode(name.getNodeType(), NodeTypes.LEAF_NODE);

        writeObject(value);
    }

    @Override
    public void startLeafSet(final YangInstanceIdentifier.NodeIdentifier name, final int childSizeHint) throws IOException, IllegalArgumentException {
        Preconditions.checkNotNull(name, "Node identifier should not be null");
        LOG.debug("Starting a new leaf set");

        startNode(name.getNodeType(), NodeTypes.LEAF_SET);
    }

    @Override
    public void leafSetEntryNode(final Object value) throws IOException, IllegalArgumentException {
        LOG.debug("Writing a new leaf set entry node");

        output.writeByte(NodeTypes.LEAF_SET_ENTRY_NODE);
        writeObject(value);
    }

    @Override
    public void startContainerNode(final YangInstanceIdentifier.NodeIdentifier name, final int childSizeHint) throws IOException, IllegalArgumentException {
        Preconditions.checkNotNull(name, "Node identifier should not be null");

        LOG.debug("Starting a new container node");

        startNode(name.getNodeType(), NodeTypes.CONTAINER_NODE);
    }

    @Override
    public void startYangModeledAnyXmlNode(final YangInstanceIdentifier.NodeIdentifier name, final int childSizeHint) throws IOException, IllegalArgumentException {
        Preconditions.checkNotNull(name, "Node identifier should not be null");

        LOG.debug("Starting a new yang modeled anyXml node");

        startNode(name.getNodeType(), NodeTypes.YANG_MODELED_ANY_XML_NODE);
    }

    @Override
    public void startUnkeyedList(final YangInstanceIdentifier.NodeIdentifier name, final int childSizeHint) throws IOException, IllegalArgumentException {
        Preconditions.checkNotNull(name, "Node identifier should not be null");
        LOG.debug("Starting a new unkeyed list");

        startNode(name.getNodeType(), NodeTypes.UNKEYED_LIST);
    }

    @Override
    public void startUnkeyedListItem(final YangInstanceIdentifier.NodeIdentifier name, final int childSizeHint) throws IOException, IllegalStateException {
        Preconditions.checkNotNull(name, "Node identifier should not be null");
        LOG.debug("Starting a new unkeyed list item");

        startNode(name.getNodeType(), NodeTypes.UNKEYED_LIST_ITEM);
    }

    @Override
    public void startMapNode(final YangInstanceIdentifier.NodeIdentifier name, final int childSizeHint) throws IOException, IllegalArgumentException {
        Preconditions.checkNotNull(name, "Node identifier should not be null");
        LOG.debug("Starting a new map node");

        startNode(name.getNodeType(), NodeTypes.MAP_NODE);
    }

    @Override
    public void startMapEntryNode(final YangInstanceIdentifier.NodeIdentifierWithPredicates identifier, final int childSizeHint) throws IOException, IllegalArgumentException {
        Preconditions.checkNotNull(identifier, "Node identifier should not be null");
        LOG.debug("Starting a new map entry node");
        startNode(identifier.getNodeType(), NodeTypes.MAP_ENTRY_NODE);

        writeKeyValueMap(identifier.getKeyValues());

    }

    @Override
    public void startOrderedMapNode(final YangInstanceIdentifier.NodeIdentifier name, final int childSizeHint) throws IOException, IllegalArgumentException {
        Preconditions.checkNotNull(name, "Node identifier should not be null");
        LOG.debug("Starting a new ordered map node");

        startNode(name.getNodeType(), NodeTypes.ORDERED_MAP_NODE);
    }

    @Override
    public void startChoiceNode(final YangInstanceIdentifier.NodeIdentifier name, final int childSizeHint) throws IOException, IllegalArgumentException {
        Preconditions.checkNotNull(name, "Node identifier should not be null");
        LOG.debug("Starting a new choice node");

        startNode(name.getNodeType(), NodeTypes.CHOICE_NODE);
    }

    @Override
    public void startAugmentationNode(final YangInstanceIdentifier.AugmentationIdentifier identifier) throws IOException, IllegalArgumentException {
        Preconditions.checkNotNull(identifier, "Node identifier should not be null");
        LOG.debug("Starting a new augmentation node");

        output.writeByte(NodeTypes.AUGMENTATION_NODE);
        writeQNameSet(identifier.getPossibleChildNames());
    }

    @Override
    public void anyxmlNode(final YangInstanceIdentifier.NodeIdentifier name, final Object value) throws IOException, IllegalArgumentException {
        Preconditions.checkNotNull(name, "Node identifier should not be null");
        LOG.debug("Writing a new xml node");

        startNode(name.getNodeType(), NodeTypes.ANY_XML_NODE);

        writeObject(value);
    }

    @Override
    public void endNode() throws IOException, IllegalStateException {
        LOG.debug("Ending the node");

        output.writeByte(NodeTypes.END_NODE);
    }

    @Override
    public void close() throws IOException {
        flush();
    }

    @Override
    public void flush() throws IOException {
        if (output instanceof OutputStream) {
            ((OutputStream)output).flush();
        }
    }

    private void startNode(final QName qName, final byte nodeType) throws IOException {

        Preconditions.checkNotNull(qName, "QName of node identifier should not be null.");

        ensureHeaderWritten();

        // First write the type of node
        output.writeByte(nodeType);
        // Write Start Tag
        writeQName(qName);
    }

    private void writeObjSet(final Set<?> set) throws IOException {
        output.writeInt(set.size());
        for (Object o : set) {
            Preconditions.checkArgument(o instanceof String, "Expected value type to be String but was %s (%s)",
                o.getClass(), o);

            writeString((String) o);
        }
    }

    @Override
    public void writeYangInstanceIdentifier(final YangInstanceIdentifier identifier) throws IOException {
        ensureHeaderWritten();
        writeYangInstanceIdentifierInternal(identifier);
    }

    private void writeYangInstanceIdentifierInternal(final YangInstanceIdentifier identifier) throws IOException {
        Collection<YangInstanceIdentifier.PathArgument> pathArguments = identifier.getPathArguments();
        output.writeInt(pathArguments.size());

        for(YangInstanceIdentifier.PathArgument pathArgument : pathArguments) {
            writePathArgument(pathArgument);
        }
    }

    @Override
    public void writePathArgument(final YangInstanceIdentifier.PathArgument pathArgument) throws IOException {

        byte type = PathArgumentTypes.getSerializablePathArgumentType(pathArgument);

        output.writeByte(type);

        switch(type) {
            case PathArgumentTypes.NODE_IDENTIFIER:

                YangInstanceIdentifier.NodeIdentifier nodeIdentifier =
                    (YangInstanceIdentifier.NodeIdentifier) pathArgument;

                writeQName(nodeIdentifier.getNodeType());
                break;

            case PathArgumentTypes.NODE_IDENTIFIER_WITH_PREDICATES:

                YangInstanceIdentifier.NodeIdentifierWithPredicates nodeIdentifierWithPredicates =
                    (YangInstanceIdentifier.NodeIdentifierWithPredicates) pathArgument;
                writeQName(nodeIdentifierWithPredicates.getNodeType());

                writeKeyValueMap(nodeIdentifierWithPredicates.getKeyValues());
                break;

            case PathArgumentTypes.NODE_IDENTIFIER_WITH_VALUE :

                YangInstanceIdentifier.NodeWithValue nodeWithValue =
                    (YangInstanceIdentifier.NodeWithValue) pathArgument;

                writeQName(nodeWithValue.getNodeType());
                writeObject(nodeWithValue.getValue());
                break;

            case PathArgumentTypes.AUGMENTATION_IDENTIFIER :

                YangInstanceIdentifier.AugmentationIdentifier augmentationIdentifier =
                    (YangInstanceIdentifier.AugmentationIdentifier) pathArgument;

                // No Qname in augmentation identifier
                writeQNameSet(augmentationIdentifier.getPossibleChildNames());
                break;
            default :
                throw new IllegalStateException("Unknown node identifier type is found : " + pathArgument.getClass().toString() );
        }
    }

    private void writeKeyValueMap(final Map<QName, Object> keyValueMap) throws IOException {
        if (keyValueMap != null && !keyValueMap.isEmpty()) {
            output.writeInt(keyValueMap.size());

            for (QName qName : keyValueMap.keySet()) {
                writeQName(qName);
                writeObject(keyValueMap.get(qName));
            }
        } else {
            output.writeInt(0);
        }
    }

    private void writeQNameSet(final Set<QName> children) throws IOException {
        // Write each child's qname separately, if list is empty send count as 0
        if (children != null && !children.isEmpty()) {
            output.writeInt(children.size());
            for (QName qName : children) {
                writeQName(qName);
            }
        } else {
            LOG.debug("augmentation node does not have any child");
            output.writeInt(0);
        }
    }

    private void writeObject(final Object value) throws IOException {

        byte type = ValueTypes.getSerializableType(value);
        // Write object type first
        output.writeByte(type);

        switch (type) {
            case ValueTypes.BOOL_TYPE:
                output.writeBoolean((Boolean) value);
                break;
            case ValueTypes.QNAME_TYPE:
                writeQName((QName) value);
                break;
            case ValueTypes.INT_TYPE:
                output.writeInt((Integer) value);
                break;
            case ValueTypes.BYTE_TYPE:
                output.writeByte((Byte) value);
                break;
            case ValueTypes.LONG_TYPE:
                output.writeLong((Long) value);
                break;
            case ValueTypes.SHORT_TYPE:
                output.writeShort((Short) value);
                break;
            case ValueTypes.BITS_TYPE:
                writeObjSet((Set<?>) value);
                break;
            case ValueTypes.BINARY_TYPE:
                byte[] bytes = (byte[]) value;
                output.writeInt(bytes.length);
                output.write(bytes);
                break;
            case ValueTypes.YANG_IDENTIFIER_TYPE:
                writeYangInstanceIdentifierInternal((YangInstanceIdentifier) value);
                break;
            case ValueTypes.NULL_TYPE :
                break;
            case ValueTypes.STRING_BYTES_TYPE:
                final byte[] valueBytes = value.toString().getBytes(StandardCharsets.UTF_8);
                output.writeInt(valueBytes.length);
                output.write(valueBytes);
                break;
            default:
                output.writeUTF(value.toString());
                break;
        }
    }
}
