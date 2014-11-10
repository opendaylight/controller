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
import org.opendaylight.controller.cluster.datastore.node.utils.QNameFactory;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.Node;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.AugmentationNode;
import org.opendaylight.yangtools.yang.data.api.schema.ChoiceNode;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.api.schema.LeafSetEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.OrderedMapNode;
import org.opendaylight.yangtools.yang.data.api.schema.UnkeyedListEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.UnkeyedListNode;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.CollectionNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.DataContainerNodeAttrBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.DataContainerNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.ListNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.NormalizedNodeAttrBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * NormalizedNodeInputStreamReader reads the byte stream and constructs the normalized node including its children nodes.
 * This process goes in recursive manner, where each NodeTypes object signifies the start of the object, except END_NODE.
 * If a node can have children, then that node's end is calculated based on appearance of END_NODE.
 *
 */

public class NormalizedNodeInputStreamReader implements NormalizedNodeStreamReader {

    private static final Logger LOG = LoggerFactory.getLogger(NormalizedNodeInputStreamReader.class);

    private static final String REVISION_ARG = "?revision=";

    private final DataInputStream reader;

    private final Map<Integer, String> codedStringMap = new HashMap<>();

    private QName lastLeafSetQName;

    public NormalizedNodeInputStreamReader(InputStream stream) throws IOException {
        Preconditions.checkNotNull(stream);
        reader = new DataInputStream(stream);
    }

    @Override
    public NormalizedNode<?, ?> readNormalizedNode() throws IOException {
        NormalizedNode<?, ?> node = null;

        // each node should start with a byte
        byte nodeType = reader.readByte();

        if(nodeType == NodeTypes.END_NODE) {
            LOG.debug("End node reached. return");
            return null;
        }
        else if(nodeType == NodeTypes.AUGMENTATION_NODE) {
            LOG.debug("Reading augmentation node. will create augmentation identifier");

            YangInstanceIdentifier.AugmentationIdentifier identifier =
                new YangInstanceIdentifier.AugmentationIdentifier(readQNameSet());
            DataContainerNodeBuilder<YangInstanceIdentifier.AugmentationIdentifier, AugmentationNode> augmentationBuilder =
                Builders.augmentationBuilder().withNodeIdentifier(identifier);
            augmentationBuilder = addDataContainerChildren(augmentationBuilder);
            node = augmentationBuilder.build();

        } else {
            if(nodeType == NodeTypes.LEAF_SET_ENTRY_NODE) {
                LOG.debug("Reading leaf set entry node. Will create NodeWithValue instance identifier");

                // Read the object value
                Object value = readObject();

                YangInstanceIdentifier.NodeWithValue nodeWithValue = new YangInstanceIdentifier.NodeWithValue(
                        lastLeafSetQName, value);
                node =  Builders.leafSetEntryBuilder().withNodeIdentifier(nodeWithValue).
                        withValue(value).build();

            } else if(nodeType == NodeTypes.MAP_ENTRY_NODE) {
                LOG.debug("Reading map entry node. Will create node identifier with predicates.");

                QName qName = readQName();
                YangInstanceIdentifier.NodeIdentifierWithPredicates nodeIdentifier =
                    new YangInstanceIdentifier.NodeIdentifierWithPredicates(qName, readKeyValueMap());
                DataContainerNodeAttrBuilder<YangInstanceIdentifier.NodeIdentifierWithPredicates, MapEntryNode> mapEntryBuilder
                    = Builders.mapEntryBuilder().withNodeIdentifier(nodeIdentifier);

                mapEntryBuilder = (DataContainerNodeAttrBuilder<YangInstanceIdentifier.NodeIdentifierWithPredicates,
                    MapEntryNode>)addDataContainerChildren(mapEntryBuilder);
                node = mapEntryBuilder.build();

            } else {
                LOG.debug("Creating standard node identifier. ");

                QName qName = readQName();
                YangInstanceIdentifier.NodeIdentifier identifier = new YangInstanceIdentifier.NodeIdentifier(qName);
                node = readNodeIdentifierDependentNode(nodeType, identifier);

            }
        }
        return node;
    }

    private NormalizedNode<?, ?> readNodeIdentifierDependentNode(byte nodeType, YangInstanceIdentifier.NodeIdentifier identifier)
        throws IOException {

        switch(nodeType) {
            case NodeTypes.LEAF_NODE :
                LOG.debug("Read leaf node");
                // Read the object value
                NormalizedNodeAttrBuilder leafBuilder = Builders.leafBuilder();
                return leafBuilder.withNodeIdentifier(identifier).withValue(readObject()).build();

            case NodeTypes.ANY_XML_NODE :
                LOG.debug("Read xml node");
                Node<?> value = (Node<?>) readObject();
                return Builders.anyXmlBuilder().withValue(value).build();

            case NodeTypes.MAP_NODE :
                LOG.debug("Read map node");
                CollectionNodeBuilder<MapEntryNode, MapNode> mapBuilder = Builders.mapBuilder().withNodeIdentifier(identifier);
                mapBuilder = addMapNodeChildren(mapBuilder);
                return mapBuilder.build();

            case NodeTypes.CHOICE_NODE :
                LOG.debug("Read choice node");
                DataContainerNodeBuilder<YangInstanceIdentifier.NodeIdentifier, ChoiceNode> choiceBuilder =
                    Builders.choiceBuilder().withNodeIdentifier(identifier);
                choiceBuilder = addDataContainerChildren(choiceBuilder);
                return choiceBuilder.build();

            case NodeTypes.ORDERED_MAP_NODE :
                LOG.debug("Reading ordered map node");
                CollectionNodeBuilder<MapEntryNode, OrderedMapNode> orderedMapBuilder =
                    Builders.orderedMapBuilder().withNodeIdentifier(identifier);
                orderedMapBuilder = addMapNodeChildren(orderedMapBuilder);
                return orderedMapBuilder.build();

            case NodeTypes.UNKEYED_LIST :
                LOG.debug("Read unkeyed list node");
                CollectionNodeBuilder<UnkeyedListEntryNode, UnkeyedListNode> unkeyedListBuilder =
                    Builders.unkeyedListBuilder().withNodeIdentifier(identifier);
                unkeyedListBuilder = addUnkeyedListChildren(unkeyedListBuilder);
                return unkeyedListBuilder.build();

            case NodeTypes.UNKEYED_LIST_ITEM :
                LOG.debug("Read unkeyed list item node");
                DataContainerNodeAttrBuilder<YangInstanceIdentifier.NodeIdentifier, UnkeyedListEntryNode> unkeyedListEntryBuilder
                    = Builders.unkeyedListEntryBuilder().withNodeIdentifier(identifier);

                unkeyedListEntryBuilder = (DataContainerNodeAttrBuilder<YangInstanceIdentifier.NodeIdentifier, UnkeyedListEntryNode>)
                    addDataContainerChildren(unkeyedListEntryBuilder);
                return unkeyedListEntryBuilder.build();

            case NodeTypes.CONTAINER_NODE :
                LOG.debug("Read container node");
                DataContainerNodeAttrBuilder<YangInstanceIdentifier.NodeIdentifier, ContainerNode> containerBuilder =
                    Builders.containerBuilder().withNodeIdentifier(identifier);

                containerBuilder = (DataContainerNodeAttrBuilder<YangInstanceIdentifier.NodeIdentifier, ContainerNode>)
                    addDataContainerChildren(containerBuilder);
                return containerBuilder.build();

            case NodeTypes.LEAF_SET :
                LOG.debug("Read leaf set node");
                ListNodeBuilder<Object, LeafSetEntryNode<Object>> leafSetBuilder =
                    Builders.leafSetBuilder().withNodeIdentifier(identifier);
                leafSetBuilder = addLeafSetChildren(identifier.getNodeType(), leafSetBuilder);
                return leafSetBuilder.build();

            case NodeTypes.ORDERED_LEAF_SET:
                LOG.debug("Read leaf set node");
                ListNodeBuilder<Object, LeafSetEntryNode<Object>> orderedLeafSetBuilder =
                        Builders.orderedLeafSetBuilder().withNodeIdentifier(identifier);
                orderedLeafSetBuilder = addLeafSetChildren(identifier.getNodeType(), orderedLeafSetBuilder);
                return orderedLeafSetBuilder.build();

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
        // Not using stringbuilder as compiler optimizes string concatenation of +
        if(revision != null){
            qName = "(" + namespace+ REVISION_ARG + revision + ")" +localName;
        } else {
            qName = "(" + namespace + ")" +localName;
        }

        return QNameFactory.create(qName);
    }


    private String readCodedString() throws IOException {
        boolean readFromMap = reader.readBoolean();
        if(readFromMap) {
            return codedStringMap.get(reader.readInt());
        } else {
            String value = reader.readUTF();
            if(value != null) {
                codedStringMap.put(Integer.valueOf(codedStringMap.size()), value);
            }
            return value;
        }
    }

    private Set<QName> readQNameSet() throws IOException{
        // Read the children count
        int count = reader.readInt();
        Set<QName> children = new HashSet<>(count);
        for(int i = 0; i<count; i++) {
            children.add(readQName());
        }
        return children;
    }

    private Map<QName, Object> readKeyValueMap() throws IOException {
        int count = reader.readInt();
        Map<QName, Object> keyValueMap = new HashMap<>(count);

        for(int i = 0; i<count; i++) {
            keyValueMap.put(readQName(), readObject());
        }

        return keyValueMap;
    }

    private Object readObject() throws IOException {
        byte objectType = reader.readByte();
        switch(objectType) {
            case ValueTypes.BITS_TYPE:
                return readObjSet();

            case ValueTypes.BOOL_TYPE :
                return reader.readBoolean();

            case ValueTypes.BYTE_TYPE :
                return reader.readByte();

            case ValueTypes.INT_TYPE :
                return reader.readInt();

            case ValueTypes.LONG_TYPE :
                return reader.readLong();

            case ValueTypes.QNAME_TYPE :
                return readQName();

            case ValueTypes.SHORT_TYPE :
                return reader.readShort();

            case ValueTypes.STRING_TYPE :
                return reader.readUTF();

            case ValueTypes.BIG_DECIMAL_TYPE :
                return new BigDecimal(reader.readUTF());

            case ValueTypes.BIG_INTEGER_TYPE :
                return new BigInteger(reader.readUTF());

            case ValueTypes.YANG_IDENTIFIER_TYPE :
                int size = reader.readInt();

                List<YangInstanceIdentifier.PathArgument> pathArguments = new ArrayList<>(size);

                for(int i=0; i<size; i++) {
                    pathArguments.add(readPathArgument());
                }
                return YangInstanceIdentifier.create(pathArguments);

            default :
                return null;
        }
    }

    private Set<String> readObjSet() throws IOException {
        int count = reader.readInt();
        Set<String> children = new HashSet<>(count);
        for(int i = 0; i<count; i++) {
            children.add(readCodedString());
        }
        return children;
    }

    private YangInstanceIdentifier.PathArgument readPathArgument() throws IOException {
        // read Type
        int type = reader.readByte();

        switch(type) {

            case PathArgumentTypes.AUGMENTATION_IDENTIFIER :
                return new YangInstanceIdentifier.AugmentationIdentifier(readQNameSet());

            case PathArgumentTypes.NODE_IDENTIFIER :
            return new YangInstanceIdentifier.NodeIdentifier(readQName());

            case PathArgumentTypes.NODE_IDENTIFIER_WITH_PREDICATES :
            return new YangInstanceIdentifier.NodeIdentifierWithPredicates(readQName(), readKeyValueMap());

            case PathArgumentTypes.NODE_IDENTIFIER_WITH_VALUE :
            return new YangInstanceIdentifier.NodeWithValue(readQName(), readObject());

            default :
                return null;
        }
    }

    private ListNodeBuilder<Object, LeafSetEntryNode<Object>> addLeafSetChildren(QName nodeType,
            ListNodeBuilder<Object, LeafSetEntryNode<Object>> builder)
        throws IOException {

        LOG.debug("Reading children of leaf set");

        lastLeafSetQName = nodeType;

        LeafSetEntryNode<Object> child = (LeafSetEntryNode<Object>)readNormalizedNode();

        while(child != null) {
            builder.withChild(child);
            child = (LeafSetEntryNode<Object>)readNormalizedNode();
        }
        return builder;
    }

    private CollectionNodeBuilder<UnkeyedListEntryNode, UnkeyedListNode> addUnkeyedListChildren(
        CollectionNodeBuilder<UnkeyedListEntryNode, UnkeyedListNode> builder)
        throws IOException{

        LOG.debug("Reading children of unkeyed list");
        UnkeyedListEntryNode child = (UnkeyedListEntryNode)readNormalizedNode();

        while(child != null) {
            builder.withChild(child);
            child = (UnkeyedListEntryNode)readNormalizedNode();
        }
        return builder;
    }

    private DataContainerNodeBuilder addDataContainerChildren(DataContainerNodeBuilder builder)
        throws IOException {
        LOG.debug("Reading data container (leaf nodes) nodes");

        DataContainerChild<? extends YangInstanceIdentifier.PathArgument, ?> child =
            (DataContainerChild<? extends YangInstanceIdentifier.PathArgument, ?>) readNormalizedNode();

        while(child != null) {
            builder.withChild(child);
            child =
                (DataContainerChild<? extends YangInstanceIdentifier.PathArgument, ?>) readNormalizedNode();
        }
        return builder;
    }


    private CollectionNodeBuilder addMapNodeChildren(CollectionNodeBuilder builder)
        throws IOException {
        LOG.debug("Reading map node children");
        MapEntryNode child = (MapEntryNode)readNormalizedNode();

        while(child != null){
            builder.withChild(child);
            child = (MapEntryNode)readNormalizedNode();
        }

        return builder;
    }


    @Override
    public void close() throws IOException {
        reader.close();
    }

}
