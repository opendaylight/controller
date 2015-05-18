/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.node.utils.serialization;

import com.google.common.base.Preconditions;
import org.opendaylight.yangtools.yang.data.api.schema.AnyXmlNode;
import org.opendaylight.yangtools.yang.data.api.schema.AugmentationNode;
import org.opendaylight.yangtools.yang.data.api.schema.ChoiceNode;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.LeafNode;
import org.opendaylight.yangtools.yang.data.api.schema.LeafSetEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.LeafSetNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.OrderedLeafSetNode;
import org.opendaylight.yangtools.yang.data.api.schema.OrderedMapNode;
import org.opendaylight.yangtools.yang.data.api.schema.UnkeyedListEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.UnkeyedListNode;

public enum NormalizedNodeType {
    CONTAINER_NODE_TYPE,
    LEAF_NODE_TYPE,
    MAP_NODE_TYPE,
    MAP_ENTRY_NODE_TYPE,
    AUGMENTATION_NODE_TYPE,
    LEAF_SET_NODE_TYPE,
    LEAF_SET_ENTRY_NODE_TYPE,
    CHOICE_NODE_TYPE,
    ORDERED_LEAF_SET_NODE_TYPE,
    ORDERED_MAP_NODE_TYPE,
    UNKEYED_LIST_NODE_TYPE,
    UNKEYED_LIST_ENTRY_NODE_TYPE,
    ANY_XML_NODE_TYPE;

    public static NormalizedNodeType getSerializableNodeType(NormalizedNode<?, ?> node){
        Preconditions.checkNotNull(node, "node should not be null");

        if(node instanceof LeafNode){
            return LEAF_NODE_TYPE;
        } else if(node instanceof LeafSetEntryNode){
            return LEAF_SET_ENTRY_NODE_TYPE;
        } else if(node instanceof MapEntryNode){
            return MAP_ENTRY_NODE_TYPE;
        } else if(node instanceof ContainerNode){
            return CONTAINER_NODE_TYPE;
        } else if(node instanceof AugmentationNode){
            return AUGMENTATION_NODE_TYPE;
        } else if(node instanceof ChoiceNode){
            return CHOICE_NODE_TYPE;
        } else if(node instanceof OrderedLeafSetNode){
            return ORDERED_LEAF_SET_NODE_TYPE;
        } else if(node instanceof OrderedMapNode){
            return ORDERED_MAP_NODE_TYPE;
        } else if(node instanceof MapNode){
            return MAP_NODE_TYPE;
        } else if(node instanceof LeafSetNode){
            return LEAF_SET_NODE_TYPE;
        } else if(node instanceof UnkeyedListNode){
            return UNKEYED_LIST_NODE_TYPE;
        } else if(node instanceof UnkeyedListEntryNode){
            return UNKEYED_LIST_ENTRY_NODE_TYPE;
        } else if(node instanceof AnyXmlNode){
            return ANY_XML_NODE_TYPE;
        }

        throw new IllegalArgumentException("Node type unknown : " + node.getClass().getSimpleName());
    }

}
