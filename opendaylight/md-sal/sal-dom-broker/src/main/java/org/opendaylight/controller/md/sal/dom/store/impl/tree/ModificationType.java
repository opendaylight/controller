/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.store.impl.tree;

public enum ModificationType {

    /**
     *
     * Node is unmodified
     *
     *
     */
    UNMODIFIED,
    /**
     *
     * Child of tree node was modified
     *
     */
    SUBTREE_MODIFIED,
    /**
     * Tree node was replaced with new value / subtree
     *
     */
    WRITE,
    /**
     *
     * Tree node is to be deleted.
     *
     */
    DELETE
}
