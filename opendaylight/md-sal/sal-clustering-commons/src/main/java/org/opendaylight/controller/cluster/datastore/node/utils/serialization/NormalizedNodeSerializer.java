/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.node.utils.serialization;

import com.google.common.base.Preconditions;
import org.opendaylight.controller.protobuff.messages.common.NormalizedNodeMessages;
import org.opendaylight.yangtools.yang.common.SimpleDateFormatUtil;
import org.opendaylight.yangtools.yang.data.api.Node;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.AnyXmlNode;
import org.opendaylight.yangtools.yang.data.api.schema.AugmentationNode;
import org.opendaylight.yangtools.yang.data.api.schema.ChoiceNode;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.api.schema.LeafNode;
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

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.opendaylight.controller.cluster.datastore.node.utils.serialization.NormalizedNodeType.ANY_XML_NODE_TYPE;
import static org.opendaylight.controller.cluster.datastore.node.utils.serialization.NormalizedNodeType.AUGMENTATION_NODE_TYPE;
import static org.opendaylight.controller.cluster.datastore.node.utils.serialization.NormalizedNodeType.CHOICE_NODE_TYPE;
import static org.opendaylight.controller.cluster.datastore.node.utils.serialization.NormalizedNodeType.CONTAINER_NODE_TYPE;
import static org.opendaylight.controller.cluster.datastore.node.utils.serialization.NormalizedNodeType.LEAF_NODE_TYPE;
import static org.opendaylight.controller.cluster.datastore.node.utils.serialization.NormalizedNodeType.LEAF_SET_ENTRY_NODE_TYPE;
import static org.opendaylight.controller.cluster.datastore.node.utils.serialization.NormalizedNodeType.LEAF_SET_NODE_TYPE;
import static org.opendaylight.controller.cluster.datastore.node.utils.serialization.NormalizedNodeType.MAP_ENTRY_NODE_TYPE;
import static org.opendaylight.controller.cluster.datastore.node.utils.serialization.NormalizedNodeType.MAP_NODE_TYPE;
import static org.opendaylight.controller.cluster.datastore.node.utils.serialization.NormalizedNodeType.ORDERED_LEAF_SET_NODE_TYPE;
import static org.opendaylight.controller.cluster.datastore.node.utils.serialization.NormalizedNodeType.ORDERED_MAP_NODE_TYPE;
import static org.opendaylight.controller.cluster.datastore.node.utils.serialization.NormalizedNodeType.UNKEYED_LIST_ENTRY_NODE_TYPE;
import static org.opendaylight.controller.cluster.datastore.node.utils.serialization.NormalizedNodeType.UNKEYED_LIST_NODE_TYPE;
import static org.opendaylight.controller.cluster.datastore.node.utils.serialization.NormalizedNodeType.getSerializableNodeType;

public class NormalizedNodeSerializer {

    public static NormalizedNodeMessages.Node serialize(NormalizedNode node){
        Preconditions.checkNotNull(node, "node should not be null");
        return new Serializer(node).serialize();
    }


    public static NormalizedNode deSerialize(NormalizedNodeMessages.Node node){
        return new DeSerializer(node).deSerialize();
    }

    public static YangInstanceIdentifier.PathArgument deSerialize(NormalizedNodeMessages.Node node, NormalizedNodeMessages.PathArgument pathArgument){
        Preconditions.checkNotNull(node, "node should not be null");
        Preconditions.checkNotNull(pathArgument, "pathArgument should not be null");
        return new DeSerializer(node).deSerialize(pathArgument);
    }

    private static class Serializer implements NormalizedNodeSerializationContext {

        private final NormalizedNode node;

        // These 3 maps and lists are here to make encoding efficient
        // We could have also solved the problem of encoding by maintaining
        // one list of strings but then we would be forced to convert uri's and
        // dates to string every time we encountered a QName to check if
        // it was there in the list. That would be far less efficient than the
        // approach of having specific typed maps for this purpose.

        private final Map<URI, Integer> namespaceMap = new HashMap<>();
        private final Map<Date, Integer> revisionMap = new HashMap<>();
        private final Map<String, Integer> localNameMap = new HashMap<>();
        private final List<String> namespaces = new ArrayList<>();
        private final List<String> revisions = new ArrayList<>();
        private final List<String> localNames = new ArrayList<>();

        private Serializer(NormalizedNode node) {
            this.node = node;
        }

        private NormalizedNodeMessages.Node serialize() {
            return this.serialize(node).addAllNamespace(
                namespaces).addAllRevision(revisions).addAllLocalName(
                localNames).build();
        }

        private NormalizedNodeMessages.Node.Builder serialize(
            NormalizedNode node) {
            NormalizedNodeMessages.Node.Builder builder =
                NormalizedNodeMessages.Node.newBuilder();

            builder.setPathArgument(PathArgumentSerializer.serialize(this, node.getIdentifier()));
            Integer nodeType = getSerializableNodeType(node);
            builder.setIntType(nodeType);
            Object value = node.getValue();

            // We need to do a specific check of the type of the node here
            // because if we looked at the value type alone we will not be
            // able to distinguish if the value was supposed to be added
            // as a child or whether the value should be added as a value of the
            // node.
            // One use case where this check is neccessary when you have a node
            // with a bits value. In that case the value of the node is a Set
            // which is also a Collection. Without the following check being
            // done first the code would flow into the Collection if condition
            // and the Set would be added as child nodes
            if(nodeType == NormalizedNodeType.LEAF_NODE_TYPE ||
               nodeType == NormalizedNodeType.LEAF_SET_ENTRY_NODE_TYPE){

                ValueSerializer.serialize(builder, this, value);

            } else if (value instanceof Collection) {
                Collection collection = (Collection) value;

                for (Object o : collection) {
                    if (o instanceof NormalizedNode) {
                        builder.addChild(serialize((NormalizedNode) o));
                    }
                }
            } else if (value instanceof Iterable) {
                Iterable iterable = (Iterable) value;

                for (Object o : iterable) {
                    if (o instanceof NormalizedNode) {
                        builder.addChild(serialize((NormalizedNode) o));
                    }
                }

            } else if (value instanceof NormalizedNode) {

                builder.addChild(serialize((NormalizedNode) value));

            } else {

                ValueSerializer.serialize(builder, this, value);
            }

            return builder;
        }


        @Override public int addNamespace(URI namespace) {
            int namespaceInt = getNamespace(namespace);

            if(namespaceInt == -1) {
                namespaceInt = getNamespaceCount();
                namespaces.add(namespace.toString());
                namespaceMap.put(namespace, namespaceInt);
            }
            return namespaceInt;
        }

        @Override public int addRevision(Date revision) {
            if(revision == null){
                return -1;
            }

            int revisionInt = getRevision(revision);
            if(revisionInt == -1) {
                String s =
                    SimpleDateFormatUtil.getRevisionFormat().format(revision);
                revisionInt = getRevisionCount();
                revisions.add(s);
                revisionMap.put(revision, revisionInt);
            }
            return revisionInt;
        }

        @Override public int addLocalName(String localName) {
            int localNameInt = getLocalName(localName);
            if(localNameInt == -1) {
                localNameInt = getLocalNameCount();
                localNames.add(localName.toString());
                localNameMap.put(localName, localNameInt);
            }
            return localNameInt;

        }

        @Override public int getNamespace(URI namespace) {
            if(namespaceMap.containsKey(namespace)){
                return namespaceMap.get(namespace);
            }
            return -1;
        }

        @Override public int getRevision(Date revision) {
            if(revisionMap.containsKey(revision)){
                return revisionMap.get(revision);
            }
            return -1;

        }

        @Override public int getLocalName(String localName) {
            if(localNameMap.containsKey(localName)){
                return localNameMap.get(localName);
            }
            return -1;

        }

        public int getNamespaceCount() {
            return namespaces.size();
        }

        public int getRevisionCount() {
            return revisions.size();
        }

        public int getLocalNameCount() {
            return localNames.size();
        }


    }

    private static class DeSerializer implements NormalizedNodeDeSerializationContext {
        private static Map<Integer, DeSerializationFunction>
            deSerializationFunctions = new HashMap<>();

        static {
            deSerializationFunctions.put(CONTAINER_NODE_TYPE,
                new DeSerializationFunction() {
                    @Override public NormalizedNode apply(
                        DeSerializer deSerializer,
                        NormalizedNodeMessages.Node node) {
                        DataContainerNodeAttrBuilder<YangInstanceIdentifier.NodeIdentifier, ContainerNode>
                            builder = Builders.containerBuilder();

                        builder
                            .withNodeIdentifier(deSerializer.toNodeIdentifier(
                                node.getPathArgument()));

                        return deSerializer.buildDataContainer(builder, node);

                    }

                });

            deSerializationFunctions.put(LEAF_NODE_TYPE,
                new DeSerializationFunction() {
                    @Override public NormalizedNode apply(
                        DeSerializer deSerializer,
                        NormalizedNodeMessages.Node node) {
                        NormalizedNodeAttrBuilder<YangInstanceIdentifier.NodeIdentifier, Object, LeafNode<Object>>
                            builder = Builders.leafBuilder();

                        builder
                            .withNodeIdentifier(deSerializer.toNodeIdentifier(
                                node.getPathArgument()));

                        return deSerializer.buildNormalizedNode(builder, node);

                    }
                });

            deSerializationFunctions.put(MAP_NODE_TYPE,
                new DeSerializationFunction() {
                    @Override public NormalizedNode apply(
                        DeSerializer deSerializer,
                        NormalizedNodeMessages.Node node) {
                        CollectionNodeBuilder<MapEntryNode, MapNode>
                            builder = Builders.mapBuilder();

                        return deSerializer.buildCollectionNode(builder, node);
                    }
                });

            deSerializationFunctions.put(MAP_ENTRY_NODE_TYPE,
                new DeSerializationFunction() {
                    @Override public NormalizedNode apply(
                        DeSerializer deSerializer,
                        NormalizedNodeMessages.Node node) {
                        DataContainerNodeAttrBuilder<YangInstanceIdentifier.NodeIdentifierWithPredicates, MapEntryNode>
                            builder = Builders.mapEntryBuilder();

                        builder.withNodeIdentifier(deSerializer.toNodeIdentifierWithPredicates(
                            node.getPathArgument()));

                        return deSerializer.buildDataContainer(builder, node);
                    }
                });

            deSerializationFunctions.put(AUGMENTATION_NODE_TYPE,
                new DeSerializationFunction() {
                    @Override public NormalizedNode apply(
                        DeSerializer deSerializer,
                        NormalizedNodeMessages.Node node) {
                        DataContainerNodeBuilder<YangInstanceIdentifier.AugmentationIdentifier, AugmentationNode>
                            builder = Builders.augmentationBuilder();

                        builder.withNodeIdentifier(
                            deSerializer.toAugmentationIdentifier(
                                node.getPathArgument()));

                        return deSerializer.buildDataContainer(builder, node);
                    }
                });

            deSerializationFunctions.put(LEAF_SET_NODE_TYPE,
                new DeSerializationFunction() {
                    @Override public NormalizedNode apply(
                        DeSerializer deSerializer,
                        NormalizedNodeMessages.Node node) {
                        ListNodeBuilder<Object, LeafSetEntryNode<Object>>
                            builder = Builders.leafSetBuilder();

                        return deSerializer.buildListNode(builder, node);
                    }
                });

            deSerializationFunctions.put(LEAF_SET_ENTRY_NODE_TYPE,
                new DeSerializationFunction() {
                    @Override public NormalizedNode apply(
                        DeSerializer deSerializer,
                        NormalizedNodeMessages.Node node) {
                        NormalizedNodeAttrBuilder<YangInstanceIdentifier.NodeWithValue, Object, LeafSetEntryNode<Object>>
                            builder = Builders.leafSetEntryBuilder();

                        builder.withNodeIdentifier(deSerializer.toNodeWithValue(
                            node.getPathArgument()));

                        return deSerializer.buildNormalizedNode(builder, node);
                    }
                });

            deSerializationFunctions.put(CHOICE_NODE_TYPE,
                new DeSerializationFunction() {
                    @Override public NormalizedNode apply(
                        DeSerializer deSerializer,
                        NormalizedNodeMessages.Node node) {
                        DataContainerNodeBuilder<YangInstanceIdentifier.NodeIdentifier, ChoiceNode>
                            builder =
                            Builders.choiceBuilder();

                        builder
                            .withNodeIdentifier(deSerializer.toNodeIdentifier(
                                node.getPathArgument()));

                        return deSerializer.buildDataContainer(builder, node);
                    }
                });

            deSerializationFunctions.put(ORDERED_LEAF_SET_NODE_TYPE,
                new DeSerializationFunction() {
                    @Override public NormalizedNode apply(
                        DeSerializer deSerializer,
                        NormalizedNodeMessages.Node node) {
                        ListNodeBuilder<Object, LeafSetEntryNode<Object>>
                            builder =
                            Builders.orderedLeafSetBuilder();

                        return deSerializer.buildListNode(builder, node);


                    }
                });

            deSerializationFunctions.put(ORDERED_MAP_NODE_TYPE,
                new DeSerializationFunction() {
                    @Override public NormalizedNode apply(
                        DeSerializer deSerializer,
                        NormalizedNodeMessages.Node node) {
                        CollectionNodeBuilder<MapEntryNode, OrderedMapNode>
                            builder =
                            Builders.orderedMapBuilder();

                        return deSerializer.buildCollectionNode(builder, node);
                    }
                });

            deSerializationFunctions.put(UNKEYED_LIST_NODE_TYPE,
                new DeSerializationFunction() {
                    @Override public NormalizedNode apply(
                        DeSerializer deSerializer,
                        NormalizedNodeMessages.Node node) {
                        CollectionNodeBuilder<UnkeyedListEntryNode, UnkeyedListNode>
                            builder =
                            Builders.unkeyedListBuilder();

                        return deSerializer.buildCollectionNode(builder, node);
                    }
                });

            deSerializationFunctions.put(UNKEYED_LIST_ENTRY_NODE_TYPE,
                new DeSerializationFunction() {
                    @Override public NormalizedNode apply(
                        DeSerializer deSerializer,
                        NormalizedNodeMessages.Node node) {
                        DataContainerNodeAttrBuilder<YangInstanceIdentifier.NodeIdentifier, UnkeyedListEntryNode>
                            builder =
                            Builders.unkeyedListEntryBuilder();

                        builder
                            .withNodeIdentifier(deSerializer.toNodeIdentifier(
                                node.getPathArgument()));

                        return deSerializer.buildDataContainer(builder, node);
                    }
                });

            deSerializationFunctions.put(ANY_XML_NODE_TYPE,
                new DeSerializationFunction() {

                    @Override public NormalizedNode apply(
                        DeSerializer deSerializer,
                        NormalizedNodeMessages.Node node) {
                        NormalizedNodeAttrBuilder<YangInstanceIdentifier.NodeIdentifier, Node<?>, AnyXmlNode>
                            builder =
                            Builders.anyXmlBuilder();

                        builder
                            .withNodeIdentifier(deSerializer.toNodeIdentifier(
                                node.getPathArgument()));

                        return deSerializer.buildNormalizedNode(builder, node);
                    }
                });

        }

        private final NormalizedNodeMessages.Node node;

        public DeSerializer(NormalizedNodeMessages.Node node){
            this.node = node;
        }

        public NormalizedNode deSerialize(){
            return deSerialize(node);
        }

        private NormalizedNode deSerialize(NormalizedNodeMessages.Node node){
            Preconditions.checkNotNull(node, "node should not be null");
            DeSerializationFunction deSerializationFunction =
                Preconditions.checkNotNull(deSerializationFunctions.get(node.getIntType()), "Unknown type " + node);

            return deSerializationFunction.apply(this, node);
        }


        private NormalizedNode buildCollectionNode(
            CollectionNodeBuilder builder,
            NormalizedNodeMessages.Node node) {

            builder.withNodeIdentifier(toNodeIdentifier(node.getPathArgument()));

            for(NormalizedNodeMessages.Node child : node.getChildList()){
                builder.withChild(deSerialize(child));
            }

            return builder.build();
        }


        private NormalizedNode buildListNode(
            ListNodeBuilder<Object, LeafSetEntryNode<Object>> builder,
            NormalizedNodeMessages.Node node) {
            builder.withNodeIdentifier(toNodeIdentifier(node.getPathArgument()));

            for(NormalizedNodeMessages.Node child : node.getChildList()){
                builder.withChild((LeafSetEntryNode<Object>) deSerialize(child));
            }

            return builder.build();
        }

        private NormalizedNode buildDataContainer(DataContainerNodeBuilder builder, NormalizedNodeMessages.Node node){

            for(NormalizedNodeMessages.Node child : node.getChildList()){
                builder.withChild((DataContainerChild<?, ?>) deSerialize(child));
            }

            //TODO : Also handle attributes

            return builder.build();
        }

        private NormalizedNode buildNormalizedNode(NormalizedNodeAttrBuilder builder, NormalizedNodeMessages.Node node){


            //TODO : Make sure value is of the right type
            builder.withValue(ValueSerializer.deSerialize(this, node));

            //TODO : Also handle attributes

            return builder.build();

        }


        private YangInstanceIdentifier.NodeIdentifierWithPredicates toNodeIdentifierWithPredicates(
            NormalizedNodeMessages.PathArgument path) {
            return (YangInstanceIdentifier.NodeIdentifierWithPredicates) PathArgumentSerializer.deSerialize(this, path);
        }

        private YangInstanceIdentifier.AugmentationIdentifier toAugmentationIdentifier(
            NormalizedNodeMessages.PathArgument path) {
            return (YangInstanceIdentifier.AugmentationIdentifier) PathArgumentSerializer.deSerialize(this, path);
        }

        private YangInstanceIdentifier.NodeWithValue toNodeWithValue(
            NormalizedNodeMessages.PathArgument path) {
            return (YangInstanceIdentifier.NodeWithValue) PathArgumentSerializer.deSerialize(
                this, path);
        }

        private YangInstanceIdentifier.NodeIdentifier toNodeIdentifier(NormalizedNodeMessages.PathArgument path){
            return (YangInstanceIdentifier.NodeIdentifier) PathArgumentSerializer.deSerialize(
                this, path);
        }

        @Override public String getNamespace(int namespace) {
            return node.getNamespace(namespace);
        }

        @Override public String getRevision(int revision) {
            return node.getRevision(revision);
        }

        @Override public String getLocalName(int localName) {
            return node.getLocalName(localName);
        }

        public YangInstanceIdentifier.PathArgument deSerialize(
            NormalizedNodeMessages.PathArgument pathArgument) {
            return PathArgumentSerializer.deSerialize(this, pathArgument);
        }

        private static interface DeSerializationFunction {
            NormalizedNode apply(DeSerializer deserializer, NormalizedNodeMessages.Node node);
        }
    }




}
