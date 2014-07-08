/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.store.impl;

import static org.junit.Assert.assertTrue;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeListener;
import org.opendaylight.controller.sal.core.spi.data.DOMStore;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadWriteTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreThreePhaseCommitCohort;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.SettableFuture;

public class DatastoreTestTask {

    private final DOMStore store;
    private AsyncDataChangeListener<InstanceIdentifier, NormalizedNode<?, ?>> changeListener;

    private WriteTransactionCustomizer setup;
    private WriteTransactionCustomizer write;
    private ReadTransactionVerifier read;
    private WriteTransactionCustomizer cleanup;
    private InstanceIdentifier changePath;
    private DataChangeScope changeScope;
    private boolean postSetup = false;
    private final ChangeEventListener internalListener;

    public DatastoreTestTask(final DOMStore datastore) {
        this.store = datastore;
        internalListener = new ChangeEventListener();
    }

    public DatastoreTestTask changeListener(final InstanceIdentifier path, final DataChangeScope scope,
            final AsyncDataChangeListener<InstanceIdentifier, NormalizedNode<?, ?>> changeListener) {
        this.changeListener = changeListener;
        this.changePath = path;
        this.changeScope = scope;
        return this;
    }

    public DatastoreTestTask changeListener(final InstanceIdentifier path, final DataChangeScope scope) {
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

    public void run() throws InterruptedException, ExecutionException {
        if (setup != null) {
            execute(setup);
        }
        ListenerRegistration<ChangeEventListener> registration = null;
        if (changePath != null) {
            registration = store.registerChangeListener(changePath, internalListener, changeScope);
        }

        Preconditions.checkState(write != null, "Write Transaction must be set.");
        postSetup = true;
        execute(write);
        if (registration != null) {
            registration.close();
        }
        if (changeListener != null) {
            changeListener.onDataChanged(internalListener.receivedChange.get());
        }
        if (read != null) {
            read.verify(store.newReadOnlyTransaction());
        }
        if (cleanup != null) {
            execute(cleanup);
        }
    }

    public Future<AsyncDataChangeEvent<InstanceIdentifier, NormalizedNode<?, ?>>> getChangeEvent() {
        return internalListener.receivedChange;
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
        public void customize(DOMStoreReadWriteTransaction tx);
    }

    public interface ReadTransactionVerifier {
        public void verify(DOMStoreReadTransaction tx);
    }

    private final class ChangeEventListener implements
            AsyncDataChangeListener<InstanceIdentifier, NormalizedNode<?, ?>> {

        protected final SettableFuture<AsyncDataChangeEvent<InstanceIdentifier, NormalizedNode<?, ?>>> receivedChange = SettableFuture
                .create();

        @Override
        public void onDataChanged(final AsyncDataChangeEvent<InstanceIdentifier, NormalizedNode<?, ?>> change) {
            if (postSetup) {
                receivedChange.set(change);
            }
        }
    }

    public static final WriteTransactionCustomizer simpleWrite(final InstanceIdentifier path,
            final NormalizedNode<?, ?> data) {
        return new WriteTransactionCustomizer() {

            @Override
            public void customize(final DOMStoreReadWriteTransaction tx) {
                tx.write(path, data);
            }
        };
    }

    public static final WriteTransactionCustomizer simpleMerge(final InstanceIdentifier path,
            final NormalizedNode<?, ?> data) {
        return new WriteTransactionCustomizer() {

            @Override
            public void customize(final DOMStoreReadWriteTransaction tx) {
                tx.merge(path, data);
            }
        };
    }

    public static final WriteTransactionCustomizer simpleDelete(final InstanceIdentifier path) {
        return new WriteTransactionCustomizer() {
            @Override
            public void customize(final DOMStoreReadWriteTransaction tx) {
                tx.delete(path);
            }
        };
    }
}
