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
import static org.opendaylight.controller.md.sal.dom.store.impl.DatastoreTestTask.subtreeModified;

public class RootScopeSubtreeTest extends DefaultDataTreeChangeListenerTestSuite {

    @Override
    protected void putTopLevelOneNestedSetup(final DatastoreTestTask task) {
        task.changeListener(TOP_LEVEL, added(TOP_LEVEL));
    }

    @Override
    protected void putTopLevelOneNestedVerify(final DatastoreTestTask task) {
        task.verifyChangeEvents();
    }

    @Override
    protected void replaceTopLevelNestedSetup(DatastoreTestTask task) {
        task.changeListener(TOP_LEVEL, added(TOP_LEVEL), replaced(TOP_LEVEL));
    }

    @Override
    protected void replaceTopLevelNestedVerify(final DatastoreTestTask task) {
        task.verifyChangeEvents();
    }

    @Override
    protected void putTopLevelWithTwoNestedSetup(final DatastoreTestTask task) {
        task.changeListener(TOP_LEVEL, added(TOP_LEVEL));
    }

    @Override
    protected void putTopLevelWithTwoNestedVerify(final DatastoreTestTask task) {
        task.verifyChangeEvents();
    }

    @Override
    protected void twoNestedExistsOneIsDeletedSetup(DatastoreTestTask task) {
        task.changeListener(TOP_LEVEL, added(TOP_LEVEL), subtreeModified(TOP_LEVEL));
    }

    @Override
    protected void twoNestedExistsOneIsDeletedVerify(final DatastoreTestTask task) {
        task.verifyChangeEvents();
    }

    @Override
    protected void nestedListExistsRootDeletedSetup(DatastoreTestTask task) {
        task.changeListener(TOP_LEVEL, added(TOP_LEVEL), deleted(TOP_LEVEL));
    }

    @Override
    protected void nestedListExistsRootDeletedVerify(final DatastoreTestTask task) {
        task.verifyChangeEvents();
    }

    @Override
    protected void existingOneNestedWriteAdditionalNestedSetup(DatastoreTestTask task) {
        task.changeListener(TOP_LEVEL, added(TOP_LEVEL), subtreeModified(TOP_LEVEL));
    }

    @Override
    protected void existingOneNestedWriteAdditionalNestedVerify(final DatastoreTestTask task) {
        task.verifyChangeEvents();
    }

    @Override
    protected void existingTopWriteTwoNestedSetup(DatastoreTestTask task) {
        task.changeListener(TOP_LEVEL, added(TOP_LEVEL), subtreeModified(TOP_LEVEL));
    }

    @Override
    protected void existingTopWriteTwoNestedVerify(final DatastoreTestTask task) {
        task.verifyChangeEvents();
    }

    @Override
    protected void existingTopWriteSiblingSetup(DatastoreTestTask task) {
        task.changeListener(TOP_LEVEL, added(TOP_LEVEL), subtreeModified(TOP_LEVEL));
    }

    @Override
    protected void existingTopWriteSiblingVerify(final DatastoreTestTask task) {
        task.verifyChangeEvents();
    }
}
