/*
 *
 *  Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package org.opendaylight.controller.stream;

import com.google.common.base.Preconditions;
import org.opendaylight.controller.cluster.datastore.node.utils.serialization.PathArgumentType;
import org.opendaylight.controller.cluster.datastore.node.utils.serialization.ValueType;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * DDSNormalizedNodeStreamWriter will be used by distributed datastore to send normalized node in
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

public class DDSNormalizedNodeStreamWriter implements NormalizedNodeStreamWriter{

    private DataOutputStream writer;

    private static final Logger LOG = LoggerFactory.getLogger(DDSNormalizedNodeStreamWriter.class);

    private Map<String, Integer> namespaceMap = new HashMap<>();

    public DDSNormalizedNodeStreamWriter(OutputStream stream) throws IOException {
        Preconditions.checkNotNull(stream);
        init(stream);
    }

    private void init(OutputStream stream) throws IOException {
        writer = new DataOutputStream(stream);
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
    public void leafSetEntryNode(YangInstanceIdentifier.NodeWithValue name, Object value) throws IOException, IllegalArgumentException {
        Preconditions.checkNotNull(name, "Node identifier should not be null");

        LOG.debug("Writing a new leaf set entry node");
        startNode(name.getNodeType(), NodeTypes.LEAF_SET_ENTRY_NODE);

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

        writer.writeInt(NodeTypes.AUGMENTATION_NODE.ordinal());
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

        writer.writeInt(NodeTypes.END_NODE.ordinal());
    }

    @Override
    public void close() throws IOException {
        writer.close();
    }

    @Override
    public void flush() throws IOException {
        writer.flush();
    }

    private void startNode(final QName qName, NodeTypes nodeType) throws IOException {

        Preconditions.checkNotNull(qName, "QName of node identifier should not be null.");
        // First write the type of node
        writer.writeInt(nodeType.ordinal());
        // Write Start Tag
        writeQName(qName);
    }

    private void writeQName(QName qName) throws IOException {
        writer.writeUTF(qName.getLocalName());

        String namespace = qName.getNamespace().toString();

        if(namespaceMap.containsKey(namespace)) {
            writer.writeUTF(String.valueOf(namespaceMap.get(namespace)));
        } else {
            namespaceMap.put(namespace, namespaceMap.size());
            writer.writeUTF(namespace);
        }

        writer.writeUTF(qName.getFormattedRevision());
    }

    private void writeObjSet(Set set) throws IOException {
        if(!set.isEmpty()){
            writer.writeInt(set.size());
            for(Object o : set){
                if(o instanceof String){
                    writer.writeUTF(o.toString());
                } else {
                    throw new IllegalArgumentException("Expected value type to be String but was : " +
                        o.toString());
                }
            }
        } else {
            writer.writeInt(0);
        }
    }

    private void writeYangInstanceIdentifier(YangInstanceIdentifier identifier) throws IOException {
        Iterable<YangInstanceIdentifier.PathArgument> pathArguments = identifier.getPathArguments();
        int size = 0;

        if(pathArguments instanceof Collection<?>) {
            Collection<YangInstanceIdentifier.PathArgument> pathArgumentCollection =
                (Collection<YangInstanceIdentifier.PathArgument>) pathArguments;
            size = pathArgumentCollection.size();
        } else { //hopefully, this else will never be used
            for(YangInstanceIdentifier.PathArgument pathArgument : pathArguments) {
                size++;
            }
        }
        writer.writeInt(size);
        for(YangInstanceIdentifier.PathArgument pathArgument : pathArguments) {
            writePathArgument(pathArgument);
        }
    }

    private void writePathArgument(YangInstanceIdentifier.PathArgument pathArgument) throws IOException {

        if(pathArgument instanceof YangInstanceIdentifier.NodeIdentifier) {

            writer.writeInt(PathArgumentType.NODE_IDENTIFIER.ordinal());
            YangInstanceIdentifier.NodeIdentifier nodeIdentifier =
                (YangInstanceIdentifier.NodeIdentifier) pathArgument;

            writeQName(nodeIdentifier.getNodeType());

        } else if(pathArgument instanceof YangInstanceIdentifier.NodeIdentifierWithPredicates) {

            writer.writeInt(PathArgumentType.NODE_IDENTIFIER_WITH_PREDICATES.ordinal());
            YangInstanceIdentifier.NodeIdentifierWithPredicates nodeIdentifier =
                (YangInstanceIdentifier.NodeIdentifierWithPredicates) pathArgument;
            writeQName(nodeIdentifier.getNodeType());

            writeKeyValueMap(nodeIdentifier.getKeyValues());

        } else if(pathArgument instanceof YangInstanceIdentifier.NodeWithValue) {

            writer.writeInt(PathArgumentType.NODE_IDENTIFIER_WITH_VALUE.ordinal());

            YangInstanceIdentifier.NodeWithValue nodeWithValue =
                (YangInstanceIdentifier.NodeWithValue) pathArgument;

            writeQName(nodeWithValue.getNodeType());
            writeObject(nodeWithValue.getValue());


        } else if(pathArgument instanceof YangInstanceIdentifier.AugmentationIdentifier) {

            writer.writeInt(PathArgumentType.AUGMENTATION_IDENTIFIER.ordinal());

            YangInstanceIdentifier.AugmentationIdentifier nodeIdentifier =
                (YangInstanceIdentifier.AugmentationIdentifier) pathArgument;

            // No Qname in augmentation identifier
            writeQNameSet(nodeIdentifier.getPossibleChildNames());
        }
    }

    private void writeKeyValueMap(Map<QName, Object> keyValueMap) throws IOException {
        if(keyValueMap != null && !keyValueMap.isEmpty()) {
            writer.writeInt(keyValueMap.size());
            Set<QName> qNameSet = keyValueMap.keySet();

            for(QName qName : qNameSet) {
                writeQName(qName);
                writeObject(keyValueMap.get(qName));
            }
        } else {
            writer.writeInt(0);
        }
    }

    private void writeQNameSet(Set<QName> children) throws IOException {
        // Write each child's qname separately, if list is empty send count as 0
        if(children != null && !children.isEmpty()) {
            writer.writeInt(children.size());
            for(QName qName : children) {
                writeQName(qName);
            }
        } else {
            LOG.debug("augmentation node does not have any child");
            writer.writeInt(0);
        }
    }

    private void writeObject(Object value) throws IOException {
        // Write object type first
        writer.writeInt(ValueType.getSerializableType(value).ordinal());

        if(value instanceof YangInstanceIdentifier) {
            writeYangInstanceIdentifier((YangInstanceIdentifier) value);

        } else if(value instanceof Set) {
            writeObjSet((Set) value);

        } else if(value instanceof QName) {
            writeQName((QName) value);
        } else if(value instanceof Integer) {
            writer.writeInt((Integer) value);
        } else if(value instanceof Long) {
            writer.writeLong((Long) value);
        } else if(value instanceof Boolean) {
            writer.writeBoolean((Boolean) value);
        } else if(value instanceof Byte) {
            writer.writeByte((Byte) value);
        } else if(value instanceof Short) {
            writer.writeShort((Short) value);
        } else {
            writer.writeUTF(value.toString());
        }

    }

}
