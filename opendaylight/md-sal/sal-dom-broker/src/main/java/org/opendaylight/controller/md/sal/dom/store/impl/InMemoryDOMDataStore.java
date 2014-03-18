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

import java.util.concurrent.atomic.AtomicLong;

import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeListener;
import org.opendaylight.controller.sal.core.spi.data.DOMStore;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadWriteTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreThreePhaseCommitCohort;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreWriteTransaction;
import org.opendaylight.yangtools.concepts.Identifiable;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.schema.NormalizedNodeUtils;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;

public class InMemoryDOMDataStore implements DOMStore, Identifiable<String> {

    private final AtomicLong txCounter = new AtomicLong(0);

    private DataAndMetadataSnapshot snapshot;
    private final ListeningExecutorService executor;
    private final String name;

    protected InMemoryDOMDataStore(final String name, final ListeningExecutorService executor) {
        this.executor = executor;
        this.name = name;
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
        return new SnapshotBackedReadWriteTransaction(nextIdentifier(), snapshot, this);
    }

    @Override
    public DOMStoreWriteTransaction newWriteOnlyTransaction() {
        return new SnaphostBackedWriteTransaction(nextIdentifier(), snapshot, this);
    }

    @Override
    public <L extends AsyncDataChangeListener<InstanceIdentifier, NormalizedNode<?, ?>>> ListenerRegistration<L> registerChangeListener(
            final InstanceIdentifier path, final L listener, final DataChangeScope scope) {
        return null;
    }

    private DOMStoreThreePhaseCommitCohort submit(final SnaphostBackedWriteTransaction snaphostBackedWriteTransaction) {
        return null;
    }

    private Object nextIdentifier() {
        return name + "-" + txCounter.getAndIncrement();
    }

    public class SnaphostBackedWriteTransaction implements DOMStoreWriteTransaction {

        private MutableDataTree mutableTree;
        private final Object identifier;
        private InMemoryDOMDataStore store;

        private boolean sealed = false;

        public SnaphostBackedWriteTransaction(final Object identifier, final DataAndMetadataSnapshot snapshot,
                final InMemoryDOMDataStore store) {
            this.identifier = identifier;
            mutableTree = MutableDataTree.from(snapshot, new SchemaAwareApplyOperationRoot(snapshot.getSchemaContext()
                    .get()));
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
            checkSealed();
            mutableTree.write(path, data);
        }

        @Override
        public void delete(final InstanceIdentifier path) {
            checkSealed();
            mutableTree.delete(path);
        }

        protected void checkSealed() {
            checkState(!sealed, "Transaction is sealed. No further modifications allowed.");
        }

        @Override
        public synchronized DOMStoreThreePhaseCommitCohort ready() {
            sealed = true;
            return store.submit(this);
        }

        protected MutableDataTree getMutatedView() {
            return mutableTree;
        }

    }

    public static class SnapshotBackedReadTransaction implements DOMStoreReadTransaction {

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
    }

    public class SnapshotBackedReadWriteTransaction extends SnaphostBackedWriteTransaction implements
            DOMStoreReadWriteTransaction {

        public SnapshotBackedReadWriteTransaction(final Object identifier, final DataAndMetadataSnapshot snapshot,
                final InMemoryDOMDataStore store) {
            super(identifier, snapshot, store);
        }

        @Override
        public ListenableFuture<Optional<NormalizedNode<?, ?>>> read(final InstanceIdentifier path) {
            return Futures.immediateFuture(getMutatedView().read(path));
        }
    }
}
