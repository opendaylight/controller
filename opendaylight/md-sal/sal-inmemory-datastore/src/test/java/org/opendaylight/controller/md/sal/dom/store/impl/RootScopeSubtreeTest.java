/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.store.impl;

import java.util.concurrent.ExecutionException;

import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

public class RootScopeSubtreeTest extends DefaultDataChangeListenerTestSuite {

    @Override
    protected void customizeTask(final DatastoreTestTask task) {
        task.changeListener(TOP_LEVEL, DataChangeScope.SUBTREE);
    }

    @Override
    public void putTopLevelOneNested(final DatastoreTestTask task) throws InterruptedException, ExecutionException {
        AsyncDataChangeEvent<YangInstanceIdentifier, NormalizedNode<?, ?>> change = task.getChangeEvent();

        assertContains(change.getCreatedData(), TOP_LEVEL, path(FOO), path(FOO, BAR));
        assertEmpty(change.getUpdatedData());
        assertEmpty(change.getRemovedPaths());
    }

    @Override
    public void replaceTopLevelNestedChanged(final DatastoreTestTask task) throws InterruptedException,
            ExecutionException {

        AsyncDataChangeEvent<YangInstanceIdentifier, NormalizedNode<?, ?>> change = task.getChangeEvent();

        assertContains(change.getCreatedData(), path(FOO, BAZ));
        assertContains(change.getUpdatedData(), TOP_LEVEL, path(FOO));
        assertContains(change.getRemovedPaths(), path(FOO, BAR));
    }

    @Override
    protected void putTopLevelWithTwoNested(final DatastoreTestTask task) throws InterruptedException,
            ExecutionException {

        AsyncDataChangeEvent<YangInstanceIdentifier, NormalizedNode<?, ?>> change = task.getChangeEvent();

        assertContains(change.getCreatedData(), TOP_LEVEL, path(FOO), path(FOO, BAR), path(FOO, BAZ));
        assertEmpty(change.getUpdatedData());
        assertEmpty(change.getRemovedPaths());
    }

    @Override
    protected void twoNestedExistsOneIsDeleted(final DatastoreTestTask task) throws InterruptedException,
            ExecutionException {

        AsyncDataChangeEvent<YangInstanceIdentifier, NormalizedNode<?, ?>> change = task.getChangeEvent();

        assertEmpty(change.getCreatedData());
        assertContains(change.getUpdatedData(), TOP_LEVEL, path(FOO));
        assertContains(change.getRemovedPaths(), path(FOO, BAZ));
    }

    @Override
    protected void nestedListExistsRootDeleted(final DatastoreTestTask task) throws InterruptedException,
            ExecutionException {

        AsyncDataChangeEvent<YangInstanceIdentifier, NormalizedNode<?, ?>> change = task.getChangeEvent();

        assertEmpty(change.getCreatedData());
        assertEmpty(change.getUpdatedData());
        assertContains(change.getRemovedPaths(), TOP_LEVEL, path(FOO), path(FOO, BAR), path(FOO, BAZ));
    }

    @Override
    protected void existingOneNestedWriteAdditionalNested(final DatastoreTestTask task) throws InterruptedException, ExecutionException {
        AsyncDataChangeEvent<YangInstanceIdentifier, NormalizedNode<?, ?>> change = task.getChangeEvent();

        assertContains(change.getCreatedData(), path(FOO,BAZ));
        assertNotContains(change.getCreatedData(), path(FOO,BAR));
        assertContains(change.getUpdatedData(), TOP_LEVEL, path(FOO));
        assertEmpty(change.getRemovedPaths());
    }

    @Override
    protected void existingTopWriteTwoNested(final DatastoreTestTask task) throws InterruptedException, ExecutionException {
        AsyncDataChangeEvent<YangInstanceIdentifier, NormalizedNode<?, ?>> change = task.getChangeEvent();

        assertContains(change.getCreatedData(), path(FOO,BAR),path(FOO,BAZ));
        assertContains(change.getUpdatedData(), TOP_LEVEL, path(FOO));
        assertNotContains(change.getUpdatedData(), path(FOO,BAR));
        assertEmpty(change.getRemovedPaths());
    }

    @Override
    protected void existingTopWriteSibling(final DatastoreTestTask task) throws InterruptedException, ExecutionException {
        AsyncDataChangeEvent<YangInstanceIdentifier, NormalizedNode<?, ?>> change = task.getChangeEvent();

        assertContains(change.getCreatedData(), path(FOO_SIBLING));
        assertContains(change.getUpdatedData(), TOP_LEVEL);
        assertNotContains(change.getUpdatedData(), path(FOO));
        assertEmpty(change.getRemovedPaths());
    }
}
