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
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

/**
 * DDSNormalizedNodeStreamWriter will be used by distributed datastore to send normalized node messages in
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

    private ObjectOutputStream writer;

    private static final Logger LOG = LoggerFactory.getLogger(DDSNormalizedNodeStreamWriter.class);

    public DDSNormalizedNodeStreamWriter(OutputStream stream) throws IOException {
        Preconditions.checkNotNull(stream);
        init(stream);
    }

    private void init(OutputStream stream) throws IOException {
        writer = new ObjectOutputStream(stream);
    }

    private void startNode(final QName qName, NodeTypes nodeType) throws IOException {

        Preconditions.checkNotNull(qName, "QName of node identifier should not be null.");
        // First write the type of node
        writer.writeObject(nodeType);
        // Write Start Tag
        writer.writeObject(qName);
    }

    @Override
    public void leafNode(YangInstanceIdentifier.NodeIdentifier name, Object value) throws IOException, IllegalArgumentException {
        Preconditions.checkNotNull(name, "Node identifier should not be null");
        LOG.debug("Writing a new leaf node");
        startNode(name.getNodeType(), NodeTypes.LEAF_NODE);
        writer.writeObject(value);
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

        writer.writeObject(value);
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

        writer.writeObject(identifier.getKeyValues());
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

        writer.writeObject(NodeTypes.AUGMENTATION_NODE);

        writer.writeObject(identifier.getPossibleChildNames());
    }

    @Override
    public void anyxmlNode(YangInstanceIdentifier.NodeIdentifier name, Object value) throws IOException, IllegalArgumentException {
        Preconditions.checkNotNull(name, "Node identifier should not be null");
        LOG.debug("Writing a new xml node");

        startNode(name.getNodeType(), NodeTypes.ANY_XML_NODE);
        writer.writeObject(value);
    }

    @Override
    public void endNode() throws IOException, IllegalStateException {
        LOG.debug("Ending the node");

        writer.writeObject(NodeTypes.END_NODE);
    }

    @Override
    public void close() throws IOException {
        writer.close();
    }

    @Override
    public void flush() throws IOException {
        writer.flush();
    }
}
