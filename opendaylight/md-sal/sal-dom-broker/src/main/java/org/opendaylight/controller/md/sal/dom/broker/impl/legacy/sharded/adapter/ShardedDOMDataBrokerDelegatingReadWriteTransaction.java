/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.md.sal.dom.broker.impl.legacy.sharded.adapter;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.SettableFuture;
import java.util.Map;
import java.util.Queue;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadOnlyTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadWriteTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTree;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeConfiguration;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeModification;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeSnapshot;
import org.opendaylight.yangtools.yang.data.impl.schema.tree.InMemoryDataTreeFactory;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

/**
 * Read/write transaction that delegates write and initial read to
 * {@link org.opendaylight.mdsal.dom.broker.ShardedDOMWriteTransactionAdapter}
 * and {@link org.opendaylight.mdsal.dom.broker.ShardedDOMReadTransactionAdapter}
 * respectively. These two in turn rely on shard aware implementation of
 * {@link org.opendaylight.mdsal.dom.api.DOMDataTreeService}.
 *
 * <p>
 * Since reading data distributed on different subshards is not guaranteed to
 * return all relevant data, best effort is to try to operate only on single
 * subtree in conceptual data tree. We define this subtree by first write
 * operation performed on transaction. All next read and write operations
 * should be performed just in this initial subtree.
 */
// FIXME explicitly enforce just one subtree requirement
@NotThreadSafe
class ShardedDOMDataBrokerDelegatingReadWriteTransaction implements DOMDataReadWriteTransaction {
    private final DOMDataReadOnlyTransaction readTxDelegate;
    private final DOMDataWriteTransaction writeTxDelegate;
    private final Object txIdentifier;
    private final ImmutableMap<LogicalDatastoreType, Queue<Modification>> modificationHistoryMap;
    private final ImmutableMap<LogicalDatastoreType, DataTreeSnapshot> snapshotMap;
    private final Map<LogicalDatastoreType, ListenableFuture<Optional<NormalizedNode<?, ?>>>> initialReadMap;
    private YangInstanceIdentifier root = null;

    ShardedDOMDataBrokerDelegatingReadWriteTransaction(final Object readWriteTxId, final SchemaContext ctx,
                                                              final DOMDataReadOnlyTransaction readTxDelegate,
                                                              final DOMDataWriteTransaction writeTxDelegate) {
        this.readTxDelegate = checkNotNull(readTxDelegate);
        this.writeTxDelegate = checkNotNull(writeTxDelegate);
        this.txIdentifier = checkNotNull(readWriteTxId);
        this.initialReadMap = Maps.newEnumMap(LogicalDatastoreType.class);

        final InMemoryDataTreeFactory treeFactory = new InMemoryDataTreeFactory();
        final ImmutableMap.Builder<LogicalDatastoreType, DataTreeSnapshot> snapshotMapBuilder = ImmutableMap.builder();
        final ImmutableMap.Builder<LogicalDatastoreType, Queue<Modification>> modificationHistoryMapBuilder
                = ImmutableMap.builder();
        for (final LogicalDatastoreType store : LogicalDatastoreType.values()) {
            final DataTree tree = treeFactory.create(treeConfigForStore(store));
            tree.setSchemaContext(ctx);
            snapshotMapBuilder.put(store, tree.takeSnapshot());

            modificationHistoryMapBuilder.put(store, Lists.newLinkedList());
        }

        modificationHistoryMap = modificationHistoryMapBuilder.build();
        snapshotMap = snapshotMapBuilder.build();
    }

    @Override
    public boolean cancel() {
        readTxDelegate.close();
        return writeTxDelegate.cancel();
    }

    @Override
    public void delete(final LogicalDatastoreType store, final YangInstanceIdentifier path) {
        if (root == null) {
            initialRead(path);
        }

        modificationHistoryMap.get(store).add(new Modification(Modification.Operation.DELETE, path, null));
        writeTxDelegate.delete(store, path);
    }

    @Override
    public FluentFuture<? extends CommitInfo> commit() {
        return writeTxDelegate.commit();
    }

    @Override
    public CheckedFuture<Optional<NormalizedNode<?, ?>>, ReadFailedException> read(final LogicalDatastoreType store,
                                                                                   final YangInstanceIdentifier path) {
        checkState(root != null,
                   "A modify operation (put, merge or delete) must be performed prior to a read operation");
        final SettableFuture<Optional<NormalizedNode<?, ?>>> readResult = SettableFuture.create();
        final Queue<Modification> currentHistory = Lists.newLinkedList(modificationHistoryMap.get(store));
        Futures.addCallback(initialReadMap.get(store), new FutureCallback<Optional<NormalizedNode<?, ?>>>() {
            @Override
            public void onSuccess(@Nonnull final Optional<NormalizedNode<?, ?>> result) {
                final DataTreeModification mod = snapshotMap.get(store).newModification();
                if (result.isPresent()) {
                    mod.write(path, result.get());
                }
                applyModificationHistoryToSnapshot(mod, currentHistory);
                readResult.set(Optional.fromJavaUtil(mod.readNode(path)));
            }

            @Override
            public void onFailure(final Throwable throwable) {
                readResult.setException(throwable);
            }
        }, MoreExecutors.directExecutor());

        return Futures.makeChecked(readResult, ReadFailedException.MAPPER);
    }

    @Override
    public CheckedFuture<Boolean, ReadFailedException> exists(final LogicalDatastoreType store,
                                                              final YangInstanceIdentifier path) {
        checkState(root != null,
                   "A modify operation (put, merge or delete) must be performed prior to an exists operation");
        return Futures.makeChecked(Futures.transform(read(store, path),
                                                     (Function<Optional<NormalizedNode<?, ?>>, Boolean>)
                                                             Optional::isPresent),
                                   ReadFailedException.MAPPER);
    }

    @Override
    public void put(final LogicalDatastoreType store, final YangInstanceIdentifier path,
                    final NormalizedNode<?, ?> data) {
        if (root == null) {
            initialRead(path);
        }

        modificationHistoryMap.get(store).add(new Modification(Modification.Operation.WRITE, path, data));
        writeTxDelegate.put(store, path, data);
    }

    @Override
    public void merge(final LogicalDatastoreType store, final YangInstanceIdentifier path,
                      final NormalizedNode<?, ?> data) {
        if (root == null) {
            initialRead(path);
        }

        modificationHistoryMap.get(store).add(new Modification(Modification.Operation.MERGE, path, data));
        writeTxDelegate.merge(store, path, data);
    }

    @Override
    public Object getIdentifier() {
        return txIdentifier;
    }

    private void initialRead(final YangInstanceIdentifier path) {
        root = path;

        for (final LogicalDatastoreType store : LogicalDatastoreType.values()) {
            initialReadMap.put(store, readTxDelegate.read(store, path));
        }
    }

    private static DataTreeConfiguration treeConfigForStore(final LogicalDatastoreType store) {
        return store == LogicalDatastoreType.CONFIGURATION ? DataTreeConfiguration.DEFAULT_CONFIGURATION
                : DataTreeConfiguration.DEFAULT_OPERATIONAL;
    }

    private static void applyModificationHistoryToSnapshot(final DataTreeModification dataTreeModification,
                                                    final Queue<Modification> modificationHistory) {
        while (!modificationHistory.isEmpty()) {
            final Modification modification = modificationHistory.poll();
            switch (modification.getOperation()) {
                case WRITE:
                    dataTreeModification.write(modification.getPath(), modification.getData());
                    break;
                case MERGE:
                    dataTreeModification.merge(modification.getPath(), modification.getData());
                    break;
                case DELETE:
                    dataTreeModification.delete(modification.getPath());
                    break;
                default:
                    // NOOP
            }
        }
    }

    static class Modification {

        enum Operation {
            WRITE, MERGE, DELETE
        }

        private final NormalizedNode<?, ?> data;
        private final YangInstanceIdentifier path;
        private final Operation operation;

        Modification(final Operation operation, final YangInstanceIdentifier path, final NormalizedNode<?, ?> data) {
            this.data = data;
            this.path = checkNotNull(path);
            this.operation = checkNotNull(operation);
        }

        Operation getOperation() {
            return operation;
        }

        YangInstanceIdentifier getPath() {
            return path;
        }

        NormalizedNode<?, ?> getData() {
            return data;
        }
    }
}
