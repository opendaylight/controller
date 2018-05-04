/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.store.impl;

import org.junit.Test;

/**
 * Base template for a test suite for testing DataTreeChangeListener functionality.
 */
public abstract class DefaultDataTreeChangeListenerTestSuite extends AbstractDataTreeChangeListenerTest {

    protected static final String FOO_SIBLING = "foo-sibling";

    @Test
    public final void putTopLevelOneNested() throws Exception {

        DatastoreTestTask task = newTestTask().test(writeOneTopMultipleNested(FOO, BAR));
        putTopLevelOneNestedSetup(task);
        task.run();
        putTopLevelOneNestedVerify(task);
    }

    protected abstract void putTopLevelOneNestedSetup(DatastoreTestTask task);

    protected abstract void putTopLevelOneNestedVerify(DatastoreTestTask task);

    @Test
    public final void existingTopWriteSibling() throws Exception {
        DatastoreTestTask task = newTestTask().setup(writeOneTopMultipleNested(FOO)).test(
            tx -> tx.write(path(FOO_SIBLING), topLevelList(FOO_SIBLING).build()));
        existingTopWriteSiblingSetup(task);
        task.run();
        existingTopWriteSiblingVerify(task);
    }

    protected abstract void existingTopWriteSiblingSetup(DatastoreTestTask task);

    protected abstract void existingTopWriteSiblingVerify(DatastoreTestTask task);

    @Test
    public final void existingTopWriteTwoNested() throws Exception {
        DatastoreTestTask task = newTestTask().setup(writeOneTopMultipleNested(FOO)).test(
            tx -> {
                tx.write(path(FOO,BAR), nestedList(BAR).build());
                tx.write(path(FOO,BAZ), nestedList(BAZ).build());
            });
        existingTopWriteTwoNestedSetup(task);
        task.run();
        existingTopWriteTwoNestedVerify(task);
    }

    protected abstract void existingTopWriteTwoNestedSetup(DatastoreTestTask task);

    protected abstract void existingTopWriteTwoNestedVerify(DatastoreTestTask task);


    @Test
    public final void existingOneNestedWriteAdditionalNested() throws Exception {
        DatastoreTestTask task = newTestTask().setup(writeOneTopMultipleNested(FOO, BAR)).test(
            tx -> tx.write(path(FOO,BAZ), nestedList(BAZ).build()));
        existingOneNestedWriteAdditionalNestedSetup(task);
        task.run();
        existingOneNestedWriteAdditionalNestedVerify(task);
    }

    protected abstract void existingOneNestedWriteAdditionalNestedSetup(DatastoreTestTask task);

    protected abstract void existingOneNestedWriteAdditionalNestedVerify(DatastoreTestTask task);

    @Test
    public final void replaceTopLevelNestedChanged() throws Exception {
        DatastoreTestTask task = newTestTask().setup(writeOneTopMultipleNested(FOO, BAR)).test(
                writeOneTopMultipleNested(FOO, BAZ));
        replaceTopLevelNestedSetup(task);
        task.run();
        replaceTopLevelNestedVerify(task);
    }

    protected abstract void replaceTopLevelNestedSetup(DatastoreTestTask task);

    protected abstract void replaceTopLevelNestedVerify(DatastoreTestTask task);

    @Test
    public final void putTopLevelWithTwoNested() throws Exception {

        DatastoreTestTask task = newTestTask().test(writeOneTopMultipleNested(FOO, BAR, BAZ));
        putTopLevelWithTwoNestedSetup(task);
        task.run();
        putTopLevelWithTwoNestedVerify(task);
    }

    protected abstract void putTopLevelWithTwoNestedSetup(DatastoreTestTask task);

    protected abstract void putTopLevelWithTwoNestedVerify(DatastoreTestTask task);

    @Test
    public final void twoNestedExistsOneIsDeleted() throws Exception {

        DatastoreTestTask task = newTestTask().setup(writeOneTopMultipleNested(FOO, BAR, BAZ)).test(
                deleteNested(FOO, BAZ));
        twoNestedExistsOneIsDeletedSetup(task);
        task.run();
        twoNestedExistsOneIsDeletedVerify(task);
    }

    protected abstract void twoNestedExistsOneIsDeletedSetup(DatastoreTestTask task);

    protected abstract void twoNestedExistsOneIsDeletedVerify(DatastoreTestTask task);

    @Test
    public final void nestedListExistsRootDeleted() throws Exception {

        DatastoreTestTask task = newTestTask().cleanup(null).setup(writeOneTopMultipleNested(FOO, BAR, BAZ))
                .test(DatastoreTestTask.simpleDelete(TOP_LEVEL));
        nestedListExistsRootDeletedSetup(task);
        task.run();
        nestedListExistsRootDeletedVerify(task);
    }

    protected abstract void nestedListExistsRootDeletedSetup(DatastoreTestTask task);

    protected abstract void nestedListExistsRootDeletedVerify(DatastoreTestTask task);
}
