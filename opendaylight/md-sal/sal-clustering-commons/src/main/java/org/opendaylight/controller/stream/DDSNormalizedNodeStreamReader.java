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
import org.opendaylight.yangtools.yang.data.api.Node;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.api.schema.LeafSetEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.UnkeyedListEntryNode;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * DDSNormalizedNodeStreamReader reads the object stream and constructs the normalized node including its children nodes.
 * This process goes in recursive manner, where each NodeTypes object signifies the start of the object, except END_NODE.
 * If a node can have children, then that node's end is calculated based on appearance of END_NODE.
 *
 */

public class DDSNormalizedNodeStreamReader implements NormalizedNodeStreamReader {

    private ObjectInputStream reader;

    private static final Logger LOG = LoggerFactory.getLogger(DDSNormalizedNodeStreamReader.class);

    public DDSNormalizedNodeStreamReader(InputStream stream) throws IOException {
        Preconditions.checkNotNull(stream);
        init(stream);
    }

    private void init(InputStream stream) throws IOException {
        reader = new ObjectInputStream(stream);
    }

    public NormalizedNode<?, ?> readNormalizedNode() throws IOException, ClassNotFoundException, URISyntaxException {
        NormalizedNode<?, ?> node = null;
        Object obj = reader.readObject();

        if(obj instanceof NodeTypes) {
            // Start of the node
            NodeTypes nodeType = (NodeTypes) obj;
            if(nodeType.equals(NodeTypes.END_NODE)) {
                LOG.debug("End node reached. return");

                return null;
            }
            else if(nodeType.equals(NodeTypes.AUGMENTATION_NODE)) {
                LOG.debug("Reading augmentation node. will create augmentation identifier");

                Set<QName> children = (Set<QName>) reader.readObject();

                YangInstanceIdentifier.AugmentationIdentifier identifier = new YangInstanceIdentifier.AugmentationIdentifier(children);

                node = Builders.augmentationBuilder().withNodeIdentifier(identifier).withValue(getDataContainerChildren()).build();

            } else {
                // read the start string
                QName qName = (QName)reader.readObject();

               if(nodeType.equals(NodeTypes.LEAF_SET_ENTRY_NODE)) {
                   LOG.debug("Reading leaf set entry node. Will create NodeWithValue instance identifier");

                    // Read the object value
                   Object value = reader.readObject();

                   YangInstanceIdentifier.NodeWithValue nodeWithValue = new YangInstanceIdentifier.NodeWithValue(qName, value);
                    node =  Builders.leafSetEntryBuilder().withNodeIdentifier(nodeWithValue).withValue(value).build();

                } else if(nodeType.equals(NodeTypes.MAP_ENTRY_NODE)) {
                    LOG.debug("Reading map entry node. Will create node identifier with predicates.");
                    Map<QName, Object> keyValues  = (Map<QName, Object>)reader.readObject();

                    YangInstanceIdentifier.NodeIdentifierWithPredicates nodeIdentifier =
                        new YangInstanceIdentifier.NodeIdentifierWithPredicates(qName, keyValues);
                    nodeIdentifier.getKeyValues();

                    node = Builders.mapEntryBuilder().withNodeIdentifier(nodeIdentifier).withValue(getDataContainerChildren()).build();

                } else {
                   LOG.debug("Creating standard node identifier. ");
                   YangInstanceIdentifier.NodeIdentifier identifier = new YangInstanceIdentifier.NodeIdentifier(qName);
                   node = readNodeIdentifierDependentNode(nodeType, identifier);

                }
            }
        }
        return node;
    }

    private NormalizedNode<?, ?> readNodeIdentifierDependentNode(NodeTypes nodeType, YangInstanceIdentifier.NodeIdentifier identifier)
        throws IOException, ClassNotFoundException, URISyntaxException {
        NormalizedNode<?, ?> node = null;
        if (nodeType.equals(NodeTypes.LEAF_NODE)) {
            LOG.debug("Read leaf node");
            // Read the object value
            node = Builders.leafBuilder().withNodeIdentifier(identifier).withValue(reader.readObject()).build();

        } else if (nodeType.equals(NodeTypes.ANY_XML_NODE)) {
            LOG.debug("Read xml node");
            Node value = (Node) reader.readObject();
            node = Builders.anyXmlBuilder().withValue(value).build();

        } else if (nodeType.equals(NodeTypes.MAP_NODE)) {
            LOG.debug("Read map node");
            node = Builders.mapBuilder().withNodeIdentifier(identifier).withValue(getMapNodeChildren()).build();

        } else if (nodeType.equals(NodeTypes.CHOICE_NODE)) {
            LOG.debug("Read choice node");
            node = Builders.choiceBuilder().withNodeIdentifier(identifier).withValue(getDataContainerChildren()).build();

        } else if (nodeType.equals(NodeTypes.ORDERED_MAP_NODE)) {

            node = Builders.orderedMapBuilder().withNodeIdentifier(identifier).withValue(getMapNodeChildren()).build();

        } else if (nodeType.equals(NodeTypes.UNKEYED_LIST)) {
            LOG.debug("Read unkeyed list node");
            node = Builders.unkeyedListBuilder().withNodeIdentifier(identifier).withValue(getUnkeyedListChildren()).build();

        } else if (nodeType.equals(NodeTypes.UNKEYED_LIST_ITEM)) {
            LOG.debug("Read unkeyed list item node");
            node = Builders.unkeyedListEntryBuilder().withNodeIdentifier(identifier).withValue(getDataContainerChildren()).build();

        } else if (nodeType.equals(NodeTypes.CONTAINER_NODE)) {
            LOG.debug("Read container node");
            node = Builders.containerBuilder().withNodeIdentifier(identifier).withValue(getDataContainerChildren()).build();

        } else if (nodeType.equals(NodeTypes.LEAF_SET)) {
            LOG.debug("Read leaf set node");
            node = Builders.leafSetBuilder().withNodeIdentifier(identifier).withValue(getLeafSetChildren()).build();
        }
        return node;
    }

    private List<LeafSetEntryNode<Object>> getLeafSetChildren()
        throws URISyntaxException, IOException, ClassNotFoundException {
        LOG.debug("Reading children of leaf set");
        List<LeafSetEntryNode<Object>> children = new ArrayList<>(1);
        while(true) {
            LeafSetEntryNode<Object> child = (LeafSetEntryNode<Object>)readNormalizedNode();
            if(child != null) {
                children.add(child);
            } else {
                break;
            }
        }
        return children;
    }

    private List<UnkeyedListEntryNode> getUnkeyedListChildren()
        throws URISyntaxException, IOException, ClassNotFoundException {
        LOG.debug("Reading children of unkeyed list");
        List<UnkeyedListEntryNode> children = new ArrayList<>(1);
        while(true) {
            UnkeyedListEntryNode child = (UnkeyedListEntryNode)readNormalizedNode();
            if(child != null) {
                children.add(child);
            } else {
                break;
            }
        }
        return children;
    }

    private List<DataContainerChild<? extends YangInstanceIdentifier.PathArgument, ?>>
    getDataContainerChildren() throws URISyntaxException, IOException, ClassNotFoundException {
        LOG.debug("Reading data container (leaf nodes) nodes");

        List<DataContainerChild<? extends YangInstanceIdentifier.PathArgument, ?>> children = new ArrayList<>(1);

        while(true) {
            DataContainerChild<? extends YangInstanceIdentifier.PathArgument, ?> child =
                (DataContainerChild<? extends YangInstanceIdentifier.PathArgument, ?>) readNormalizedNode();
            if(child != null) {
                children.add(child);
            } else {
                break;
            }
        }
        return children;
    }

    private List<MapEntryNode> getMapNodeChildren() throws URISyntaxException, IOException, ClassNotFoundException {
        LOG.debug("Reading map node children");

        List<MapEntryNode> children = new ArrayList<>(1);
        while(true) {
            MapEntryNode child = (MapEntryNode)readNormalizedNode();
            if(child != null) {
                children.add(child);
            } else {
                break;
            }
        }
        return children;
    }


    @Override
    public void close() throws IOException {
        reader.close();
    }

}
