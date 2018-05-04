/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.store.impl;

import static org.opendaylight.controller.md.sal.dom.store.impl.DatastoreTestTask.added;
import static org.opendaylight.controller.md.sal.dom.store.impl.DatastoreTestTask.deleted;
import static org.opendaylight.controller.md.sal.dom.store.impl.DatastoreTestTask.replaced;

import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.two.level.list.TopLevelList;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

public class WildcardedScopeBaseTest extends DefaultDataTreeChangeListenerTestSuite {

    private static final YangInstanceIdentifier TOP_LEVEL_LIST_ALL = TOP_LEVEL.node(TopLevelList.QNAME).node(
            TopLevelList.QNAME);

    @Override
    protected void putTopLevelOneNestedSetup(final DatastoreTestTask task) {
        task.changeListener(TOP_LEVEL_LIST_ALL, added(path(FOO)));
    }

    @Override
    protected void putTopLevelOneNestedVerify(final DatastoreTestTask task) {
        task.verifyChangeEvents();
    }

    @Override
    protected void replaceTopLevelNestedSetup(DatastoreTestTask task) {
        task.changeListener(TOP_LEVEL_LIST_ALL, added(path(FOO)), replaced(path(FOO)));
    }

    @Override
    protected void replaceTopLevelNestedVerify(final DatastoreTestTask task) {
        task.verifyChangeEvents();
    }

    @Override
    protected void putTopLevelWithTwoNestedSetup(final DatastoreTestTask task) {
        task.changeListener(TOP_LEVEL_LIST_ALL, added(path(FOO)));
    }

    @Override
    protected void putTopLevelWithTwoNestedVerify(final DatastoreTestTask task) {
        task.verifyChangeEvents();
    }

    @Override
    protected void twoNestedExistsOneIsDeletedSetup(DatastoreTestTask task) {
        task.changeListener(TOP_LEVEL_LIST_ALL, added(path(FOO)));
    }

    @Override
    protected void twoNestedExistsOneIsDeletedVerify(final DatastoreTestTask task) {
        task.verifyChangeEvents();
    }

    @Override
    protected void nestedListExistsRootDeletedSetup(DatastoreTestTask task) {
        task.changeListener(TOP_LEVEL_LIST_ALL, added(path(FOO)), deleted(path(FOO)));
    }

    @Override
    protected void nestedListExistsRootDeletedVerify(final DatastoreTestTask task) {
        task.verifyChangeEvents();
    }

    @Override
    protected void existingOneNestedWriteAdditionalNestedSetup(DatastoreTestTask task) {
        task.changeListener(TOP_LEVEL_LIST_ALL, added(path(FOO)));
    }

    @Override
    protected void existingOneNestedWriteAdditionalNestedVerify(final DatastoreTestTask task) {
        task.verifyChangeEvents();
    }

    @Override
    protected void existingTopWriteTwoNestedSetup(DatastoreTestTask task) {
        task.changeListener(TOP_LEVEL_LIST_ALL, added(path(FOO)));
    }

    @Override
    protected void existingTopWriteTwoNestedVerify(final DatastoreTestTask task) {
        task.verifyChangeEvents();
    }

    @Override
    protected void existingTopWriteSiblingSetup(DatastoreTestTask task) {
        task.changeListener(TOP_LEVEL_LIST_ALL, added(path(FOO)), added(path(FOO_SIBLING)));
    }

    @Override
    protected void existingTopWriteSiblingVerify(final DatastoreTestTask task) {
        task.verifyChangeEvents();
    }
}
