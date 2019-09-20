/*
 * Copyright (c) 2014, 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.node.utils.stream;

/**
 * Stream constants identifying individual node types.
 */
final class LithiumNode {
    static final byte LEAF_NODE = 1;
    static final byte LEAF_SET = 2;
    static final byte LEAF_SET_ENTRY_NODE = 3;
    static final byte CONTAINER_NODE = 4;
    static final byte UNKEYED_LIST = 5;
    static final byte UNKEYED_LIST_ITEM = 6;
    static final byte MAP_NODE = 7;
    static final byte MAP_ENTRY_NODE = 8;
    static final byte ORDERED_MAP_NODE = 9;
    static final byte CHOICE_NODE = 10;
    static final byte AUGMENTATION_NODE = 11;
    static final byte ANY_XML_NODE = 12;
    static final byte END_NODE = 13;
    static final byte ORDERED_LEAF_SET = 14;
    static final byte YANG_MODELED_ANY_XML_NODE = 15;

    private LithiumNode() {
        throw new UnsupportedOperationException("utility class");
    }
}
