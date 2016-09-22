/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.store.impl;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeListener;
import org.opendaylight.controller.sal.core.spi.data.DOMStore;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadWriteTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreThreePhaseCommitCohort;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.SettableFuture;

public class DatastoreTestTask {

    private final DOMStore store;
    private AsyncDataChangeListener<YangInstanceIdentifier, NormalizedNode<?, ?>> changeListener;

    private WriteTransactionCustomizer setup;
    private WriteTransactionCustomizer write;
    private ReadTransactionVerifier read;
    private WriteTransactionCustomizer cleanup;
    private YangInstanceIdentifier changePath;
    private DataChangeScope changeScope;
    private volatile boolean postSetup = false;
    private final ChangeEventListener internalListener;
    private final TestDCLExecutorService dclExecutorService;

    public DatastoreTestTask(final DOMStore datastore, final TestDCLExecutorService dclExecutorService) {
        this.store = datastore;
        this.dclExecutorService = dclExecutorService;
        internalListener = new ChangeEventListener();
    }

    public DatastoreTestTask changeListener(final YangInstanceIdentifier path, final DataChangeScope scope,
            final AsyncDataChangeListener<YangInstanceIdentifier, NormalizedNode<?, ?>> changeListener) {
        this.changeListener = changeListener;
        this.changePath = path;
        this.changeScope = scope;
        return this;
    }

    public DatastoreTestTask changeListener(final YangInstanceIdentifier path, final DataChangeScope scope) {
        this.changePath = path;
        this.changeScope = scope;
        return this;
    }

    public DatastoreTestTask setup(final WriteTransactionCustomizer setup) {
        this.setup = setup;
        return this;
    }

    public DatastoreTestTask test(final WriteTransactionCustomizer write) {
        this.write = write;
        return this;
    }

    public DatastoreTestTask read(final ReadTransactionVerifier read) {
        this.read = read;
        return this;
    }

    public DatastoreTestTask cleanup(final WriteTransactionCustomizer cleanup) {
        this.cleanup = cleanup;
        return this;
    }

    public void run() throws InterruptedException, ExecutionException, TimeoutException {
        if (setup != null) {
            execute(setup);
        }
        ListenerRegistration<ChangeEventListener> registration = null;
        if (changePath != null) {
            registration = store.registerChangeListener(changePath, internalListener, changeScope);
        }

        Preconditions.checkState(write != null, "Write Transaction must be set.");

        postSetup = true;
        dclExecutorService.afterTestSetup();

        execute(write);
        if (registration != null) {
            registration.close();
        }

        if (changeListener != null) {
            changeListener.onDataChanged(getChangeEvent());
        }
        if (read != null) {
            read.verify(store.newReadOnlyTransaction());
        }
        if (cleanup != null) {
            execute(cleanup);
        }
    }

    public AsyncDataChangeEvent<YangInstanceIdentifier, NormalizedNode<?, ?>> getChangeEvent() {
        try {
            return internalListener.receivedChange.get(10, TimeUnit.SECONDS);
        } catch( Exception e ) {
            fail( "Error getting the AsyncDataChangeEvent from the Future: " + e );
        }

        // won't get here
        return null;
    }

    public void verifyNoChangeEvent() {
        try {
            Object unexpected = internalListener.receivedChange.get(500, TimeUnit.MILLISECONDS);
            fail( "Got unexpected AsyncDataChangeEvent from the Future: " + unexpected );
        } catch( TimeoutException e ) {
            // Expected
        } catch( Exception e ) {
            fail( "Error getting the AsyncDataChangeEvent from the Future: " + e );
        }
    }

    private void execute(final WriteTransactionCustomizer writeCustomizer) throws InterruptedException,
            ExecutionException {
        DOMStoreReadWriteTransaction tx = store.newReadWriteTransaction();
        writeCustomizer.customize(tx);
        DOMStoreThreePhaseCommitCohort cohort = tx.ready();
        assertTrue(cohort.canCommit().get());
        cohort.preCommit().get();
        cohort.commit().get();
    }

    public interface WriteTransactionCustomizer {
        void customize(DOMStoreReadWriteTransaction tx);
    }

    public interface ReadTransactionVerifier {
        void verify(DOMStoreReadTransaction tx);
    }

    private final class ChangeEventListener implements
            AsyncDataChangeListener<YangInstanceIdentifier, NormalizedNode<?, ?>> {

        protected final SettableFuture<AsyncDataChangeEvent<YangInstanceIdentifier, NormalizedNode<?, ?>>> receivedChange = SettableFuture
                .create();

        @Override
        public void onDataChanged(final AsyncDataChangeEvent<YangInstanceIdentifier, NormalizedNode<?, ?>> change) {
            if (postSetup) {
                receivedChange.set(change);
            }
        }
    }

    public static final WriteTransactionCustomizer simpleWrite(final YangInstanceIdentifier path,
            final NormalizedNode<?, ?> data) {
        return new WriteTransactionCustomizer() {

            @Override
            public void customize(final DOMStoreReadWriteTransaction tx) {
                tx.write(path, data);
            }
        };
    }

    public static final WriteTransactionCustomizer simpleMerge(final YangInstanceIdentifier path,
            final NormalizedNode<?, ?> data) {
        return new WriteTransactionCustomizer() {

            @Override
            public void customize(final DOMStoreReadWriteTransaction tx) {
                tx.merge(path, data);
            }
        };
    }

    public static final WriteTransactionCustomizer simpleDelete(final YangInstanceIdentifier path) {
        return new WriteTransactionCustomizer() {
            @Override
            public void customize(final DOMStoreReadWriteTransaction tx) {
                tx.delete(path);
            }
        };
    }
}
