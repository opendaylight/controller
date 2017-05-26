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
import static org.junit.Assert.assertTrue;

import java.util.concurrent.ExecutionException;

import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.two.level.list.TopLevelList;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

public class WildcardedScopeSubtreeTest extends DefaultDataChangeListenerTestSuite {

    private static final YangInstanceIdentifier TOP_LEVEL_LIST_ALL = TOP_LEVEL.node(TopLevelList.QNAME).node(
            TopLevelList.QNAME);

    @Override
    protected void customizeTask(final DatastoreTestTask task) {
        task.changeListener(TOP_LEVEL_LIST_ALL, DataChangeScope.SUBTREE);
    }

    @Override
    public void putTopLevelOneNested(final DatastoreTestTask task) throws InterruptedException, ExecutionException {

        AsyncDataChangeEvent<YangInstanceIdentifier, NormalizedNode<?, ?>> change = task.getChangeEvent();

        assertNotContains(change.getCreatedData(), TOP_LEVEL);
        assertContains(change.getCreatedData(), path(FOO), path(FOO, BAR));
        assertEmpty(change.getUpdatedData());
        assertEmpty(change.getRemovedPaths());

    }

    @Override
    public void replaceTopLevelNestedChanged(final DatastoreTestTask task) throws InterruptedException,
            ExecutionException {

        AsyncDataChangeEvent<YangInstanceIdentifier, NormalizedNode<?, ?>> change = task.getChangeEvent();
        assertNotNull(change);

        assertContains(change.getCreatedData(), path(FOO, BAZ));
        assertContains(change.getUpdatedData(), path(FOO));
        assertNotContains(change.getUpdatedData(), TOP_LEVEL);
        assertContains(change.getRemovedPaths(), path(FOO, BAR));

    }

    @Override
    protected void putTopLevelWithTwoNested(final DatastoreTestTask task) throws InterruptedException,
            ExecutionException {

        AsyncDataChangeEvent<YangInstanceIdentifier, NormalizedNode<?, ?>> change = task.getChangeEvent();
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

        AsyncDataChangeEvent<YangInstanceIdentifier, NormalizedNode<?, ?>> change = task.getChangeEvent();
        assertNotNull(change);
        assertTrue(change.getCreatedData().isEmpty());
        assertContains(change.getUpdatedData(), path(FOO));
        assertNotContains(change.getUpdatedData(), TOP_LEVEL);
        assertContains(change.getRemovedPaths(),path(FOO, BAZ));
    }

    @Override
    public void nestedListExistsRootDeleted(final DatastoreTestTask task) throws InterruptedException,
            ExecutionException {

        AsyncDataChangeEvent<YangInstanceIdentifier, NormalizedNode<?, ?>> change = task.getChangeEvent();

        assertEmpty(change.getCreatedData());
        assertEmpty(change.getUpdatedData());

        assertNotContains(change.getUpdatedData(), TOP_LEVEL);
        assertContains(change.getRemovedPaths(), path(FOO),path(FOO, BAZ),path(FOO,BAR));
    }

    @Override
    protected void existingOneNestedWriteAdditionalNested(final DatastoreTestTask task) throws InterruptedException, ExecutionException {
        AsyncDataChangeEvent<YangInstanceIdentifier, NormalizedNode<?, ?>> change = task.getChangeEvent();

        assertContains(change.getCreatedData(), path(FOO,BAZ));
        assertNotContains(change.getCreatedData(), path(FOO,BAR));
        assertContains(change.getUpdatedData(), path(FOO));
        assertNotContains(change.getUpdatedData(), TOP_LEVEL);
        assertEmpty(change.getRemovedPaths());
    }

    @Override
    protected void existingTopWriteTwoNested(final DatastoreTestTask task) throws InterruptedException, ExecutionException {
        AsyncDataChangeEvent<YangInstanceIdentifier, NormalizedNode<?, ?>> change = task.getChangeEvent();

        assertContains(change.getCreatedData(), path(FOO,BAR),path(FOO,BAZ));
        assertContains(change.getUpdatedData(), path(FOO));
        assertNotContains(change.getUpdatedData(), TOP_LEVEL, path(FOO,BAR));
        assertEmpty(change.getRemovedPaths());
    }

    @Override
    protected void existingTopWriteSibling(final DatastoreTestTask task) throws InterruptedException, ExecutionException {
        AsyncDataChangeEvent<YangInstanceIdentifier, NormalizedNode<?, ?>> change = task.getChangeEvent();

        assertContains(change.getCreatedData(), path(FOO_SIBLING));
        assertNotContains(change.getUpdatedData(), path(FOO), TOP_LEVEL);
        assertEmpty(change.getRemovedPaths());
    }
}
