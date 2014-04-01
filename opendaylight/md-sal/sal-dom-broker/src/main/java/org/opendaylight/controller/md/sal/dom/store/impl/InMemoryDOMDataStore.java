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

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicLong;

import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeListener;
import org.opendaylight.controller.md.sal.dom.store.impl.tree.NodeModification;
import org.opendaylight.controller.md.sal.dom.store.impl.tree.StoreMetadataNode;
import org.opendaylight.controller.sal.core.spi.data.DOMStore;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadWriteTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreThreePhaseCommitCohort;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreWriteTransaction;
import org.opendaylight.yangtools.concepts.Identifiable;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.schema.NormalizedNodeUtils;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaContextListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.primitives.UnsignedLong;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;

public class InMemoryDOMDataStore implements DOMStore, Identifiable<String>, SchemaContextListener {

    private static final Logger LOG = LoggerFactory.getLogger(InMemoryDOMDataStore.class);

    private final AtomicLong txCounter = new AtomicLong(0);

    private DataAndMetadataSnapshot snapshot;
    private ModificationApplyOperation operation;

    private final ListeningExecutorService executor;
    private final String name;

    private SchemaContext schemaContext;

    public InMemoryDOMDataStore(final String name, final ListeningExecutorService executor) {
        this.executor = executor;
        this.name = name;
        this.operation = new AllwaysFailOperation();
        this.snapshot = DataAndMetadataSnapshot.createEmpty();
    }

    @Override
    public String getIdentifier() {
        return name;
    }

    @Override
    public DOMStoreReadTransaction newReadOnlyTransaction() {
        return new SnapshotBackedReadTransaction(nextIdentifier(), snapshot);
    }

    @Override
    public DOMStoreReadWriteTransaction newReadWriteTransaction() {
        return new SnapshotBackedReadWriteTransaction(nextIdentifier(), snapshot, this, operation);
    }

    @Override
    public DOMStoreWriteTransaction newWriteOnlyTransaction() {
        return new SnaphostBackedWriteTransaction(nextIdentifier(), snapshot, this, operation);
    }

    @Override
    public synchronized void onGlobalContextUpdated(final SchemaContext ctx) {
        operation = SchemaAwareApplyOperationRoot.from(ctx);
        schemaContext = ctx;
    }

    @Override
    public <L extends AsyncDataChangeListener<InstanceIdentifier, NormalizedNode<?, ?>>> ListenerRegistration<L> registerChangeListener(
            final InstanceIdentifier path, final L listener, final DataChangeScope scope) {
        return null;
    }

    private synchronized DOMStoreThreePhaseCommitCohort submit(
            final SnaphostBackedWriteTransaction snaphostBackedWriteTransaction) {
        return new ThreePhaseCommitImpl(snaphostBackedWriteTransaction);
    }

    private Object nextIdentifier() {
        return name + "-" + txCounter.getAndIncrement();
    }

    private static class SnapshotBackedReadTransaction implements DOMStoreReadTransaction {

        private DataAndMetadataSnapshot stableSnapshot;
        private final Object identifier;

        public SnapshotBackedReadTransaction(final Object identifier, final DataAndMetadataSnapshot snapshot) {
            this.identifier = identifier;
            this.stableSnapshot = snapshot;
        }

        @Override
        public Object getIdentifier() {
            return identifier;
        }

        @Override
        public void close() {
            stableSnapshot = null;
        }

        @Override
        public ListenableFuture<Optional<NormalizedNode<?, ?>>> read(final InstanceIdentifier path) {
            checkNotNull(path, "Path must not be null.");
            checkState(stableSnapshot != null, "Transaction is closed");
            return Futures.immediateFuture(NormalizedNodeUtils.findNode(stableSnapshot.getDataTree(), path));
        }

        @Override
        public String toString() {
            return "SnapshotBackedReadTransaction [id =" + identifier + "]";
        }

    }

    private static class SnaphostBackedWriteTransaction implements DOMStoreWriteTransaction {

        private MutableDataTree mutableTree;
        private final Object identifier;
        private InMemoryDOMDataStore store;

        private boolean ready = false;

        public SnaphostBackedWriteTransaction(final Object identifier, final DataAndMetadataSnapshot snapshot,
                final InMemoryDOMDataStore store, final ModificationApplyOperation applyOper) {
            this.identifier = identifier;
            mutableTree = MutableDataTree.from(snapshot, applyOper);
            this.store = store;
        }

        @Override
        public Object getIdentifier() {
            return identifier;
        }

        @Override
        public void close() {
            this.mutableTree = null;
            this.store = null;
        }

        @Override
        public void write(final InstanceIdentifier path, final NormalizedNode<?, ?> data) {
            checkNotReady();
            mutableTree.write(path, data);
        }

        @Override
        public void delete(final InstanceIdentifier path) {
            checkNotReady();
            mutableTree.delete(path);
        }

        protected boolean isReady() {
            return ready;
        }

        protected void checkNotReady() {
            checkState(!ready, "Transaction is ready. No further modifications allowed.");
        }

        @Override
        public synchronized DOMStoreThreePhaseCommitCohort ready() {
            ready = true;
            LOG.debug("Store transaction: {} : Ready",getIdentifier());
            mutableTree.seal();
            return store.submit(this);
        }

        protected MutableDataTree getMutatedView() {
            return mutableTree;
        }

        @Override
        public String toString() {
            return "SnaphostBackedWriteTransaction [id=" + getIdentifier() + ", ready=" + isReady() + "]";
        }

    }

    private static class SnapshotBackedReadWriteTransaction extends SnaphostBackedWriteTransaction implements
            DOMStoreReadWriteTransaction {

        protected SnapshotBackedReadWriteTransaction(final Object identifier, final DataAndMetadataSnapshot snapshot,
                final InMemoryDOMDataStore store, final ModificationApplyOperation applyOper) {
            super(identifier, snapshot, store, applyOper);
        }

        @Override
        public ListenableFuture<Optional<NormalizedNode<?, ?>>> read(final InstanceIdentifier path) {
            return Futures.immediateFuture(getMutatedView().read(path));
        }

        @Override
        public String toString() {
            return "SnapshotBackedReadWriteTransaction [id=" + getIdentifier() + ", ready=" + isReady() + "]";
        }

    }

    private class ThreePhaseCommitImpl implements DOMStoreThreePhaseCommitCohort {

        private final SnaphostBackedWriteTransaction transaction;
        private final NodeModification modification;

        private DataAndMetadataSnapshot storeSnapshot;
        private Optional<StoreMetadataNode> proposedSubtree;

        public ThreePhaseCommitImpl(final SnaphostBackedWriteTransaction writeTransaction) {
            this.transaction = writeTransaction;
            this.modification = transaction.getMutatedView().getRootModification();
        }

        @Override
        public ListenableFuture<Boolean> canCommit() {
            final DataAndMetadataSnapshot snapshotCapture = snapshot;
            final ModificationApplyOperation snapshotOperation = operation;

            return executor.submit(new Callable<Boolean>() {

                @Override
                public Boolean call() throws Exception {
                    boolean applicable = snapshotOperation.isApplicable(modification, Optional.of(snapshotCapture.getMetadataTree()));
                    LOG.debug("Store Transcation: {} : canCommit : {}",transaction.getIdentifier(),applicable);
                    return applicable;
                }
            });
        }

        @Override
        public ListenableFuture<Void> preCommit() {
            storeSnapshot = snapshot;
            return executor.submit(new Callable<Void>() {

                @Override
                public Void call() throws Exception {
                    StoreMetadataNode metadataTree = storeSnapshot.getMetadataTree();
                    proposedSubtree = operation.apply(modification, Optional.of(metadataTree),increase(metadataTree.getSubtreeVersion()));
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
            checkState(proposedSubtree != null);
            checkState(storeSnapshot != null);
            // return ImmediateFuture<>;
            InMemoryDOMDataStore.this.commit(storeSnapshot, proposedSubtree);
            return Futures.<Void> immediateFuture(null);
        }

    }

    private synchronized void commit(final DataAndMetadataSnapshot storeSnapshot,
            final Optional<StoreMetadataNode> proposedSubtree) {
        //LOG.info("Updating Store snaphot.");
        checkState(snapshot == storeSnapshot, "Store snapshot and transaction snapshot differs");
        snapshot = DataAndMetadataSnapshot.builder().setMetadataTree(proposedSubtree.get())
                .setSchemaContext(schemaContext).build();
    }

    private class AllwaysFailOperation implements ModificationApplyOperation {

        @Override
        public Optional<StoreMetadataNode> apply(final NodeModification modification,
                final Optional<StoreMetadataNode> storeMeta,final UnsignedLong subtreeVersion) {
            throw new IllegalStateException("Schema Context is not available.");
        }

        @Override
        public boolean isApplicable(final NodeModification modification, final Optional<StoreMetadataNode> storeMetadata) {
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
