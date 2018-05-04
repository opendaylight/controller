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

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.SettableFuture;
import com.google.common.util.concurrent.Uninterruptibles;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeChangeListener;
import org.opendaylight.controller.sal.core.spi.data.DOMStore;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadWriteTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreThreePhaseCommitCohort;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreTreeChangePublisher;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;
import org.opendaylight.yangtools.yang.data.api.schema.tree.ModificationType;

public class DatastoreTestTask {

    private final DOMStore store;

    private WriteTransactionCustomizer setup;
    private WriteTransactionCustomizer write;
    private ReadTransactionVerifier read;
    private WriteTransactionCustomizer cleanup;
    private YangInstanceIdentifier changePath;
    private DOMStoreTreeChangePublisher storeTreeChangePublisher;
    private ChangeEventListener internalListener;
    private final TestDCLExecutorService dclExecutorService;

    public DatastoreTestTask(final DOMStore datastore, final TestDCLExecutorService dclExecutorService) {
        this.store = datastore;
        this.dclExecutorService = dclExecutorService;
    }

    @SafeVarargs
    public final DatastoreTestTask changeListener(final YangInstanceIdentifier path,
            Function<DataTreeCandidate, Boolean>... matchers) {
        assertTrue(store instanceof DOMStoreTreeChangePublisher);
        this.storeTreeChangePublisher = (DOMStoreTreeChangePublisher)store;
        this.changePath = path;
        this.internalListener = new ChangeEventListener(matchers);
        return this;
    }

    public static Function<DataTreeCandidate, Boolean> added(YangInstanceIdentifier path) {
        return candidate -> candidate.getRootNode().getModificationType() == ModificationType.WRITE
                && path.equals(candidate.getRootPath()) && !candidate.getRootNode().getDataBefore().isPresent()
                && candidate.getRootNode().getDataAfter().isPresent();
    }

    public static Function<DataTreeCandidate, Boolean> replaced(YangInstanceIdentifier path) {
        return candidate -> candidate.getRootNode().getModificationType() == ModificationType.WRITE
                && path.equals(candidate.getRootPath()) && candidate.getRootNode().getDataBefore().isPresent()
                && candidate.getRootNode().getDataAfter().isPresent();
    }

    public static Function<DataTreeCandidate, Boolean> deleted(YangInstanceIdentifier path) {
        return candidate -> candidate.getRootNode().getModificationType() == ModificationType.DELETE
                && path.equals(candidate.getRootPath()) && candidate.getRootNode().getDataBefore().isPresent()
                && !candidate.getRootNode().getDataAfter().isPresent();
    }

    public static Function<DataTreeCandidate, Boolean> subtreeModified(YangInstanceIdentifier path) {
        return candidate -> candidate.getRootNode().getModificationType() == ModificationType.SUBTREE_MODIFIED
                && path.equals(candidate.getRootPath()) && candidate.getRootNode().getDataBefore().isPresent()
                && candidate.getRootNode().getDataAfter().isPresent();
    }

    public DatastoreTestTask setup(final WriteTransactionCustomizer customizer) {
        this.setup = customizer;
        return this;
    }

    public DatastoreTestTask test(final WriteTransactionCustomizer customizer) {
        this.write = customizer;
        return this;
    }

    public DatastoreTestTask read(final ReadTransactionVerifier customizer) {
        this.read = customizer;
        return this;
    }

    public DatastoreTestTask cleanup(final WriteTransactionCustomizer customizer) {
        this.cleanup = customizer;
        return this;
    }

    public void run() throws Exception {
        if (setup != null) {
            execute(setup);
        }
        ListenerRegistration<ChangeEventListener> registration = null;
        if (changePath != null) {
            registration = storeTreeChangePublisher.registerTreeChangeListener(changePath, internalListener);
        }

        Preconditions.checkState(write != null, "Write Transaction must be set.");

        dclExecutorService.afterTestSetup();

        execute(write);
        if (registration != null) {
            registration.close();
        }

        if (read != null) {
            read.verify(store.newReadOnlyTransaction());
        }
        if (cleanup != null) {
            execute(cleanup);
        }
    }

    public void verifyChangeEvents() {
        internalListener.verifyChangeEvents();
    }

    public void verifyNoChangeEvent() {
        internalListener.verifyNoChangeEvent();
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

    private final class ChangeEventListener implements DOMDataTreeChangeListener {

        final SettableFuture<Collection<DataTreeCandidate>> future = SettableFuture.create();
        final Collection<DataTreeCandidate> accumulatedChanges = new ArrayList<>();
        final Function<DataTreeCandidate, Boolean>[] matchers;
        final int expChangeCount;

        ChangeEventListener(Function<DataTreeCandidate, Boolean>[] matchers) {
            this.expChangeCount = matchers.length;
            this.matchers = matchers;
        }

        Collection<DataTreeCandidate> changes() {
            try {
                Collection<DataTreeCandidate> changes = internalListener.future.get(10, TimeUnit.SECONDS);
                Uninterruptibles.sleepUninterruptibly(500, TimeUnit.MILLISECONDS);
                return changes;
            } catch (TimeoutException e) {
                throw new AssertionError(String.format(
                        "Data tree change notifications not received. Expected: %s. Actual: %s - %s",
                        expChangeCount, accumulatedChanges.size(), accumulatedChanges), e);
            } catch (InterruptedException | ExecutionException e) {
                throw new AssertionError("Data tree change notifications failed", e);
            }
        }

        void verifyChangeEvents() {
            Collection<DataTreeCandidate> changes = new ArrayList<>(changes());
            Iterator<DataTreeCandidate> iter = changes.iterator();
            while (iter.hasNext()) {
                DataTreeCandidate dataTreeModification = iter.next();
                for (Function<DataTreeCandidate, Boolean> matcher: matchers) {
                    if (matcher.apply(dataTreeModification)) {
                        iter.remove();
                        break;
                    }
                }
            }

            if (!changes.isEmpty()) {
                DataTreeCandidate mod = changes.iterator().next();
                fail(String.format("Received unexpected notification: type: %s, path: %s, before: %s, after: %s",
                        mod.getRootNode().getModificationType(), mod.getRootPath(),
                        mod.getRootNode().getDataBefore(), mod.getRootNode().getDataAfter()));
            }
        }

        void verifyNoChangeEvent() {
            try {
                Object unexpected = internalListener.future.get(500, TimeUnit.MILLISECONDS);
                fail("Got unexpected Data tree change notifications: " + unexpected);
            } catch (TimeoutException e) {
                // Expected
            } catch (InterruptedException | ExecutionException e) {
                throw new AssertionError("Data tree change notifications failed", e);
            }
        }

        @Override
        public void onDataTreeChanged(Collection<DataTreeCandidate> changes) {
            synchronized (accumulatedChanges) {
                accumulatedChanges.addAll(changes);
                if (expChangeCount == accumulatedChanges.size()) {
                    future.set(new ArrayList<>(accumulatedChanges));
                }
            }
        }
    }

    public static final WriteTransactionCustomizer simpleWrite(final YangInstanceIdentifier path,
            final NormalizedNode<?, ?> data) {
        return tx -> tx.write(path, data);
    }

    public static final WriteTransactionCustomizer simpleMerge(final YangInstanceIdentifier path,
            final NormalizedNode<?, ?> data) {
        return tx -> tx.merge(path, data);
    }

    public static final WriteTransactionCustomizer simpleDelete(final YangInstanceIdentifier path) {
        return tx -> tx.delete(path);
    }
}
