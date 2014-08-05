/*
 *
 *  Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package org.opendaylight.controller.cluster.datastore.node;

import com.google.common.base.Preconditions;
import org.opendaylight.controller.cluster.datastore.util.InstanceIdentifierUtils;
import org.opendaylight.controller.protobuff.messages.common.NormalizedNodeMessages;
import org.opendaylight.controller.protobuff.messages.common.NormalizedNodeMessages.Node;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.AugmentationNode;
import org.opendaylight.yangtools.yang.data.api.schema.ChoiceNode;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.LeafNode;
import org.opendaylight.yangtools.yang.data.api.schema.LeafSetEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.LeafSetNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;
import org.opendaylight.yangtools.yang.data.api.schema.MixinNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNodeContainer;
import org.opendaylight.yangtools.yang.data.api.schema.UnkeyedListEntryNode;

import java.util.Map;

/**
 * NormalizedNodeToProtocolBufferNode walks the NormalizedNode tree converting it to the
 * NormalizedMessage.Node
 * <p/>
 * {@link org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode } is a tree like structure that provides a generic structure for a yang data
 * model
 */
public class NormalizedNodeToProtocolBufferNode {


    private final Node.Builder builderRoot;
    private NormalizedNodeMessages.Container container;

    public NormalizedNodeToProtocolBufferNode() {

        builderRoot = Node.newBuilder();
    }

    public void encode(String parentPath, NormalizedNode<?, ?> normalizedNode) {
        if (parentPath == null) {
            parentPath = "";
        }

        NormalizedNodeMessages.Container.Builder containerBuilder =
            NormalizedNodeMessages.Container.newBuilder();

        if (normalizedNode != null) {

            navigateNormalizedNode(0, parentPath, normalizedNode, builderRoot);
            // here we need to put back the Node Tree in Container

            container =
                containerBuilder.setParentPath(parentPath).setNormalizedNode(
                    builderRoot.build()).build();
        } else {
            //this can happen when an attempt was made to read from datastore and normalized node was null.
            container = containerBuilder.setParentPath(parentPath).build();

        }

    }


    private void navigateDataContainerNode(int level, final String parentPath,
        final DataContainerNode<?> dataContainerNode,
        Node.Builder builderParent) {

        String newParentPath =
            parentPath + "/" + dataContainerNode.getIdentifier().toString();
        String type = getDataContainerType(dataContainerNode).getSimpleName();
        builderParent.setPath(dataContainerNode.getIdentifier().toString())
            .setType(type);

        final Iterable<DataContainerChild<? extends YangInstanceIdentifier.PathArgument, ?>>
            value =
            dataContainerNode.getValue();
        for (NormalizedNode<?, ?> node : value) {
            Node.Builder builderChild = Node.newBuilder();
            if (node instanceof MixinNode
                && node instanceof NormalizedNodeContainer) {

                navigateNormalizedNodeContainerMixin(level, newParentPath,
                    (NormalizedNodeContainer<?, ?, ?>) node, builderChild);
            } else {
                navigateNormalizedNode(level, newParentPath, node,
                    builderChild);
            }
            builderParent.addChild(builderChild);
        }
    }

    private Class getDataContainerType(
        NormalizedNodeContainer<?, ?, ?> dataContainerNode) {
        if (dataContainerNode instanceof ChoiceNode) {
            return ChoiceNode.class;
        } else if (dataContainerNode instanceof AugmentationNode) {
            return AugmentationNode.class;
        } else if (dataContainerNode instanceof ContainerNode) {
            return ContainerNode.class;
        } else if (dataContainerNode instanceof MapEntryNode) {
            return MapEntryNode.class;
        } else if (dataContainerNode instanceof UnkeyedListEntryNode) {
            return UnkeyedListEntryNode.class;
        } else if (dataContainerNode instanceof MapNode) {
            return MapNode.class;
        } else if (dataContainerNode instanceof LeafSetNode) {
            return LeafSetNode.class;
        }
        throw new IllegalArgumentException(
            "could not find the data container node type "
                + dataContainerNode.toString()
        );
    }

    private void navigateNormalizedNodeContainerMixin(int level,
        final String parentPath,
        NormalizedNodeContainer<?, ?, ?> node, Node.Builder builderParent) {
        String newParentPath =
            parentPath + "/" + node.getIdentifier().toString();

        builderParent.setPath(node.getIdentifier().toString()).setType(
            this.getDataContainerType(node).getSimpleName());
        final Iterable<? extends NormalizedNode<?, ?>> value = node.getValue();
        for (NormalizedNode normalizedNode : value) {
            // child node builder
            Node.Builder builderChild = Node.newBuilder();
            if (normalizedNode instanceof MixinNode
                && normalizedNode instanceof NormalizedNodeContainer) {
                navigateNormalizedNodeContainerMixin(level + 1, newParentPath,
                    (NormalizedNodeContainer) normalizedNode, builderChild);
            } else {
                navigateNormalizedNode(level, newParentPath, normalizedNode,
                    builderChild);
            }
            builderParent.addChild(builderChild);

        }



    }


    private void navigateNormalizedNode(int level,
        String parentPath, NormalizedNode<?, ?> normalizedNode,
        Node.Builder builderParent) {

        if (normalizedNode instanceof DataContainerNode) {

            final DataContainerNode<?> dataContainerNode =
                (DataContainerNode) normalizedNode;

            navigateDataContainerNode(level + 1, parentPath, dataContainerNode,
                builderParent);
        } else if (normalizedNode instanceof MixinNode
            && normalizedNode instanceof NormalizedNodeContainer) {

            navigateNormalizedNodeContainerMixin(level, parentPath,
                (NormalizedNodeContainer<?, ?, ?>) normalizedNode,
                builderParent);
        } else {
            if (normalizedNode instanceof LeafNode) {
                buildLeafNode(parentPath, normalizedNode, builderParent);
            } else if (normalizedNode instanceof LeafSetEntryNode) {
                buildLeafSetEntryNode(parentPath, normalizedNode,
                    builderParent);
            }

        }

    }

    private void buildLeafSetEntryNode(String parentPath,
        NormalizedNode<?, ?> normalizedNode,
        Node.Builder builderParent) {
        String path =
            parentPath + "/" + normalizedNode.getIdentifier().toString();
        LeafSetEntryNode leafSetEntryNode = (LeafSetEntryNode) normalizedNode;
        Map<QName, String> attributes = leafSetEntryNode.getAttributes();
        if (!attributes.isEmpty()) {
            NormalizedNodeMessages.Attribute.Builder builder = null;
            for (Map.Entry<QName, String> attribute : attributes.entrySet()) {
                builder = NormalizedNodeMessages.Attribute.newBuilder();

                builder
                    .setName(attribute.getKey().toString())
                    .setValue(normalizedNode.getValue().toString());

                builderParent.addAttributes(builder.build());
            }
        }
        buildNodeValue(normalizedNode, builderParent);
    }

    private void buildLeafNode(String parentPath,
        NormalizedNode<?, ?> normalizedNode,
        Node.Builder builderParent) {
        Preconditions.checkNotNull(parentPath);
        Preconditions.checkNotNull(normalizedNode);
        String path =
            parentPath + "/" + normalizedNode.getIdentifier().toString();
        LeafNode leafNode = (LeafNode) normalizedNode;
        Map<QName, String> attributes = leafNode.getAttributes();
        if (!attributes.isEmpty()) {
            NormalizedNodeMessages.Attribute.Builder builder = null;
            for (Map.Entry<QName, String> attribute : attributes.entrySet()) {
                builder = NormalizedNodeMessages.Attribute.newBuilder();
                builder
                    .setName(attribute.getKey().toString())
                    .setValue(attribute.getValue().toString());

                builderParent.addAttributes(builder.build());
            }
        }

        Object value = normalizedNode.getValue();
        if (value == null) {
            builderParent
                .setPath(normalizedNode.getIdentifier().toString())
                .setType(LeafNode.class.getSimpleName())
                .setValueType(String.class.getSimpleName())
                .setValue("");
        } else {
            buildNodeValue(normalizedNode, builderParent);
        }
    }

    private void buildNodeValue(NormalizedNode<?, ?> normalizedNode,
        Node.Builder builderParent) {

        Object value = normalizedNode.getValue();

        builderParent
            .setPath(normalizedNode.getIdentifier().toString())
            .setType(LeafNode.class.getSimpleName())
            .setValueType((value.getClass().getSimpleName()))
            .setValue(value.toString());

        if(value.getClass().equals(YangInstanceIdentifier.class)){
            builderParent.setInstanceIdentifierValue(
                InstanceIdentifierUtils
                    .toSerializable((YangInstanceIdentifier) value));
        }
    }

    public NormalizedNodeMessages.Container getContainer() {
        return container;
    }
}
