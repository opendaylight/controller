/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.dom.broker.util.operations;

import com.google.common.base.Optional;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.ModifyAction;
import org.opendaylight.yangtools.yang.model.api.ContainerSchemaNode;

/**
 * Edit config operations.
 */
public final class DataOperations {

    public static Optional<CompositeNode> modify(ContainerSchemaNode schema, CompositeNode stored,
            CompositeNode modified) throws DataModificationException {
        return modify(schema, stored, modified, ModifyAction.MERGE);
    }

    public static Optional<CompositeNode> modify(ContainerSchemaNode schema, CompositeNode stored,
            CompositeNode modified, ModifyAction defaultOperation) throws DataModificationException {

        OperationStack operations = new OperationStack.OperationStackImpl(defaultOperation);

        Modification.SingleNodeWrapper returned = new ContainerNodeModification().modify(schema,
                NodeWrappers.wrapNode(stored), NodeWrappers.wrapNode(modified), operations);

        return returned.isEmpty() ? Optional.<CompositeNode> absent() : Optional.of((CompositeNode) returned
                .getSingleNode());
    }
}
