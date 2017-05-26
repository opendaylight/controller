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
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.two.level.list.TopLevelList;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

public class WildcardedScopeBaseTest extends DefaultDataChangeListenerTestSuite {

    private static final YangInstanceIdentifier TOP_LEVEL_LIST_ALL = TOP_LEVEL.node(TopLevelList.QNAME).node(
            TopLevelList.QNAME);

    @Override
    protected void customizeTask(final DatastoreTestTask task) {
        task.changeListener(TOP_LEVEL_LIST_ALL, DataChangeScope.BASE);
    }

    @Override
    public void putTopLevelOneNested(final DatastoreTestTask task) throws InterruptedException, ExecutionException {

        AsyncDataChangeEvent<YangInstanceIdentifier, NormalizedNode<?, ?>> change = task.getChangeEvent();

        assertNotNull(change);

        /*
         * Created data must not contain nested-list item, since that is two-level deep.
         */
        assertNotContains(change.getCreatedData(), TOP_LEVEL,path(FOO, BAR));
        assertContains(change.getCreatedData(), path(FOO) );

        assertEmpty(change.getUpdatedData());
        assertEmpty(change.getRemovedPaths());

    }

    @Override
    public void replaceTopLevelNestedChanged(final DatastoreTestTask task) throws InterruptedException,
            ExecutionException {

        AsyncDataChangeEvent<YangInstanceIdentifier, NormalizedNode<?, ?>> change = task.getChangeEvent();
        assertNotNull(change);
        /*
         * Created data must NOT contain nested-list item since scope is base, and change is two
         * level deep.
         */
        assertNotContains(change.getCreatedData(), path(FOO, BAZ));
        assertContains(change.getUpdatedData(), path(FOO));
        assertNotContains(change.getUpdatedData(), TOP_LEVEL);
        /*
         * Removed data must NOT contain nested-list item since scope is base, and change is two
         * level deep.
         */
        assertNotContains(change.getRemovedPaths(), path(FOO, BAR));

    }

    @Override
    protected void putTopLevelWithTwoNested(final DatastoreTestTask task) throws InterruptedException,
            ExecutionException {

        AsyncDataChangeEvent<YangInstanceIdentifier, NormalizedNode<?, ?>> change = task.getChangeEvent();
        assertNotNull(change);
        assertFalse(change.getCreatedData().isEmpty());

        // Base event should contain only changed item, no details about child.
        assertContains(change.getCreatedData(), path(FOO));
        assertNotContains(change.getCreatedData(), TOP_LEVEL,path(FOO, BAR), path(FOO, BAZ));
        assertEmpty(change.getUpdatedData());
        assertEmpty(change.getRemovedPaths());

    }

    @Override
    protected void twoNestedExistsOneIsDeleted(final DatastoreTestTask task) throws InterruptedException,
            ExecutionException {

        /*
         * Base listener should be notified only and only if actual node changed its state,
         * since deletion of child, did not result in change of node we are listening
         * for, we should not be getting data change event
         * and this means settable future containing receivedDataChangeEvent is not done.
         *
         */
        task.verifyNoChangeEvent();
    }

    @Override
    public void nestedListExistsRootDeleted(final DatastoreTestTask task) throws InterruptedException,
            ExecutionException {

        AsyncDataChangeEvent<YangInstanceIdentifier, NormalizedNode<?, ?>> change = task.getChangeEvent();

        assertEmpty(change.getCreatedData());
        assertEmpty(change.getUpdatedData());

        assertNotContains(change.getUpdatedData(), TOP_LEVEL);
        /*
         *  Scope base listener event should contain top-level-list item and nested list path
         *  and should not contain baz, bar which are two-level deep
         */
        assertContains(change.getRemovedPaths(), path(FOO));
        assertNotContains(change.getRemovedPaths(),path(FOO, BAZ),path(FOO,BAR));
    }

    @Override
    protected void existingOneNestedWriteAdditionalNested(final DatastoreTestTask task) {
        /*
         * One listener should be notified only and only if actual node changed its state,
         * since deletion of nested child (in this case /nested-list/nested-list[foo],
         * did not result in change of node we are listening
         * for, we should not be getting data change event
         * and this means settable future containing receivedDataChangeEvent is not done.
         *
         */
        task.verifyNoChangeEvent();
    }

    @Override
    protected void existingTopWriteTwoNested(final DatastoreTestTask task) throws InterruptedException, ExecutionException {
        /*
         * One listener should be notified only and only if actual node changed its state,
         * since deletion of nested child (in this case /nested-list/nested-list[foo],
         * did not result in change of node we are listening
         * for, we should not be getting data change event
         * and this means settable future containing receivedDataChangeEvent is not done.
         *
         */
        task.verifyNoChangeEvent();
    }

    @Override
    protected void existingTopWriteSibling(final DatastoreTestTask task) throws InterruptedException, ExecutionException {
        AsyncDataChangeEvent<YangInstanceIdentifier, NormalizedNode<?, ?>> change = task.getChangeEvent();

        assertContains(change.getCreatedData(), path(FOO_SIBLING));
        assertNotContains(change.getUpdatedData(), path(FOO), TOP_LEVEL);
        assertEmpty(change.getRemovedPaths());
    }
}
