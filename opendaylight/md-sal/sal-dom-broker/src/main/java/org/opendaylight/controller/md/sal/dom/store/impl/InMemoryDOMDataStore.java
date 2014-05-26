/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.store.impl;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.util.Collections;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicLong;

import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeListener;
import org.opendaylight.controller.md.sal.dom.store.impl.tree.DataPreconditionFailedException;
import org.opendaylight.controller.md.sal.dom.store.impl.tree.DataTree;
import org.opendaylight.controller.md.sal.dom.store.impl.tree.DataTreeCandidate;
import org.opendaylight.controller.md.sal.dom.store.impl.tree.DataTreeModification;
import org.opendaylight.controller.md.sal.dom.store.impl.tree.DataTreeSnapshot;
import org.opendaylight.controller.md.sal.dom.store.impl.tree.ListenerTree;
import org.opendaylight.controller.md.sal.dom.store.impl.tree.data.InMemoryDataTreeFactory;
import org.opendaylight.controller.sal.core.spi.data.DOMStore;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadWriteTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreThreePhaseCommitCohort;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreTransactionChain;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreWriteTransaction;
import org.opendaylight.yangtools.concepts.AbstractListenerRegistration;
import org.opendaylight.yangtools.concepts.Identifiable;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaContextListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Objects;
import com.google.common.base.Objects.ToStringHelper;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;

public class InMemoryDOMDataStore implements DOMStore, Identifiable<String>, SchemaContextListener {
    private static final Logger LOG = LoggerFactory.getLogger(InMemoryDOMDataStore.class);
    private final DataTree dataTree = InMemoryDataTreeFactory.getInstance().create();
    private final ListenerTree listenerTree = ListenerTree.create();
    private final AtomicLong txCounter = new AtomicLong(0);
    private final ListeningExecutorService executor;
    private final String name;

    public InMemoryDOMDataStore(final String name, final ListeningExecutorService executor) {
        this.name = Preconditions.checkNotNull(name);
        this.executor = Preconditions.checkNotNull(executor);
    }

    @Override
    public final String getIdentifier() {
        return name;
    }

    @Override
    public DOMStoreReadTransaction newReadOnlyTransaction() {
        return new SnapshotBackedReadTransaction(nextIdentifier(), dataTree.takeSnapshot());
    }

    @Override
    public DOMStoreReadWriteTransaction newReadWriteTransaction() {
        return new SnapshotBackedReadWriteTransaction(nextIdentifier(), dataTree.takeSnapshot(), this);
    }

    @Override
    public DOMStoreWriteTransaction newWriteOnlyTransaction() {
        return new SnapshotBackedWriteTransaction(nextIdentifier(), dataTree.takeSnapshot(), this);
    }

    @Override
    public DOMStoreTransactionChain createTransactionChain() {
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    @Override
    public synchronized void onGlobalContextUpdated(final SchemaContext ctx) {
        dataTree.setSchemaContext(ctx);
    }

    @Override
    public <L extends AsyncDataChangeListener<InstanceIdentifier, NormalizedNode<?, ?>>> ListenerRegistration<L> registerChangeListener(
            final InstanceIdentifier path, final L listener, final DataChangeScope scope) {

        /*
         * Make sure commit is not occurring right now. Listener has to be
         * registered and its state capture enqueued at a consistent point.
         *
         * FIXME: improve this to read-write lock, such that multiple listener
         * registrations can occur simultaneously
         */
        final DataChangeListenerRegistration<L> reg;
        synchronized (this) {
            LOG.debug("{}: Registering data change listener {} for {}", name, listener, path);

            reg = listenerTree.registerDataChangeListener(path, listener, scope);

            Optional<NormalizedNode<?, ?>> currentState = dataTree.takeSnapshot().readNode(path);
            if (currentState.isPresent()) {
                final NormalizedNode<?, ?> data = currentState.get();

                final DOMImmutableDataChangeEvent event = DOMImmutableDataChangeEvent.builder(DataChangeScope.BASE) //
                        .setAfter(data) //
                        .addCreated(path, data) //
                        .build();
                executor.submit(new ChangeListenerNotifyTask(Collections.singletonList(reg), event));
            }
        }

        return new AbstractListenerRegistration<L>(listener) {
            @Override
            protected void removeRegistration() {
                synchronized (InMemoryDOMDataStore.this) {
                    reg.close();
                }
            }
        };
    }

    private synchronized DOMStoreThreePhaseCommitCohort submit(final SnapshotBackedWriteTransaction writeTx) {
        LOG.debug("Tx: {} is submitted. Modifications: {}", writeTx.getIdentifier(), writeTx.getMutatedView());
        return new ThreePhaseCommitImpl(writeTx);
    }

    private Object nextIdentifier() {
        return name + "-" + txCounter.getAndIncrement();
    }

    private static abstract class AbstractDOMStoreTransaction implements DOMStoreTransaction {
        private final Object identifier;

        protected AbstractDOMStoreTransaction(final Object identifier) {
            this.identifier = identifier;
        }

        @Override
        public final Object getIdentifier() {
            return identifier;
        }

        @Override
        public final String toString() {
            return addToStringAttributes(Objects.toStringHelper(this)).toString();
        }

        /**
         * Add class-specific toString attributes.
         *
         * @param toStringHelper
         *            ToStringHelper instance
         * @return ToStringHelper instance which was passed in
         */
        protected ToStringHelper addToStringAttributes(final ToStringHelper toStringHelper) {
            return toStringHelper.add("id", identifier);
        }
    }

    private static final class SnapshotBackedReadTransaction extends AbstractDOMStoreTransaction implements
    DOMStoreReadTransaction {
        private DataTreeSnapshot stableSnapshot;

        public SnapshotBackedReadTransaction(final Object identifier, final DataTreeSnapshot snapshot) {
            super(identifier);
            this.stableSnapshot = Preconditions.checkNotNull(snapshot);
            LOG.debug("ReadOnly Tx: {} allocated with snapshot {}", identifier, snapshot);
        }

        @Override
        public void close() {
            LOG.debug("Store transaction: {} : Closed", getIdentifier());
            stableSnapshot = null;
        }

        @Override
        public ListenableFuture<Optional<NormalizedNode<?, ?>>> read(final InstanceIdentifier path) {
            checkNotNull(path, "Path must not be null.");
            checkState(stableSnapshot != null, "Transaction is closed");
            return Futures.immediateFuture(stableSnapshot.readNode(path));
        }
    }

    private static class SnapshotBackedWriteTransaction extends AbstractDOMStoreTransaction implements
    DOMStoreWriteTransaction {
        private DataTreeModification mutableTree;
        private InMemoryDOMDataStore store;
        private boolean ready = false;

        public SnapshotBackedWriteTransaction(final Object identifier, final DataTreeSnapshot snapshot,
                final InMemoryDOMDataStore store) {
            super(identifier);
            mutableTree = snapshot.newModification();
            this.store = store;
            LOG.debug("Write Tx: {} allocated with snapshot {}", identifier, snapshot);
        }

        @Override
        public void close() {
            LOG.debug("Store transaction: {} : Closed", getIdentifier());
            this.mutableTree = null;
            this.store = null;
        }

        @Override
        public void write(final InstanceIdentifier path, final NormalizedNode<?, ?> data) {
            checkNotReady();
            try {
                LOG.trace("Tx: {} Write: {}:{}", getIdentifier(), path, data);
                mutableTree.write(path, data);
                // FIXME: Add checked exception
            } catch (Exception e) {
                LOG.error("Tx: {}, failed to write {}:{} in {}", getIdentifier(), path, data, mutableTree, e);
            }
        }

        @Override
        public void merge(final InstanceIdentifier path, final NormalizedNode<?, ?> data) {
            checkNotReady();
            try {
                LOG.trace("Tx: {} Merge: {}:{}", getIdentifier(), path, data);
                mutableTree.merge(path, data);
                // FIXME: Add checked exception
            } catch (Exception e) {
                LOG.error("Tx: {}, failed to write {}:{} in {}", getIdentifier(), path, data, mutableTree, e);
            }
        }

        @Override
        public void delete(final InstanceIdentifier path) {
            checkNotReady();
            try {
                LOG.trace("Tx: {} Delete: {}", getIdentifier(), path);
                mutableTree.delete(path);
                // FIXME: Add checked exception
            } catch (Exception e) {
                LOG.error("Tx: {}, failed to delete {} in {}", getIdentifier(), path, mutableTree, e);
            }
        }

        protected final boolean isReady() {
            return ready;
        }

        protected final void checkNotReady() {
            checkState(!ready, "Transaction %s is ready. No further modifications allowed.", getIdentifier());
        }

        @Override
        public synchronized DOMStoreThreePhaseCommitCohort ready() {
            checkState(!ready, "Transaction %s is already ready.", getIdentifier());
            ready = true;

            LOG.debug("Store transaction: {} : Ready", getIdentifier());
            mutableTree.seal();
            return store.submit(this);
        }

        protected DataTreeModification getMutatedView() {
            return mutableTree;
        }

        @Override
        protected ToStringHelper addToStringAttributes(final ToStringHelper toStringHelper) {
            return toStringHelper.add("ready", isReady());
        }
    }

    private static class SnapshotBackedReadWriteTransaction extends SnapshotBackedWriteTransaction implements
    DOMStoreReadWriteTransaction {

        protected SnapshotBackedReadWriteTransaction(final Object identifier, final DataTreeSnapshot snapshot,
                final InMemoryDOMDataStore store) {
            super(identifier, snapshot, store);
        }

        @Override
        public ListenableFuture<Optional<NormalizedNode<?, ?>>> read(final InstanceIdentifier path) {
            LOG.trace("Tx: {} Read: {}", getIdentifier(), path);
            try {
                return Futures.immediateFuture(getMutatedView().readNode(path));
            } catch (Exception e) {
                LOG.error("Tx: {} Failed Read of {}", getIdentifier(), path, e);
                throw e;
            }
        }
    }

    private class ThreePhaseCommitImpl implements DOMStoreThreePhaseCommitCohort {

        private final SnapshotBackedWriteTransaction transaction;
        private final DataTreeModification modification;

        private ResolveDataChangeEventsTask listenerResolver;
        private DataTreeCandidate candidate;

        public ThreePhaseCommitImpl(final SnapshotBackedWriteTransaction writeTransaction) {
            this.transaction = writeTransaction;
            this.modification = transaction.getMutatedView();
        }

        @Override
        public ListenableFuture<Boolean> canCommit() {
            return executor.submit(new Callable<Boolean>() {
                @Override
                public Boolean call() {
                    try {
                        dataTree.validate(modification);
                        LOG.debug("Store Transaction: {} can be committed", transaction.getIdentifier());
                        return true;
                    } catch (DataPreconditionFailedException e) {
                        LOG.warn("Store Tx: {} Data Precondition failed for {}.",transaction.getIdentifier(),e.getPath(),e);
                        return false;
                    }
                }
            });
        }

        @Override
        public ListenableFuture<Void> preCommit() {
            return executor.submit(new Callable<Void>() {
                @Override
                public Void call() {
                    candidate = dataTree.prepare(modification);
                    listenerResolver = ResolveDataChangeEventsTask.create(candidate, listenerTree);
                    return null;
                }
            });
        }

        @Override
        public ListenableFuture<Void> abort() {
            candidate = null;
            return Futures.immediateFuture(null);
        }

        @Override
        public ListenableFuture<Void> commit() {
            checkState(candidate != null, "Proposed subtree must be computed");

            /*
             * The commit has to occur atomically with regard to listener
             * registrations.
             */
            synchronized (this) {
                dataTree.commit(candidate);

                for (ChangeListenerNotifyTask task : listenerResolver.call()) {
                    LOG.trace("Scheduling invocation of listeners: {}", task);
                    executor.submit(task);
                }
            }

            return Futures.immediateFuture(null);
        }
    }
}
