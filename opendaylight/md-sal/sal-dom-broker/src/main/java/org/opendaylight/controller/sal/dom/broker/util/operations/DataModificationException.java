/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.dom.broker.util.operations;

import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.Node;
import org.opendaylight.yangtools.yang.data.api.SimpleNode;
import org.opendaylight.yangtools.yang.model.api.DataNodeContainer;

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

        static void check(QName nodeQName, Modification.LeafListNodeWrapper actualNodes, Node<?> modificationNode)
                throws DataMissingException {
            if (actualNodes.contains(modificationNode) == false) {
                throw new DataMissingException(nodeQName, modificationNode);
            }
        }

        static void check(QName nodeQName, Modification.ListNodeWrapper actualNodes, Node<?> modificationNode)
                throws DataModificationException {
            if (actualNodes.contains(modificationNode) == false) {
                throw new DataMissingException(nodeQName, modificationNode);
            }
        }
    }

    public static final class DataExistsException extends DataModificationException {

        public DataExistsException(QName nodeType, Modification.NodeWrapper actualNodes) {
            super(String
                    .format("Data already exists for node: %s, current value: %s", nodeType, actualNodes.getNodes()),
                    nodeType);
        }

        static void check(QName nodeQName, Modification.NodeWrapper actualNodes) throws DataExistsException {
            if (actualNodes.isEmpty() == false) {
                throw new DataExistsException(nodeQName, actualNodes);
            }
        }

        static void check(QName nodeQName, Modification.LeafListNodeWrapper actualNodes, Node<?> modificationNode)
                throws DataExistsException {
            if (actualNodes.contains(modificationNode)) {
                throw new DataExistsException(nodeQName, actualNodes);
            }
        }

        public static void check(QName qName, Modification.ListNodeWrapper actualNodes, Node<?> listModification)
                throws DataModificationException {
            if (actualNodes.contains(listModification)) {
                throw new DataExistsException(qName, actualNodes);
            }
        }
    }

    public static final class MissingElementException extends DataModificationException {

        public MissingElementException(QName listQName, QName keyQName, Node<?> listNode) {
            super(String.format("Missing key value %s for %s, node %s", keyQName, listQName, listNode), keyQName);
        }

        public static void check(List<SimpleNode<?>> keyLeaves, QName listQName, QName keyQName, Node<?> listNode)
                throws MissingElementException {
            if (keyLeaves == null || keyLeaves.isEmpty())
                throw new MissingElementException(listQName, keyQName, listNode);
        }
    }

    public static final class IllegalChoiceValuesException extends DataModificationException {

        public IllegalChoiceValuesException(String message, QName node) {
            super(message, node);
        }

        public static void throwMultipleCasesReferenced(QName choiceQName, CompositeNode modification,
                QName case1QName, QName case2QName) throws IllegalChoiceValuesException {
            throw new IllegalChoiceValuesException(String.format(
                    "Child nodes from multiple cases present in modification: %s, choice: %s, case1: %s, case2: %s",
                    modification, choiceQName, case1QName, case2QName), choiceQName);
        }

        public static void throwDuplicateChild(QName choiceQName, QName caseQName, QName nodeQName) throws IllegalChoiceValuesException {
            throw new IllegalChoiceValuesException(String.format(
                    "Duplicate child node detected, choice: %s, case1: %s, child node: %s",
                    choiceQName, caseQName, nodeQName), choiceQName);
        }
    }

    public static final class UnknownNodeException extends DataModificationException {

        public UnknownNodeException(String message, QName node) {
            super(message, node);
        }

        public static void throwUnknownNode(QName unknownChildNode, DataNodeContainer schema) throws UnknownNodeException {
            throw new UnknownNodeException(String.format(
                    "Unknown child node: %s under %s detected in actual or modification data",
                    unknownChildNode, schema), unknownChildNode);
        }
    }
}
