/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.restconf.impl;

import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.AugmentationIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.AnyXmlNode;
import org.opendaylight.yangtools.yang.data.api.schema.AugmentationNode;
import org.opendaylight.yangtools.yang.data.api.schema.ChoiceNode;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.LeafNode;
import org.opendaylight.yangtools.yang.data.api.schema.LeafSetNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;
import org.opendaylight.yangtools.yang.data.api.schema.MixinNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.OrderedLeafSetNode;
import org.opendaylight.yangtools.yang.data.api.schema.OrderedMapNode;
import org.opendaylight.yangtools.yang.data.api.schema.UnkeyedListEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.UnkeyedListNode;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.CollectionNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.DataContainerNodeAttrBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.DataContainerNodeBuilder;

class NormalizedDataPrunner {

    public DataContainerChild<?, ?> pruneDataAtDepth(final DataContainerChild<?, ?> node, final Integer depth) {
        if (depth == null) {
            return node;
        }

        if (node instanceof LeafNode || node instanceof LeafSetNode || node instanceof AnyXmlNode
                || node instanceof OrderedLeafSetNode) {
            return node;
        } else if (node instanceof MixinNode) {
            return processMixinNode(node, depth);
        } else if (node instanceof DataContainerNode) {
            return processContainerNode(node, depth);
        }
        throw new IllegalStateException("Unexpected Mixin node occured why pruning data to requested depth");
    }

    private DataContainerChild<?, ?> processMixinNode(final NormalizedNode<?, ?> node, final Integer depth) {
        if (node instanceof AugmentationNode) {
            return processAugmentationNode(node, depth);
        } else if (node instanceof ChoiceNode) {
            return processChoiceNode(node, depth);
        } else if (node instanceof OrderedMapNode) {
            return processOrderedMapNode(node, depth);
        } else if (node instanceof MapNode) {
            return processMapNode(node, depth);
        } else if (node instanceof UnkeyedListNode) {
            return processUnkeyedListNode(node, depth);
        }
        throw new IllegalStateException("Unexpected Mixin node occured why pruning data to requested depth");
    }

    private DataContainerChild<?, ?> processContainerNode(final NormalizedNode<?, ?> node, final Integer depth) {
        final ContainerNode containerNode = (ContainerNode) node;
        DataContainerNodeAttrBuilder<NodeIdentifier, ContainerNode> newContainerBuilder = Builders.containerBuilder()
                .withNodeIdentifier(containerNode.getIdentifier());
        if (depth > 1) {
            processDataContainerChild((DataContainerNode<?>) node, depth, newContainerBuilder);
        }
        return newContainerBuilder.build();
    }

    private DataContainerChild<?, ?> processChoiceNode(final NormalizedNode<?, ?> node, final Integer depth) {
        final ChoiceNode choiceNode = (ChoiceNode) node;
        DataContainerNodeBuilder<NodeIdentifier, ChoiceNode> newChoiceBuilder = Builders.choiceBuilder()
                .withNodeIdentifier(choiceNode.getIdentifier());

        processDataContainerChild((DataContainerNode<?>) node, depth, newChoiceBuilder);

        return newChoiceBuilder.build();
    }

    private DataContainerChild<?, ?> processAugmentationNode(final NormalizedNode<?, ?> node, final Integer depth) {
        final AugmentationNode augmentationNode = (AugmentationNode) node;
        DataContainerNodeBuilder<AugmentationIdentifier, ? extends DataContainerChild<?, ?>> newAugmentationBuilder = Builders
                .augmentationBuilder().withNodeIdentifier(augmentationNode.getIdentifier());

        processDataContainerChild((DataContainerNode<?>) node, depth, newAugmentationBuilder);

        return newAugmentationBuilder.build();
    }

    private void processDataContainerChild(
            final DataContainerNode<?> node,
            final Integer depth,
            final DataContainerNodeBuilder<? extends YangInstanceIdentifier.PathArgument, ? extends DataContainerNode<?>> newBuilder) {

        for (DataContainerChild<? extends PathArgument, ?> nodeValue : node.getValue()) {
            newBuilder.withChild(pruneDataAtDepth(nodeValue, depth - 1));
        }

    }

    private DataContainerChild<?, ?> processUnkeyedListNode(final NormalizedNode<?, ?> node, final Integer depth) {
        CollectionNodeBuilder<UnkeyedListEntryNode, UnkeyedListNode> newUnkeyedListBuilder = Builders
                .unkeyedListBuilder();
        if (depth > 1) {
            for (UnkeyedListEntryNode oldUnkeyedListEntry : ((UnkeyedListNode) node).getValue()) {
                DataContainerNodeAttrBuilder<NodeIdentifier, UnkeyedListEntryNode> newUnkeyedListEntry = Builders
                        .unkeyedListEntryBuilder().withNodeIdentifier(oldUnkeyedListEntry.getIdentifier());
                for (DataContainerChild<? extends PathArgument, ?> oldUnkeyedListEntryValue : oldUnkeyedListEntry
                        .getValue()) {
                    newUnkeyedListEntry.withChild(pruneDataAtDepth(oldUnkeyedListEntryValue, depth - 1));
                }
                newUnkeyedListBuilder.addChild(newUnkeyedListEntry.build());
            }
        }
        return newUnkeyedListBuilder.build();
    }

    private DataContainerChild<?, ?> processOrderedMapNode(final NormalizedNode<?, ?> node, final Integer depth) {
        CollectionNodeBuilder<MapEntryNode, OrderedMapNode> newOrderedMapNodeBuilder = Builders.orderedMapBuilder();
        processMapEntries(node, depth, newOrderedMapNodeBuilder);
        return newOrderedMapNodeBuilder.build();
    }

    private DataContainerChild<?, ?> processMapNode(final NormalizedNode<?, ?> node, final Integer depth) {
        CollectionNodeBuilder<MapEntryNode, MapNode> newMapNodeBuilder = Builders.mapBuilder();
        processMapEntries(node, depth, newMapNodeBuilder);
        return newMapNodeBuilder.build();
    }

    private void processMapEntries(final NormalizedNode<?, ?> node, final Integer depth,
            CollectionNodeBuilder<MapEntryNode, ? extends MapNode> newOrderedMapNodeBuilder) {
        if (depth > 1) {
            for (MapEntryNode oldMapEntryNode : ((MapNode) node).getValue()) {
                DataContainerNodeAttrBuilder<NodeIdentifierWithPredicates, MapEntryNode> newMapEntryNodeBuilder = Builders
                        .mapEntryBuilder().withNodeIdentifier(oldMapEntryNode.getIdentifier());
                for (DataContainerChild<? extends PathArgument, ?> mapEntryNodeValue : oldMapEntryNode.getValue()) {
                    newMapEntryNodeBuilder.withChild(pruneDataAtDepth(mapEntryNodeValue, depth - 1));
                }
                newOrderedMapNodeBuilder.withChild(newMapEntryNodeBuilder.build());
            }
        }
    }


}
