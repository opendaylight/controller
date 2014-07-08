/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.store.impl;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.two.level.list.TopLevelList;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

public class WildcardedScopeOneTest extends DefaultDataChangeListenerTestSuite {

    private static final InstanceIdentifier TOP_LEVEL_LIST_ALL = TOP_LEVEL.node(TopLevelList.QNAME).node(
            TopLevelList.QNAME);

    @Override
    protected void customizeTask(final DatastoreTestTask task) {
        task.changeListener(TOP_LEVEL_LIST_ALL, DataChangeScope.ONE);
    }

    @Override
    public void putTopLevelOneNested(final DatastoreTestTask task) throws InterruptedException, ExecutionException {

        AsyncDataChangeEvent<InstanceIdentifier, NormalizedNode<?, ?>> change = task.getChangeEvent().get();

        assertNotNull(change);

        assertNotContains(change.getCreatedData(), TOP_LEVEL);
        assertContains(change.getCreatedData(), path(FOO), path(FOO, BAR));

        assertEmpty(change.getUpdatedData());
        assertEmpty(change.getRemovedPaths());

    }

    @Override
    public void replaceTopLevelNestedChanged(final DatastoreTestTask task) throws InterruptedException,
            ExecutionException {

        AsyncDataChangeEvent<InstanceIdentifier, NormalizedNode<?, ?>> change = task.getChangeEvent().get();
        assertNotNull(change);

        assertContains(change.getCreatedData(), path(FOO, BAZ));
        assertContains(change.getUpdatedData(), path(FOO));
        assertNotContains(change.getUpdatedData(), TOP_LEVEL);
        assertContains(change.getRemovedPaths(), path(FOO, BAR));

    }

    @Override
    protected void putTopLevelWithTwoNested(final DatastoreTestTask task) throws InterruptedException,
            ExecutionException {

        AsyncDataChangeEvent<InstanceIdentifier, NormalizedNode<?, ?>> change = task.getChangeEvent().get();
        assertNotNull(change);
        assertFalse(change.getCreatedData().isEmpty());

        assertContains(change.getCreatedData(), path(FOO), path(FOO, BAR), path(FOO, BAZ));
        assertNotContains(change.getCreatedData(), TOP_LEVEL);
        assertEmpty(change.getUpdatedData());
        assertEmpty(change.getRemovedPaths());

    }

    @Override
    protected void twoNestedExistsOneIsDeleted(final DatastoreTestTask task) throws InterruptedException,
            ExecutionException {

        Future<?> future = task.getChangeEvent();
        /*
         * One listener should be notified only and only if actual node changed its state,
         * since deletion of nested child (in this case /nested-list/nested-list[foo],
         * did not result in change of node we are listening
         * for, we should not be getting data change event
         * and this means settable future containing receivedDataChangeEvent is not done.
         *
         */
        assertFalse(future.isDone());
    }

    @Override
    public void nestedListExistsRootDeleted(final DatastoreTestTask task) throws InterruptedException,
            ExecutionException {

        AsyncDataChangeEvent<InstanceIdentifier, NormalizedNode<?, ?>> change = task.getChangeEvent().get();

        assertEmpty(change.getCreatedData());
        assertEmpty(change.getUpdatedData());

        assertNotContains(change.getUpdatedData(), TOP_LEVEL);
        assertContains(change.getRemovedPaths(), path(FOO),path(FOO, BAZ),path(FOO,BAR));
    }

    @Override
    protected void existingOneNestedWriteAdditionalNested(final DatastoreTestTask task) {
        Future<?> future = task.getChangeEvent();
        /*
         * One listener should be notified only and only if actual node changed its state,
         * since deletion of nested child (in this case /nested-list/nested-list[foo],
         * did not result in change of node we are listening
         * for, we should not be getting data change event
         * and this means settable future containing receivedDataChangeEvent is not done.
         *
         */
        assertFalse(future.isDone());
    }

    @Override
    protected void existingTopWriteTwoNested(final DatastoreTestTask task) throws InterruptedException, ExecutionException {
        Future<?> future = task.getChangeEvent();
        /*
         * One listener should be notified only and only if actual node changed its state,
         * since deletion of nested child (in this case /nested-list/nested-list[foo],
         * did not result in change of node we are listening
         * for, we should not be getting data change event
         * and this means settable future containing receivedDataChangeEvent is not done.
         *
         */
        assertFalse(future.isDone());
    }

    @Override
    protected void existingTopWriteSibling(final DatastoreTestTask task) throws InterruptedException, ExecutionException {
        AsyncDataChangeEvent<InstanceIdentifier, NormalizedNode<?, ?>> change = task.getChangeEvent().get();

        assertContains(change.getCreatedData(), path(FOO_SIBLING));
        assertNotContains(change.getUpdatedData(),path(FOO), TOP_LEVEL);
        assertEmpty(change.getRemovedPaths());
    }
}
