/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.dom.broker.util.operations;

import java.util.List;
import java.util.Map;

import org.opendaylight.yangtools.yang.data.api.Node;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;

/**
 * Strategy interface for data modification.
 *
 * @param <T> SchemaNode type
 * @param <W> NodeWrapper type (reflects the type of nodes to be handled e.g. leaf-list data, cotnainer data e.g.)
 *
 */
public interface Modification<T extends DataSchemaNode, W extends Modification.NodeWrapper> {

    W modify(T schemaNode, W actual, W modification, OperationStack operations) throws DataModificationException;

    /**
     * Simple wrapper for different node types
     */
    static interface NodeWrapper {

        boolean isEmpty();

        List<Node<?>> getNodes();

    }

    /**
     * Wrapper for leaf, container nodes
     */
    static interface SingleNodeWrapper extends NodeWrapper {
        Node<?> getSingleNode();
    }

    /**
     * Wrapper for list nodes
     */
    static interface ListNodeWrapper extends NodeWrapper {

        boolean contains(ListNodeKey key);

        boolean contains(Node<?> key) throws DataModificationException.MissingElementException;

        Node<?> getSingleNode(ListNodeKey key);

        Node<?> getSingleNode(Node<?> key) throws DataModificationException.MissingElementException;

        Map<ListNodeKey, Node<?>> getMappedNodes();
    }

    /**
     * Wrapper for leaf list nodes
     */
    static interface LeafListNodeWrapper extends NodeWrapper {

        boolean contains(Node<?> node);

    }

    /**
     * Simple wrapper for list nodes key. List node key might be a collection of child nodes or the list node itself
     */
    static interface ListNodeKey {

    }
}
