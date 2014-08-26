/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.node.utils.serialization;

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

public class NormalizedNodeType {
    public static Integer CONTAINER_NODE_TYPE = new Integer(0);
    public static Integer LEAF_NODE_TYPE = new Integer(1);
    public static Integer MAP_NODE_TYPE = new Integer(2);
    public static Integer MAP_ENTRY_NODE_TYPE = new Integer(3);
    public static Integer AUGMENTATION_NODE_TYPE = new Integer(4);
    public static Integer LEAF_SET_NODE_TYPE = new Integer(5);
    public static Integer LEAF_SET_ENTRY_NODE_TYPE = new Integer(6);
    public static Integer CHOICE_NODE_TYPE = new Integer(7);
    public static Integer ORDERED_LEAF_SET_NODE_TYPE = new Integer(8);
    public static Integer ORDERED_MAP_NODE_TYPE = new Integer(9);
    public static Integer UNKEYED_LIST_NODE_TYPE = new Integer(10);
    public static Integer UNKEYED_LIST_ENTRY_NODE_TYPE = new Integer(11);
    public static Integer ANY_XML_NODE_TYPE = new Integer(12);

    public static Integer getSerializableNodeType(NormalizedNode node){
        if(node instanceof ContainerNode){
            return CONTAINER_NODE_TYPE;
        } else if(node instanceof LeafNode){
            return LEAF_NODE_TYPE;
        } else if(node instanceof MapNode){
            return MAP_NODE_TYPE;
        } else if(node instanceof MapEntryNode){
            return MAP_ENTRY_NODE_TYPE;
        } else if(node instanceof AugmentationNode){
            return AUGMENTATION_NODE_TYPE;
        } else if(node instanceof LeafSetNode){
            return LEAF_SET_NODE_TYPE;
        } else if(node instanceof LeafSetEntryNode){
            return LEAF_SET_ENTRY_NODE_TYPE;
        } else if(node instanceof ChoiceNode){
            return CHOICE_NODE_TYPE;
        } else if(node instanceof OrderedLeafSetNode){
            return ORDERED_LEAF_SET_NODE_TYPE;
        } else if(node instanceof OrderedMapNode){
            return ORDERED_MAP_NODE_TYPE;
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
