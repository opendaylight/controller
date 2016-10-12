/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.node.utils.serialization;

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

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import java.util.EnumMap;
import java.util.Map;
import javax.xml.transform.dom.DOMSource;
import org.opendaylight.controller.cluster.datastore.util.InstanceIdentifierUtils;
import org.opendaylight.controller.protobuff.messages.common.NormalizedNodeMessages;
import org.opendaylight.controller.protobuff.messages.common.NormalizedNodeMessages.Node.Builder;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.AugmentationIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeWithValue;
import org.opendaylight.yangtools.yang.data.api.schema.AnyXmlNode;
import org.opendaylight.yangtools.yang.data.api.schema.AugmentationNode;
import org.opendaylight.yangtools.yang.data.api.schema.ChoiceNode;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.api.schema.LeafNode;
import org.opendaylight.yangtools.yang.data.api.schema.LeafSetEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.UnkeyedListEntryNode;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.CollectionNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.DataContainerNodeAttrBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.DataContainerNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.ListNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.NormalizedNodeAttrBuilder;

/**
 * NormalizedNodeSerializer can be used to convert a Normalized node to and and
 * from a protocol buffer message.
 *
 *
 */
public class NormalizedNodeSerializer {

    /**
     * Serialize a NormalizedNode into a protocol buffer message
     * <p/>
     * The significant things to be aware of the Serialization process are
     * <ul>
     *     <li>Repeated strings like namespaces, revisions and localNames are
     *     compressed to codes and stored in the top level of the normalized
     *     node protocol buffer message
     *     </li>
     *     <li>All value types are encoded for each leaf value. This is so that
     *     the deSerialization process does not need to use the schema context to
     *     figure out how to decode values
     *     </li>
     * </ul>
     * One question which may arise is why not use something like gzip to
     * compress the protocol buffer message instead of rolling our own
     * encoding scheme. This has to be explored further as it is a more
     * general solution.
     *
     * @param node the node
     * @return a NormalizedNodeMessages.Node
     */
    public static NormalizedNodeMessages.Node serialize(final NormalizedNode<?, ?> node) {
        Preconditions.checkNotNull(node, "node should not be null");
        return new Serializer(node).serialize();
    }

    public static Serializer newSerializer(final NormalizedNode<?, ?> node) {
        Preconditions.checkNotNull(node, "node should not be null");
        return new Serializer(node);
    }

    /**
     * DeSerialize a protocol buffer message back into a NormalizedNode.
     *
     * @param node the node
     * @return a NormalizedNode
     */
    public static NormalizedNode<?, ?> deSerialize(final NormalizedNodeMessages.Node node) {
        Preconditions.checkNotNull(node, "node should not be null");
        return new DeSerializer(null, node).deSerialize();
    }

    /**
     * DeSerialize a PathArgument which is in the protocol buffer format into
     * a yang PathArgument. The protocol buffer path argument is specially
     * encoded and can only be interpreted in the context of a top level
     * serialized NormalizedNode protocol buffer message. The reason for this
     * is that during the NormalizedNode serialization process certain repeated
     * strings are encoded into a "codes" list and the actual strings are
     * replaced by an integer which is an index into that list.
     */
    public static YangInstanceIdentifier.PathArgument deSerialize(final NormalizedNodeMessages.Node node,
            final NormalizedNodeMessages.PathArgument pathArgument) {
        Preconditions.checkNotNull(node, "node should not be null");
        Preconditions.checkNotNull(pathArgument, "pathArgument should not be null");
        return new DeSerializer(null, node).deSerialize(pathArgument);
    }

    public static DeSerializer newDeSerializer(final NormalizedNodeMessages.InstanceIdentifier path,
            final NormalizedNodeMessages.Node node) {
        Preconditions.checkNotNull(node, "node should not be null");
        return new DeSerializer(path, node);
    }

    public static class Serializer extends QNameSerializationContextImpl
                                   implements NormalizedNodeSerializationContext {

        private final NormalizedNode<?, ?> node;

        private NormalizedNodeMessages.InstanceIdentifier serializedPath;

        private Serializer(final NormalizedNode<?, ?> node) {
            this.node = node;
        }

        public NormalizedNodeMessages.InstanceIdentifier getSerializedPath() {
            return serializedPath;
        }

        public NormalizedNodeMessages.Node serialize() {
            return this.serialize(node).addAllCode(getCodes()).build();
        }

        public NormalizedNodeMessages.Node serialize(final YangInstanceIdentifier path) {
            Builder builder = serialize(node);
            serializedPath = InstanceIdentifierUtils.toSerializable(path, this);
            return builder.addAllCode(getCodes()).build();
        }

        private NormalizedNodeMessages.Node.Builder serialize(final NormalizedNode<?, ?> fromNode) {
            NormalizedNodeMessages.Node.Builder builder =
                NormalizedNodeMessages.Node.newBuilder();

            builder.setPathArgument(PathArgumentSerializer.serialize(this, fromNode.getIdentifier()));
            Integer nodeType = getSerializableNodeType(fromNode).ordinal();
            builder.setIntType(nodeType);
            Object value = fromNode.getValue();

            // We need to do a specific check of the type of the node here
            // because if we looked at the value type alone we will not be
            // able to distinguish if the value was supposed to be added
            // as a child or whether the value should be added as a value of the
            // node.
            // One use case where this check is necessary when you have a node
            // with a bits value. In that case the value of the node is a Set
            // which is also a Collection. Without the following check being
            // done first the code would flow into the Collection if condition
            // and the Set would be added as child nodes
            if (nodeType == NormalizedNodeType.LEAF_NODE_TYPE.ordinal()
                    || nodeType == NormalizedNodeType.LEAF_SET_ENTRY_NODE_TYPE.ordinal()) {

                ValueSerializer.serialize(builder, this, value);

            } else if (value instanceof Iterable) {
                Iterable<?> iterable = (Iterable<?>) value;

                for (Object o : iterable) {
                    if (o instanceof NormalizedNode) {
                        builder.addChild(serialize((NormalizedNode<?, ?>) o));
                    }
                }

            } else if (value instanceof NormalizedNode) {

                builder.addChild(serialize((NormalizedNode<?, ?>) value));

            } else {

                ValueSerializer.serialize(builder, this, value);
            }

            return builder;
        }
    }

    @SuppressWarnings("rawtypes")
    public static class DeSerializer extends QNameDeSerializationContextImpl
                                     implements NormalizedNodeDeSerializationContext {
        private static final Map<NormalizedNodeType, DeSerializationFunction> DESERIALIZATION_FUNCTIONS;

        static {
            final EnumMap<NormalizedNodeType, DeSerializationFunction> m = new EnumMap<>(NormalizedNodeType.class);

            m.put(CONTAINER_NODE_TYPE, (deSerializer, node) -> {
                DataContainerNodeAttrBuilder<NodeIdentifier, ContainerNode> builder = Builders.containerBuilder()
                        .withNodeIdentifier(deSerializer.toNodeIdentifier(node.getPathArgument()));

                return deSerializer.buildDataContainer(builder, node);
            });
            m.put(LEAF_NODE_TYPE, (deSerializer, node) -> {
                NormalizedNodeAttrBuilder<NodeIdentifier, Object, LeafNode<Object>> builder = Builders.leafBuilder()
                        .withNodeIdentifier(deSerializer.toNodeIdentifier(node.getPathArgument()));

                return deSerializer.buildNormalizedNode(builder, node);
            });
            m.put(MAP_NODE_TYPE, (deSerializer, node) -> deSerializer.buildCollectionNode(Builders.mapBuilder(), node));
            m.put(MAP_ENTRY_NODE_TYPE, (deSerializer, node) -> {
                DataContainerNodeAttrBuilder<NodeIdentifierWithPredicates, MapEntryNode> builder =
                        Builders.mapEntryBuilder().withNodeIdentifier(deSerializer.toNodeIdentifierWithPredicates(
                            node.getPathArgument()));

                return deSerializer.buildDataContainer(builder, node);
            });
            m.put(AUGMENTATION_NODE_TYPE, (deSerializer, node) -> {
                DataContainerNodeBuilder<AugmentationIdentifier, AugmentationNode> builder =
                        Builders.augmentationBuilder().withNodeIdentifier(
                            deSerializer.toAugmentationIdentifier(node.getPathArgument()));

                return deSerializer.buildDataContainer(builder, node);
            });
            m.put(LEAF_SET_NODE_TYPE, (deSerializer, node)
                -> deSerializer.buildListNode(Builders.leafSetBuilder(), node));
            m.put(LEAF_SET_ENTRY_NODE_TYPE, (deSerializer, node) -> {
                NormalizedNodeAttrBuilder<NodeWithValue, Object, LeafSetEntryNode<Object>> builder =
                        Builders.leafSetEntryBuilder().withNodeIdentifier(deSerializer.toNodeWithValue(
                            node.getPathArgument()));

                return deSerializer.buildNormalizedNode(builder, node);
            });
            m.put(CHOICE_NODE_TYPE, (deSerializer, node) -> {
                DataContainerNodeBuilder<NodeIdentifier, ChoiceNode> builder = Builders.choiceBuilder()
                        .withNodeIdentifier(deSerializer.toNodeIdentifier(node.getPathArgument()));

                return deSerializer.buildDataContainer(builder, node);
            });
            m.put(ORDERED_LEAF_SET_NODE_TYPE, (deSerializer, node)
                -> deSerializer.buildListNode(Builders.orderedLeafSetBuilder(), node));
            m.put(ORDERED_MAP_NODE_TYPE, (deSerializer, node)
                -> deSerializer.buildCollectionNode(Builders.orderedMapBuilder(), node));
            m.put(UNKEYED_LIST_NODE_TYPE, (deSerializer, node)
                -> deSerializer.buildCollectionNode(Builders.unkeyedListBuilder(), node));
            m.put(UNKEYED_LIST_ENTRY_NODE_TYPE, (deSerializer, node) -> {
                DataContainerNodeAttrBuilder<NodeIdentifier, UnkeyedListEntryNode> builder =
                        Builders.unkeyedListEntryBuilder().withNodeIdentifier(deSerializer.toNodeIdentifier(
                            node.getPathArgument()));

                return deSerializer.buildDataContainer(builder, node);
            });
            m.put(ANY_XML_NODE_TYPE, (deSerializer, node) -> {
                NormalizedNodeAttrBuilder<NodeIdentifier, DOMSource, AnyXmlNode> builder = Builders.anyXmlBuilder()
                        .withNodeIdentifier(deSerializer.toNodeIdentifier(node.getPathArgument()));

                return deSerializer.buildNormalizedNode(builder, node);
            });

            DESERIALIZATION_FUNCTIONS = Maps.immutableEnumMap(m);
        }

        private final NormalizedNodeMessages.Node node;
        private final NormalizedNodeMessages.InstanceIdentifier path;
        private YangInstanceIdentifier deserializedPath;

        public DeSerializer(final NormalizedNodeMessages.InstanceIdentifier path,
                final NormalizedNodeMessages.Node node) {
            super(node.getCodeList());
            this.path = path;
            this.node = node;
        }

        public YangInstanceIdentifier getDeserializedPath() {
            return deserializedPath;
        }

        public NormalizedNode<?, ?> deSerialize() {
            NormalizedNode<?, ?> deserializedNode = deSerialize(node);
            if (path != null) {
                deserializedPath = InstanceIdentifierUtils.fromSerializable(path, this);
            }

            return deserializedNode;
        }

        private NormalizedNode<?, ?> deSerialize(final NormalizedNodeMessages.Node fromNode) {
            Preconditions.checkNotNull(fromNode, "node should not be null");

            DeSerializationFunction deSerializationFunction = DESERIALIZATION_FUNCTIONS.get(
                    NormalizedNodeType.values()[fromNode.getIntType()]);

            return deSerializationFunction.apply(this, fromNode);
        }

        public YangInstanceIdentifier.PathArgument deSerialize(final NormalizedNodeMessages.PathArgument pathArgument) {
            return PathArgumentSerializer.deSerialize(this, pathArgument);
        }

        @SuppressWarnings("unchecked")
        private NormalizedNode<?, ?> buildCollectionNode(final CollectionNodeBuilder builder,
                final NormalizedNodeMessages.Node fromNode) {

            builder.withNodeIdentifier(toNodeIdentifier(fromNode.getPathArgument()));

            for (NormalizedNodeMessages.Node child : fromNode.getChildList()) {
                builder.withChild(deSerialize(child));
            }

            return builder.build();
        }


        @SuppressWarnings("unchecked")
        private NormalizedNode<?, ?> buildListNode(final ListNodeBuilder<Object, LeafSetEntryNode<Object>> builder,
                final NormalizedNodeMessages.Node fromNode) {
            builder.withNodeIdentifier(toNodeIdentifier(fromNode.getPathArgument()));

            for (NormalizedNodeMessages.Node child : fromNode.getChildList()) {
                builder.withChild((LeafSetEntryNode<Object>) deSerialize(child));
            }

            return builder.build();
        }

        private NormalizedNode<?, ?> buildDataContainer(final DataContainerNodeBuilder<?, ?> builder,
                final NormalizedNodeMessages.Node fromNode) {

            for (NormalizedNodeMessages.Node child : fromNode.getChildList()) {
                builder.withChild((DataContainerChild<?, ?>) deSerialize(child));
            }

            //TODO : Also handle attributes

            return builder.build();
        }

        @SuppressWarnings("unchecked")
        private NormalizedNode<?, ?> buildNormalizedNode(final NormalizedNodeAttrBuilder builder,
                final NormalizedNodeMessages.Node fromNode) {

            builder.withValue(ValueSerializer.deSerialize(this, fromNode));

            //TODO : Also handle attributes

            return builder.build();

        }

        private NodeIdentifierWithPredicates toNodeIdentifierWithPredicates(
                final NormalizedNodeMessages.PathArgument fromPath) {
            return (NodeIdentifierWithPredicates) PathArgumentSerializer.deSerialize(this, fromPath);
        }

        private AugmentationIdentifier toAugmentationIdentifier(final NormalizedNodeMessages.PathArgument fromPath) {
            return (AugmentationIdentifier) PathArgumentSerializer.deSerialize(this, fromPath);
        }

        @SuppressWarnings("unchecked")
        private <T> NodeWithValue<T> toNodeWithValue(final NormalizedNodeMessages.PathArgument fromPath) {
            return (NodeWithValue<T>) PathArgumentSerializer.deSerialize(this, fromPath);
        }

        private NodeIdentifier toNodeIdentifier(final NormalizedNodeMessages.PathArgument fromPath) {
            return (NodeIdentifier) PathArgumentSerializer.deSerialize(this, fromPath);
        }

        private interface DeSerializationFunction {
            NormalizedNode<?, ?> apply(DeSerializer deserializer, NormalizedNodeMessages.Node node);
        }
    }
}
