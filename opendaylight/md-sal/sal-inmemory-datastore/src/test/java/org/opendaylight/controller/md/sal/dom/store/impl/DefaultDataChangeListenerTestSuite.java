/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.store.impl;

import java.util.concurrent.ExecutionException;

import org.junit.Test;
import org.opendaylight.controller.md.sal.dom.store.impl.DatastoreTestTask.WriteTransactionCustomizer;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadWriteTransaction;

public abstract class DefaultDataChangeListenerTestSuite extends AbstractDataChangeListenerTest {

    protected static final String FOO_SIBLING = "foo-sibling";

    abstract protected void customizeTask(DatastoreTestTask task);

    @Test
    public final void putTopLevelOneNested() throws InterruptedException, ExecutionException {

        DatastoreTestTask task = newTestTask().test(writeOneTopMultipleNested(FOO, BAR));
        customizeTask(task);
        task.run();
        putTopLevelOneNested(task);
    }

    @Test
    public final void existingTopWriteSibling() throws InterruptedException, ExecutionException {
        DatastoreTestTask task = newTestTask().setup(writeOneTopMultipleNested(FOO)).test(
                new WriteTransactionCustomizer() {
                    @Override
                    public void customize(final DOMStoreReadWriteTransaction tx) {
                        tx.write(path(FOO_SIBLING), topLevelList(FOO_SIBLING).build());
                    }
                });
        customizeTask(task);
        task.run();
        existingTopWriteSibling(task);
    }

    protected abstract void existingTopWriteSibling(DatastoreTestTask task) throws InterruptedException, ExecutionException;


    @Test
    public final void existingTopWriteTwoNested() throws InterruptedException, ExecutionException {
        DatastoreTestTask task = newTestTask().setup(writeOneTopMultipleNested(FOO)).test(
                new WriteTransactionCustomizer() {
                    @Override
                    public void customize(final DOMStoreReadWriteTransaction tx) {
                        tx.write(path(FOO,BAR), nestedList(BAR).build());
                        tx.write(path(FOO,BAZ), nestedList(BAZ).build());
                    }
                });
        customizeTask(task);
        task.run();
        existingTopWriteTwoNested(task);
    }

    protected abstract void existingTopWriteTwoNested(DatastoreTestTask task) throws InterruptedException, ExecutionException;


    @Test
    public final void existingOneNestedWriteAdditionalNested() throws InterruptedException, ExecutionException {
        DatastoreTestTask task = newTestTask().setup(writeOneTopMultipleNested(FOO, BAR)).test(
                new WriteTransactionCustomizer() {
                    @Override
                    public void customize(final DOMStoreReadWriteTransaction tx) {
                        tx.write(path(FOO,BAZ), nestedList(BAZ).build());
                    }
                });
        customizeTask(task);
        task.run();
        existingOneNestedWriteAdditionalNested(task);
    }

    protected abstract void existingOneNestedWriteAdditionalNested(DatastoreTestTask task) throws InterruptedException, ExecutionException;

    protected abstract void putTopLevelOneNested(DatastoreTestTask task) throws InterruptedException,
            ExecutionException;

    @Test
    public final void replaceTopLevelNestedChanged() throws InterruptedException, ExecutionException {
        DatastoreTestTask task = newTestTask().setup(writeOneTopMultipleNested(FOO, BAR)).test(
                writeOneTopMultipleNested(FOO, BAZ));
        customizeTask(task);
        task.run();
        replaceTopLevelNestedChanged(task);
    }

    protected abstract void replaceTopLevelNestedChanged(DatastoreTestTask task) throws InterruptedException,
            ExecutionException;

    @Test
    public final void putTopLevelWithTwoNested() throws InterruptedException, ExecutionException {

        DatastoreTestTask task = newTestTask().test(writeOneTopMultipleNested(FOO, BAR, BAZ));
        customizeTask(task);
        task.run();
        putTopLevelWithTwoNested(task);
    }

    protected abstract void putTopLevelWithTwoNested(DatastoreTestTask task) throws InterruptedException,
            ExecutionException;

    @Test
    public final void twoNestedExistsOneIsDeleted() throws InterruptedException, ExecutionException {

        DatastoreTestTask task = newTestTask().setup(writeOneTopMultipleNested(FOO, BAR, BAZ)).test(
                deleteNested(FOO, BAZ));
        customizeTask(task);
        task.run();
        twoNestedExistsOneIsDeleted(task);
    }

    protected abstract void twoNestedExistsOneIsDeleted(DatastoreTestTask task) throws InterruptedException,
            ExecutionException;

    @Test
    public final void nestedListExistsRootDeleted() throws InterruptedException, ExecutionException {

        DatastoreTestTask task = newTestTask().cleanup(null).setup(writeOneTopMultipleNested(FOO, BAR, BAZ))
                .test(DatastoreTestTask.simpleDelete(TOP_LEVEL));
        customizeTask(task);
        task.run();
        nestedListExistsRootDeleted(task);
    }

    protected abstract void nestedListExistsRootDeleted(DatastoreTestTask task) throws InterruptedException,
            ExecutionException;
}
