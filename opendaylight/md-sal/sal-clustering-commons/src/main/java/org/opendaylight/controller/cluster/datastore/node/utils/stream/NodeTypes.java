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

final class NodeTypes {
    public static final byte LEAF_NODE = 1;
    public static final byte LEAF_SET = 2;
    public static final byte LEAF_SET_ENTRY_NODE = 3;
    public static final byte CONTAINER_NODE = 4;
    public static final byte UNKEYED_LIST = 5;
    public static final byte UNKEYED_LIST_ITEM = 6;
    public static final byte MAP_NODE = 7;
    public static final byte MAP_ENTRY_NODE = 8;
    public static final byte ORDERED_MAP_NODE = 9;
    public static final byte CHOICE_NODE = 10;
    public static final byte AUGMENTATION_NODE = 11;
    public static final byte ANY_XML_NODE = 12;
    public static final byte END_NODE = 13;

    private NodeTypes() {
        throw new UnsupportedOperationException("utility class");
    }
}
