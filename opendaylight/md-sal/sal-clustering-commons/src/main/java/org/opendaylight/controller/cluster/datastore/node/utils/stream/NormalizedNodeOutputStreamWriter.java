/*
 *
 *  Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package org.opendaylight.controller.cluster.datastore.node.utils.stream;

import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeStreamWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * NormalizedNodeOutputStreamWriter will be used by distributed datastore to send normalized node in
 * a stream.
 * A stream writer wrapper around this class will write node objects to stream in recursive manner.
 * for example - If you have a ContainerNode which has a two LeafNode as children, then
 * you will first call {@link #startContainerNode(YangInstanceIdentifier.NodeIdentifier, int)}, then will call
 * {@link #leafNode(YangInstanceIdentifier.NodeIdentifier, Object)} twice and then, {@link #endNode()} to end
 * container node.
 *
 * Based on the each node, the node type is also written to the stream, that helps in reconstructing the object,
 * while reading.
 *
 *
 */

public class NormalizedNodeOutputStreamWriter implements NormalizedNodeStreamWriter {

    private static final Logger LOG = LoggerFactory.getLogger(NormalizedNodeOutputStreamWriter.class);

    static final byte IS_CODE_VALUE = 1;
    static final byte IS_STRING_VALUE = 2;
    static final byte IS_NULL_VALUE = 3;

    private final DataOutput output;

    private final Map<String, Integer> stringCodeMap = new HashMap<>();

    public NormalizedNodeOutputStreamWriter(OutputStream stream) throws IOException {
        Preconditions.checkNotNull(stream);
        output = new DataOutputStream(stream);
    }

    public NormalizedNodeOutputStreamWriter(DataOutput output) throws IOException {
        this.output = Preconditions.checkNotNull(output);
    }

    @Override
    public void leafNode(YangInstanceIdentifier.NodeIdentifier name, Object value) throws IOException, IllegalArgumentException {
        Preconditions.checkNotNull(name, "Node identifier should not be null");
        LOG.debug("Writing a new leaf node");
        startNode(name.getNodeType(), NodeTypes.LEAF_NODE);

        writeObject(value);
    }

    @Override
    public void startLeafSet(YangInstanceIdentifier.NodeIdentifier name, int childSizeHint) throws IOException, IllegalArgumentException {
        Preconditions.checkNotNull(name, "Node identifier should not be null");
        LOG.debug("Starting a new leaf set");

        startNode(name.getNodeType(), NodeTypes.LEAF_SET);
    }

    @Override
    public void leafSetEntryNode(Object value) throws IOException, IllegalArgumentException {
        LOG.debug("Writing a new leaf set entry node");

        output.writeByte(NodeTypes.LEAF_SET_ENTRY_NODE);
        writeObject(value);
    }

    @Override
    public void startContainerNode(YangInstanceIdentifier.NodeIdentifier name, int childSizeHint) throws IOException, IllegalArgumentException {
        Preconditions.checkNotNull(name, "Node identifier should not be null");

        LOG.debug("Starting a new container node");

        startNode(name.getNodeType(), NodeTypes.CONTAINER_NODE);
    }

    @Override
    public void startUnkeyedList(YangInstanceIdentifier.NodeIdentifier name, int childSizeHint) throws IOException, IllegalArgumentException {
        Preconditions.checkNotNull(name, "Node identifier should not be null");
        LOG.debug("Starting a new unkeyed list");

        startNode(name.getNodeType(), NodeTypes.UNKEYED_LIST);
    }

    @Override
    public void startUnkeyedListItem(YangInstanceIdentifier.NodeIdentifier name, int childSizeHint) throws IOException, IllegalStateException {
        Preconditions.checkNotNull(name, "Node identifier should not be null");
        LOG.debug("Starting a new unkeyed list item");

        startNode(name.getNodeType(), NodeTypes.UNKEYED_LIST_ITEM);
    }

    @Override
    public void startMapNode(YangInstanceIdentifier.NodeIdentifier name, int childSizeHint) throws IOException, IllegalArgumentException {
        Preconditions.checkNotNull(name, "Node identifier should not be null");
        LOG.debug("Starting a new map node");

        startNode(name.getNodeType(), NodeTypes.MAP_NODE);
    }

    @Override
    public void startMapEntryNode(YangInstanceIdentifier.NodeIdentifierWithPredicates identifier, int childSizeHint) throws IOException, IllegalArgumentException {
        Preconditions.checkNotNull(identifier, "Node identifier should not be null");
        LOG.debug("Starting a new map entry node");
        startNode(identifier.getNodeType(), NodeTypes.MAP_ENTRY_NODE);

        writeKeyValueMap(identifier.getKeyValues());

    }

    @Override
    public void startOrderedMapNode(YangInstanceIdentifier.NodeIdentifier name, int childSizeHint) throws IOException, IllegalArgumentException {
        Preconditions.checkNotNull(name, "Node identifier should not be null");
        LOG.debug("Starting a new ordered map node");

        startNode(name.getNodeType(), NodeTypes.ORDERED_MAP_NODE);
    }

    @Override
    public void startChoiceNode(YangInstanceIdentifier.NodeIdentifier name, int childSizeHint) throws IOException, IllegalArgumentException {
        Preconditions.checkNotNull(name, "Node identifier should not be null");
        LOG.debug("Starting a new choice node");

        startNode(name.getNodeType(), NodeTypes.CHOICE_NODE);
    }

    @Override
    public void startAugmentationNode(YangInstanceIdentifier.AugmentationIdentifier identifier) throws IOException, IllegalArgumentException {
        Preconditions.checkNotNull(identifier, "Node identifier should not be null");
        LOG.debug("Starting a new augmentation node");

        output.writeByte(NodeTypes.AUGMENTATION_NODE);
        writeQNameSet(identifier.getPossibleChildNames());
    }

    @Override
    public void anyxmlNode(YangInstanceIdentifier.NodeIdentifier name, Object value) throws IOException, IllegalArgumentException {
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
    }

    @Override
    public void flush() throws IOException {
        if (output instanceof OutputStream) {
            ((OutputStream)output).flush();
        }
    }

    private void startNode(final QName qName, byte nodeType) throws IOException {

        Preconditions.checkNotNull(qName, "QName of node identifier should not be null.");
        // First write the type of node
        output.writeByte(nodeType);
        // Write Start Tag
        writeQName(qName);
    }

    private void writeQName(QName qName) throws IOException {

        writeCodedString(qName.getLocalName());
        writeCodedString(qName.getNamespace().toString());
        writeCodedString(qName.getFormattedRevision());
    }

    private void writeCodedString(String key) throws IOException {
        Integer value = stringCodeMap.get(key);
        if(value != null) {
            output.writeByte(IS_CODE_VALUE);
            output.writeInt(value);
        } else {
            if(key != null) {
                output.writeByte(IS_STRING_VALUE);
                stringCodeMap.put(key, Integer.valueOf(stringCodeMap.size()));
                output.writeUTF(key);
            } else {
                output.writeByte(IS_NULL_VALUE);
            }
        }
    }

    private void writeObjSet(Set<?> set) throws IOException {
        if(!set.isEmpty()){
            output.writeInt(set.size());
            for(Object o : set){
                if(o instanceof String){
                    writeCodedString(o.toString());
                } else {
                    throw new IllegalArgumentException("Expected value type to be String but was : " +
                        o.toString());
                }
            }
        } else {
            output.writeInt(0);
        }
    }

    public void writeYangInstanceIdentifier(YangInstanceIdentifier identifier) throws IOException {
        Iterable<YangInstanceIdentifier.PathArgument> pathArguments = identifier.getPathArguments();
        int size = Iterables.size(pathArguments);
        output.writeInt(size);

        for(YangInstanceIdentifier.PathArgument pathArgument : pathArguments) {
            writePathArgument(pathArgument);
        }
    }

    private void writePathArgument(YangInstanceIdentifier.PathArgument pathArgument) throws IOException {

        byte type = PathArgumentTypes.getSerializablePathArgumentType(pathArgument);

        output.writeByte(type);

        switch(type) {
            case PathArgumentTypes.NODE_IDENTIFIER :

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

    private void writeKeyValueMap(Map<QName, Object> keyValueMap) throws IOException {
        if(keyValueMap != null && !keyValueMap.isEmpty()) {
            output.writeInt(keyValueMap.size());
            Set<QName> qNameSet = keyValueMap.keySet();

            for(QName qName : qNameSet) {
                writeQName(qName);
                writeObject(keyValueMap.get(qName));
            }
        } else {
            output.writeInt(0);
        }
    }

    private void writeQNameSet(Set<QName> children) throws IOException {
        // Write each child's qname separately, if list is empty send count as 0
        if(children != null && !children.isEmpty()) {
            output.writeInt(children.size());
            for(QName qName : children) {
                writeQName(qName);
            }
        } else {
            LOG.debug("augmentation node does not have any child");
            output.writeInt(0);
        }
    }

    private void writeObject(Object value) throws IOException {

        byte type = ValueTypes.getSerializableType(value);
        // Write object type first
        output.writeByte(type);

        switch(type) {
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
                writeYangInstanceIdentifier((YangInstanceIdentifier) value);
                break;
            default:
                output.writeUTF(value.toString());
                break;
        }
    }
}
