/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.dom.broker.util.operations;

import org.opendaylight.yangtools.yang.model.api.LeafSchemaNode;

public class LeafNodeModification implements Modification<LeafSchemaNode, Modification.SingleNodeWrapper> {

    @Override
    public SingleNodeWrapper modify(LeafSchemaNode schemaNode, SingleNodeWrapper actualNode,
                                    SingleNodeWrapper modificationNode, OperationStack operationStack) throws DataModificationException {

        if (modificationNode.isEmpty() == false) {
            operationStack.enteringNode(modificationNode.getSingleNode());
        }

        SingleNodeWrapper result = null;

        switch (operationStack.getCurrentOperation()) {
            case MERGE: {
                result = modificationNode.isEmpty() ? actualNode : modificationNode;
                break;
            }
            case CREATE: {
                DataModificationException.DataExistsException.check(schemaNode.getQName(), actualNode);
            }
            case REPLACE: {
                result = modificationNode;
                break;
            }
            case DELETE: {
                DataModificationException.DataMissingException.check(schemaNode.getQName(), actualNode);
            }
            case REMOVE: {
                result = NodeWrappers.emptySingleNodeWrapper();
                break;
            }
            case NONE:
                result = actualNode;
                break;
        }

        if (modificationNode.isEmpty() == false) {
            operationStack.exitingNode(modificationNode.getSingleNode());
        }

        return result;
    }
}
