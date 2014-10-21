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
import org.opendaylight.controller.cluster.datastore.node.utils.serialization.PathArgumentType;
import org.opendaylight.controller.cluster.datastore.node.utils.serialization.ValueType;
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
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * DDSNormalizedNodeStreamReader reads the byte stream and constructs the normalized node including its children nodes.
 * This process goes in recursive manner, where each NodeTypes object signifies the start of the object, except END_NODE.
 * If a node can have children, then that node's end is calculated based on appearance of END_NODE.
 *
 */

public class DDSNormalizedNodeStreamReader implements NormalizedNodeStreamReader {

    private DataInputStream reader;

    private static final Logger LOG = LoggerFactory.getLogger(DDSNormalizedNodeStreamReader.class);

    private Map<Integer, String> namespaceMap = new HashMap<>();
    private Map<Integer, String> localNameMap = new HashMap<>();
    private Map<Integer, String> revisionMap = new HashMap<>();
    private static final String REVISION_ARG = "?revision=";

    public DDSNormalizedNodeStreamReader(InputStream stream) throws IOException {
        Preconditions.checkNotNull(stream);
        reader = new DataInputStream(stream);
    }


    public NormalizedNode<?, ?> readNormalizedNode() throws IOException, ClassNotFoundException, URISyntaxException {
        NormalizedNode<?, ?> node = null;

        // each node should start with a byte
        byte nodeType = reader.readByte();

        if(nodeType == NodeTypes.END_NODE.ordinal()) {
            LOG.debug("End node reached. return");
            return null;
        }
        else if(nodeType == NodeTypes.AUGMENTATION_NODE.ordinal()) {
            LOG.debug("Reading augmentation node. will create augmentation identifier");

            YangInstanceIdentifier.AugmentationIdentifier identifier =
                new YangInstanceIdentifier.AugmentationIdentifier(readQNameSet());
            DataContainerNodeBuilder<YangInstanceIdentifier.AugmentationIdentifier, AugmentationNode> augmentationBuilder =
                Builders.augmentationBuilder().withNodeIdentifier(identifier);
            augmentationBuilder = addDataContainerChildren(augmentationBuilder);
            node = augmentationBuilder.build();

        } else {
            QName qName = readQName();

            if(nodeType == NodeTypes.LEAF_SET_ENTRY_NODE.ordinal()) {
                LOG.debug("Reading leaf set entry node. Will create NodeWithValue instance identifier");

                // Read the object value
                Object value = readObject();

                YangInstanceIdentifier.NodeWithValue nodeWithValue = new YangInstanceIdentifier.NodeWithValue(qName, value);
                node =  Builders.leafSetEntryBuilder().withNodeIdentifier(nodeWithValue).withValue(value).build();

            } else if(nodeType == NodeTypes.MAP_ENTRY_NODE.ordinal()) {
                LOG.debug("Reading map entry node. Will create node identifier with predicates.");

                YangInstanceIdentifier.NodeIdentifierWithPredicates nodeIdentifier =
                    new YangInstanceIdentifier.NodeIdentifierWithPredicates(qName, readKeyValueMap());
                DataContainerNodeAttrBuilder<YangInstanceIdentifier.NodeIdentifierWithPredicates, MapEntryNode> mapEntryBuilder
                    = Builders.mapEntryBuilder().withNodeIdentifier(nodeIdentifier);

                mapEntryBuilder = (DataContainerNodeAttrBuilder<YangInstanceIdentifier.NodeIdentifierWithPredicates,
                    MapEntryNode>)addDataContainerChildren(mapEntryBuilder);
                node = mapEntryBuilder.build();

            } else {
                LOG.debug("Creating standard node identifier. ");
                YangInstanceIdentifier.NodeIdentifier identifier = new YangInstanceIdentifier.NodeIdentifier(qName);
                node = readNodeIdentifierDependentNode(nodeType, identifier);

            }
        }
        return node;
    }

    private NormalizedNode<?, ?> readNodeIdentifierDependentNode(byte nodeType, YangInstanceIdentifier.NodeIdentifier identifier)
        throws IOException, ClassNotFoundException, URISyntaxException {

        switch(NodeTypes.values()[nodeType]) {
            case LEAF_NODE :
                LOG.debug("Read leaf node");
                // Read the object value
                NormalizedNodeAttrBuilder leafBuilder = Builders.leafBuilder();
                return leafBuilder.withNodeIdentifier(identifier).withValue(readObject()).build();

            case ANY_XML_NODE :
                LOG.debug("Read xml node");
                Node value = (Node) readObject();
                return Builders.anyXmlBuilder().withValue(value).build();

            case MAP_NODE :
                LOG.debug("Read map node");
                CollectionNodeBuilder<MapEntryNode, MapNode> mapBuilder = Builders.mapBuilder().withNodeIdentifier(identifier);
                mapBuilder = addMapNodeChildren(mapBuilder);
                return mapBuilder.build();

            case CHOICE_NODE :
                LOG.debug("Read choice node");
                DataContainerNodeBuilder<YangInstanceIdentifier.NodeIdentifier, ChoiceNode> choiceBuilder =
                    Builders.choiceBuilder().withNodeIdentifier(identifier);
                choiceBuilder = addDataContainerChildren(choiceBuilder);
                return choiceBuilder.build();

            case ORDERED_MAP_NODE :
                LOG.debug("Reading ordered map node");
                CollectionNodeBuilder<MapEntryNode, OrderedMapNode> orderedMapBuilder =
                    Builders.orderedMapBuilder().withNodeIdentifier(identifier);
                orderedMapBuilder = addMapNodeChildren(orderedMapBuilder);
                return orderedMapBuilder.build();

            case UNKEYED_LIST :
                LOG.debug("Read unkeyed list node");
                CollectionNodeBuilder<UnkeyedListEntryNode, UnkeyedListNode> unkeyedListBuilder =
                    Builders.unkeyedListBuilder().withNodeIdentifier(identifier);
                unkeyedListBuilder = addUnkeyedListChildren(unkeyedListBuilder);
                return unkeyedListBuilder.build();

            case UNKEYED_LIST_ITEM :
                LOG.debug("Read unkeyed list item node");
                DataContainerNodeAttrBuilder<YangInstanceIdentifier.NodeIdentifier, UnkeyedListEntryNode> unkeyedListEntryBuilder
                    = Builders.unkeyedListEntryBuilder().withNodeIdentifier(identifier);

                unkeyedListEntryBuilder = (DataContainerNodeAttrBuilder<YangInstanceIdentifier.NodeIdentifier, UnkeyedListEntryNode>)
                    addDataContainerChildren(unkeyedListEntryBuilder);
                return unkeyedListEntryBuilder.build();

            case CONTAINER_NODE :
                LOG.debug("Read container node");
                DataContainerNodeAttrBuilder<YangInstanceIdentifier.NodeIdentifier, ContainerNode> containerBuilder =
                    Builders.containerBuilder().withNodeIdentifier(identifier);

                containerBuilder = (DataContainerNodeAttrBuilder<YangInstanceIdentifier.NodeIdentifier, ContainerNode>)
                    addDataContainerChildren(containerBuilder);
                return containerBuilder.build();

            case LEAF_SET :
                LOG.debug("Read leaf set node");
                ListNodeBuilder<Object, LeafSetEntryNode<Object>> leafSetBuilder =
                    Builders.leafSetBuilder().withNodeIdentifier(identifier);
                leafSetBuilder = addLeafSetChildren(leafSetBuilder);
                return leafSetBuilder.build();

            default :
                return null;
        }
    }

    private QName readQName() throws IOException {
        // Read in the same sequence of writing
        String localName = readLocalName();
        String namespace = readNamespace();
        String revision = readRevision();
        String qName;
        // Not using stringbuilder as compiler optimizes string concatenation of +
        if(revision != null){
            qName = "(" + namespace+ REVISION_ARG + revision + ")" +localName;
        } else {
            qName = "(" + namespace + ")" +localName;
        }

        return QNameFactory.create(qName);
    }

    private String readLocalName() throws IOException {
        return readCodedString(localNameMap);
    }

    private String readNamespace() throws IOException {
        return readCodedString(namespaceMap);
    }

    private String readRevision() throws IOException {
        return readCodedString(revisionMap);
    }


    private String readCodedString(Map<Integer, String> codeMap) throws IOException {
        boolean readFromMap = reader.readBoolean();
        if(readFromMap) {
            return codeMap.get(reader.readInt());
        } else {
            String value = reader.readUTF();
            if(value != null) {
                codeMap.put(codeMap.size(), value);
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
        switch(ValueType.values()[objectType]) {
            case BITS_TYPE:
                return readObjSet();

            case BOOL_TYPE :
                return reader.readBoolean();

            case BYTE_TYPE :
                return reader.readByte();

            case INT_TYPE :
                return reader.readInt();

            case LONG_TYPE :
                return reader.readLong();

            case QNAME_TYPE :
                return readQName();

            case SHORT_TYPE :
                return reader.readShort();

            case STRING_TYPE :
                return reader.readUTF();

            case BIG_DECIMAL_TYPE :
                return new BigDecimal(reader.readUTF());

            case BIG_INTEGER_TYPE :
                return new BigInteger(reader.readUTF());

            case YANG_IDENTIFIER_TYPE :
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
            children.add(reader.readUTF());
        }
        return children;
    }

    private YangInstanceIdentifier.PathArgument readPathArgument() throws IOException {
        // read Type
        int type = reader.readByte();

        switch(PathArgumentType.values()[type]) {

            case AUGMENTATION_IDENTIFIER :
                return new YangInstanceIdentifier.AugmentationIdentifier(readQNameSet());

            case NODE_IDENTIFIER :
            return new YangInstanceIdentifier.NodeIdentifier(readQName());

            case NODE_IDENTIFIER_WITH_PREDICATES :
            return new YangInstanceIdentifier.NodeIdentifierWithPredicates(readQName(), readKeyValueMap());

            case NODE_IDENTIFIER_WITH_VALUE :
            return new YangInstanceIdentifier.NodeWithValue(readQName(), readObject());

            default :
                return null;
        }
    }

    private ListNodeBuilder<Object, LeafSetEntryNode<Object>> addLeafSetChildren(ListNodeBuilder<Object,
        LeafSetEntryNode<Object>> builder)
        throws URISyntaxException, IOException, ClassNotFoundException {

        LOG.debug("Reading children of leaf set");
        LeafSetEntryNode<Object> child = (LeafSetEntryNode<Object>)readNormalizedNode();

        while(child != null) {
            builder.withChild(child);
            child = (LeafSetEntryNode<Object>)readNormalizedNode();
        }
        return builder;
    }

    private CollectionNodeBuilder<UnkeyedListEntryNode, UnkeyedListNode> addUnkeyedListChildren(
        CollectionNodeBuilder<UnkeyedListEntryNode, UnkeyedListNode> builder)
        throws URISyntaxException, IOException, ClassNotFoundException {

        LOG.debug("Reading children of unkeyed list");
        UnkeyedListEntryNode child = (UnkeyedListEntryNode)readNormalizedNode();

        while(child != null) {
            builder.withChild(child);
            child = (UnkeyedListEntryNode)readNormalizedNode();
        }
        return builder;
    }

    private DataContainerNodeBuilder addDataContainerChildren(DataContainerNodeBuilder builder)
        throws URISyntaxException, IOException, ClassNotFoundException {
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
        throws URISyntaxException, IOException, ClassNotFoundException {
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
