/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.dom.broker.util.operations;

import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.Node;
import org.opendaylight.yangtools.yang.data.api.SimpleNode;

import java.util.List;

public class DataModificationException extends Exception {

    private final QName node;

    public DataModificationException(String message, QName node) {
        super(message);
        this.node = node;
    }

    public QName getNodeQName() {
        return node;
    }

    public static final class DataMissingException extends DataModificationException {

        public DataMissingException(QName nodeType) {
            super(String.format("Data missing for node: %s", nodeType), nodeType);
        }

        public DataMissingException(QName nodeType, Node<?> modificationNode) {
            super(String.format("Data missing for node: %s, %s", nodeType, modificationNode), nodeType);
        }

        static void check(QName nodeQName, Modification.NodeWrapper actualNodes) throws DataMissingException {
            if (actualNodes.isEmpty()) {
                throw new DataMissingException(nodeQName);
            }
        }

        static void check(QName nodeQName, Modification.NodeWrapper actualNodes, Node<?> modificationNode)
                throws DataMissingException {
            if (actualNodes.contains(modificationNode) == false) {
                throw new DataMissingException(nodeQName, modificationNode);
            }

        }
    }

    public static final class DataExistsException extends DataModificationException {

        public DataExistsException(QName nodeType, Modification.NodeWrapper actualNodes) {
            super(String.format("Data already exists for node: %s, current value: %s", nodeType, actualNodes.getNodes()), nodeType);
        }

        static void check(QName nodeQName, Modification.NodeWrapper actualNodes) throws DataExistsException {
            if(actualNodes.isEmpty() == false) {
                throw new DataExistsException(nodeQName, actualNodes);
            }
        }

        static void check(QName nodeQName, Modification.NodeWrapper actualNodes, Node<?> modificationNode) throws DataExistsException {
            if(actualNodes.contains(modificationNode)) {
                throw new DataExistsException(nodeQName, actualNodes);
            }
        }
    }

}
