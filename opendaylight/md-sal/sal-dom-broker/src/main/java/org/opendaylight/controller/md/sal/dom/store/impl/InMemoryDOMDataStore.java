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
import static org.opendaylight.controller.md.sal.dom.store.impl.StoreUtils.increase;

import java.util.Collections;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicLong;

import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeListener;
import org.opendaylight.controller.md.sal.dom.store.impl.tree.ListenerTree;
import org.opendaylight.controller.md.sal.dom.store.impl.tree.ModificationType;
import org.opendaylight.controller.md.sal.dom.store.impl.tree.NodeModification;
import org.opendaylight.controller.md.sal.dom.store.impl.tree.StoreMetadataNode;
import org.opendaylight.controller.sal.core.spi.data.DOMStore;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadWriteTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreThreePhaseCommitCohort;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreWriteTransaction;
import org.opendaylight.yangtools.concepts.AbstractListenerRegistration;
import org.opendaylight.yangtools.concepts.Identifiable;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaContextListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Objects;
import com.google.common.base.Objects.ToStringHelper;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.primitives.UnsignedLong;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;

public class InMemoryDOMDataStore implements DOMStore, Identifiable<String>, SchemaContextListener {

    private static final Logger LOG = LoggerFactory.getLogger(InMemoryDOMDataStore.class);
    private static final InstanceIdentifier PUBLIC_ROOT_PATH = InstanceIdentifier.builder().build();

    private final ListeningExecutorService executor;
    private final String name;
    private final AtomicLong txCounter = new AtomicLong(0);
    private final ListenerTree listenerTree = ListenerTree.create();
    private final DataTree dataTree = DataTree.create(null);
    private ModificationApplyOperation operationTree = new AlwaysFailOperation();

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
        return new SnapshotBackedReadWriteTransaction(nextIdentifier(), dataTree.takeSnapshot(), this, operationTree);
    }

    @Override
    public DOMStoreWriteTransaction newWriteOnlyTransaction() {
        return new SnapshotBackedWriteTransaction(nextIdentifier(), dataTree.takeSnapshot(), this, operationTree);
    }

    @Override
    public synchronized void onGlobalContextUpdated(final SchemaContext ctx) {
        /*
         * Order of operations is important: dataTree may reject the context
         * and creation of ModificationApplyOperation may fail. So pre-construct
         * the operation, then update the data tree and then move the operation
         * into view.
         */
        final ModificationApplyOperation newOperationTree = SchemaAwareApplyOperationRoot.from(ctx);
        dataTree.setSchemaContext(ctx);
        operationTree = newOperationTree;
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

    private void commit(final DataTree.Snapshot currentSnapshot, final StoreMetadataNode newDataTree,
            final ResolveDataChangeEventsTask listenerResolver) {
        LOG.debug("Updating Store snaphot version: {} with version:{}", currentSnapshot, newDataTree.getSubtreeVersion());

        if (LOG.isTraceEnabled()) {
            LOG.trace("Data Tree is {}", StoreUtils.toStringTree(newDataTree.getData()));
        }

        /*
         * The commit has to occur atomically with regard to listener
         * registrations.
         */
        synchronized (this) {
            dataTree.commitSnapshot(currentSnapshot, newDataTree);

            for (ChangeListenerNotifyTask task : listenerResolver.call()) {
                LOG.trace("Scheduling invocation of listeners: {}", task);
                executor.submit(task);
            }
        }
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
        private DataTree.Snapshot stableSnapshot;

        public SnapshotBackedReadTransaction(final Object identifier, final DataTree.Snapshot snapshot) {
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

        public SnapshotBackedWriteTransaction(final Object identifier, final DataTree.Snapshot snapshot,
                final InMemoryDOMDataStore store, final ModificationApplyOperation applyOper) {
            super(identifier);
            mutableTree = DataTreeModification.from(snapshot, applyOper);
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

        protected SnapshotBackedReadWriteTransaction(final Object identifier, final DataTree.Snapshot snapshot,
                final InMemoryDOMDataStore store, final ModificationApplyOperation applyOper) {
            super(identifier, snapshot, store, applyOper);
        }

        @Override
        public ListenableFuture<Optional<NormalizedNode<?, ?>>> read(final InstanceIdentifier path) {
            LOG.trace("Tx: {} Read: {}", getIdentifier(), path);
            try {
                return Futures.immediateFuture(getMutatedView().read(path));
            } catch (Exception e) {
                LOG.error("Tx: {} Failed Read of {}", getIdentifier(), path, e);
                throw e;
            }
        }
    }

    private class ThreePhaseCommitImpl implements DOMStoreThreePhaseCommitCohort {

        private final SnapshotBackedWriteTransaction transaction;
        private final NodeModification modification;

        private DataTree.Snapshot storeSnapshot;
        private Optional<StoreMetadataNode> proposedSubtree;
        private ResolveDataChangeEventsTask listenerResolver;

        public ThreePhaseCommitImpl(final SnapshotBackedWriteTransaction writeTransaction) {
            this.transaction = writeTransaction;
            this.modification = transaction.getMutatedView().getRootModification();
        }

        @Override
        public ListenableFuture<Boolean> canCommit() {
            final DataTree.Snapshot snapshotCapture = dataTree.takeSnapshot();
            final ModificationApplyOperation snapshotOperation = operationTree;

            return executor.submit(new Callable<Boolean>() {

                @Override
                public Boolean call() throws Exception {
                    Boolean applicable = false;
                    try {
                        snapshotOperation.checkApplicable(PUBLIC_ROOT_PATH, modification,
                            Optional.of(snapshotCapture.getRootNode()));
                        applicable = true;
                    } catch (DataPreconditionFailedException e) {
                        LOG.warn("Store Tx: {} Data Precondition failed for {}.",transaction.getIdentifier(),e.getPath(),e);
                        applicable = false;
                    }
                    LOG.debug("Store Transaction: {} : canCommit : {}", transaction.getIdentifier(), applicable);
                    return applicable;
                }
            });
        }

        @Override
        public ListenableFuture<Void> preCommit() {
            storeSnapshot = dataTree.takeSnapshot();
            if (modification.getModificationType() == ModificationType.UNMODIFIED) {
                return Futures.immediateFuture(null);
            }
            return executor.submit(new Callable<Void>() {

                @Override
                public Void call() throws Exception {
                    StoreMetadataNode metadataTree = storeSnapshot.getRootNode();

                    proposedSubtree = operationTree.apply(modification, Optional.of(metadataTree),
                            increase(metadataTree.getSubtreeVersion()));

                    listenerResolver = ResolveDataChangeEventsTask.create() //
                            .setRootPath(PUBLIC_ROOT_PATH) //
                            .setBeforeRoot(Optional.of(metadataTree)) //
                            .setAfterRoot(proposedSubtree) //
                            .setModificationRoot(modification) //
                            .setListenerRoot(listenerTree);

                    return null;
                }
            });
        }

        @Override
        public ListenableFuture<Void> abort() {
            storeSnapshot = null;
            proposedSubtree = null;
            return Futures.<Void> immediateFuture(null);
        }

        @Override
        public ListenableFuture<Void> commit() {
            if (modification.getModificationType() == ModificationType.UNMODIFIED) {
                return Futures.immediateFuture(null);
            }

            checkState(proposedSubtree != null, "Proposed subtree must be computed");
            checkState(storeSnapshot != null, "Proposed subtree must be computed");
            // return ImmediateFuture<>;
            InMemoryDOMDataStore.this.commit(storeSnapshot, proposedSubtree.get(), listenerResolver);
            return Futures.<Void> immediateFuture(null);
        }

    }

    private static final class AlwaysFailOperation implements ModificationApplyOperation {

        @Override
        public Optional<StoreMetadataNode> apply(final NodeModification modification,
                final Optional<StoreMetadataNode> storeMeta, final UnsignedLong subtreeVersion) {
            throw new IllegalStateException("Schema Context is not available.");
        }

        @Override
        public void checkApplicable(final InstanceIdentifier path,final NodeModification modification, final Optional<StoreMetadataNode> storeMetadata) {
            throw new IllegalStateException("Schema Context is not available.");
        }

        @Override
        public Optional<ModificationApplyOperation> getChild(final PathArgument child) {
            throw new IllegalStateException("Schema Context is not available.");
        }

        @Override
        public void verifyStructure(final NodeModification modification) throws IllegalArgumentException {
            throw new IllegalStateException("Schema Context is not available.");
        }

    }
}
