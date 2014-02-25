/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.dom.broker.util.operations;

import java.util.Deque;
import com.google.common.collect.Lists;
import org.opendaylight.yangtools.yang.data.api.NodeModification;
import com.google.common.base.Preconditions;
import org.opendaylight.yangtools.yang.data.api.ModifyAction;
import org.opendaylight.yangtools.yang.data.api.Node;

/**
 * Tracks netconf operations on nested nodes.
 */
public interface OperationStack {

    void enteringNode(Node<?> modificationNode);

    ModifyAction getCurrentOperation();

    void exitingNode(Node<?> modificationNode);

    class OperationStackImpl implements org.opendaylight.controller.sal.dom.broker.util.operations.OperationStack {

        private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(OperationStackImpl.class);

        final Deque<ModifyAction> operations = Lists.newLinkedList();

        public OperationStackImpl(ModifyAction operation) {
            operations.add(operation);
        }

        void enteringNode(NodeModification modificationNode) {
            ModifyAction operation = modificationNode.getModificationAction();
            if (operation == null) {
                return;
            }

            addOperation(operation);
        }

        @Override
        public void enteringNode(Node<?> modificationNode) {
            Preconditions.checkArgument(modificationNode instanceof NodeModification);
            enteringNode((NodeModification) modificationNode);
        }

        private void addOperation(ModifyAction operation) {
            // Add check for permitted operation
            operations.add(operation);
            logger.trace("Operation added {}, {}", operation, this);
        }

        @Override
        public ModifyAction getCurrentOperation() {
            return operations.getLast();
        }

        void exitingNode(NodeModification modificationNode) {
            ModifyAction operation = modificationNode.getModificationAction();
            if (operation == null) {
                return;
            }

            Preconditions.checkState(operations.size() > 1);
            Preconditions.checkState(operations.peekLast().equals(operation), "Operations mismatch %s, %s",
                    operations.peekLast(), operation);

            ModifyAction removed = operations.removeLast();
            logger.trace("Operation removed {}, {}", removed, this);

        }

        @Override
        public void exitingNode(Node<?> modificationNode) {
            Preconditions.checkArgument(modificationNode instanceof NodeModification);
            exitingNode((NodeModification) modificationNode);
        }

        @Override
        public String toString() {
            return operations.toString();
        }

    }

}
