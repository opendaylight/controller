/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.yang.data.api;

import org.opendaylight.controller.yang.common.QName;

/**
 * For the use cases of changing the state data, the node modifications are
 * modeled as part of additional metadata in data tree. The modification types
 * are based on Netconf edit-config RPCs. In order to modify the configuration
 * or state data tree the user must create a tree representing the modification
 * of the data and apply that modification to the target tree.
 * 
 * 
 * @see <a href="http://tools.ietf.org/html/rfc6241">Network Configuration
 *      Protocol (NETCONF)</a>
 * 
 */
public interface CompositeNodeModification extends CompositeNode {

    ModifyAction getModificationAction();

    // Container Modification

    /**
     * The data identified by the node containing this modification is merged
     * with the data at the corresponding level in the data tree
     * 
     * @param node
     *            in data tree
     */
    void mergeChild(CompositeNode node);

    /**
     * The data identified by the node containing this modification replaces any
     * related data in the target data tree If no such data exists in the target
     * data tree, the child node is created.
     * 
     * @param node
     *            composite node
     */
    void replaceChild(CompositeNode node);

    /**
     * Adds new composite node into data tree
     * 
     * @param node
     *            composite node
     */
    void addChild(CompositeNode node);

    /**
     * The data identified by the node containing this modification is deleted
     * from the target data tree if and only if the data currently exists in the
     * target data tree. If the data does not exist, an error is returned with
     * an error value of "data-missing".
     * 
     * @param node
     */
    void deleteChild(CompositeNode node);

    /**
     * The data identified by the node containing this attribute is deleted from
     * the target data tree if the data currently exists in the target data
     * tree. If the data does not exist, the modification is silently ignored.
     * 
     * @param node
     *            composite node
     */
    void removeChild(CompositeNode node);

    // Leaf Modifications

    /**
     * The data identified by the node containing this modification replaces any
     * related data in the target data tree If no such data exists in the target
     * data tree, it is created.
     * 
     * @param leaf
     */
    void replaceSimpleNode(SimpleNode<?> leaf);

    /**
     * The data identified by the node containing this modification is deleted
     * from the target data tree if and only if the data currently exists in the
     * target data tree. If the data does not exist, an error is returned with
     * an error value of "data-missing".
     * 
     * @param leaf
     */
    void deleteSimpleNode(SimpleNode<?> leaf);

    /**
     * The data identified by the node containing this attribute is deleted from
     * the target data tree if the data currently exists in the target data
     * tree. If the data does not exist, the modification is silently ignored.
     * 
     * @param leaf
     */
    void removeSimpleNode(SimpleNode<?> leaf);

    void removeLeaf(SimpleNode<?> leaf);

    void removeLeaf(QName leaf);
}
