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

public enum NodeTypes {
    LEAF_NODE, LEAF_SET, LEAF_SET_ENTRY_NODE, CONTAINER_NODE,
    UNKEYED_LIST, UNKEYED_LIST_ITEM, MAP_NODE, MAP_ENTRY_NODE, ORDERED_MAP_NODE, CHOICE_NODE,
    AUGMENTATION_NODE, ANY_XML_NODE, END_NODE
}
