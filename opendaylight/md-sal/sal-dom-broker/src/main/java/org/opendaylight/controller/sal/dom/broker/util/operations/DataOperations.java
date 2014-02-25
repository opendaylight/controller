/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.dom.broker.util.operations;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.ModifyAction;
import org.opendaylight.yangtools.yang.data.api.Node;
import org.opendaylight.yangtools.yang.model.api.ContainerSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ListSchemaNode;

/**
 * Edit config operations.
 */
public final class DataOperations {

    // TODO extract Data operations to standalone module

    public static Optional<CompositeNode> modify(ContainerSchemaNode schema, CompositeNode stored,
            CompositeNode modified) throws DataModificationException {
        return modify(schema, stored, modified, ModifyAction.MERGE);
    }

    // TODO implement validation when building CompositeNodes from DOM (XmlDocumentUtils)

    public static Optional<CompositeNode> modify(ListSchemaNode schema, CompositeNode stored,
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

    public static Optional<CompositeNode> modify(ListSchemaNode schema, CompositeNode stored,
            CompositeNode modified, ModifyAction defaultOperation) throws DataModificationException {

        OperationStack operations = new OperationStack.OperationStackImpl(defaultOperation);

        Modification.ListNodeWrapper returned = new ListNodeModification().modify(schema,
                NodeWrappers.wrapListNodes(schema, Lists.<Node<?>> newArrayList(stored)),
                NodeWrappers.wrapListNodes(schema, Lists.<Node<?>> newArrayList(modified)), operations);

        return returned.isEmpty() ? Optional.<CompositeNode> absent() : Optional.of((CompositeNode) returned
                .getSingleNode(modified));
    }
}
