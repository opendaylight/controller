/*
 * Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.opendaylight.controller.cluster.datastore.modification.MutableCompositeModification;
import org.opendaylight.controller.cluster.datastore.node.NormalizedNodeToNodeCodec;
import org.opendaylight.controller.protobuff.messages.common.NormalizedNodeMessages;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreWriteTransaction;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

/**
 * Coordinates persistence recovery of journal log entries and snapshots for a shard. Each snapshot
 * and journal log entry batch are de-serialized and applied to their own write transaction
 * instance in parallel on a thread pool for faster recovery time. However the transactions are
 * committed to the data store in the order the corresponding snapshot or log batch are received
 * to preserve data store integrity.
 *
 * @author Thomas Panetelis
 */
class ShardRecoveryCoordinator {

    private static final int TIME_OUT = 10;

    private static final Logger LOG = LoggerFactory.getLogger(ShardRecoveryCoordinator.class);

    private final List<DOMStoreWriteTransaction> resultingTxList = Lists.newArrayList();
    private final SchemaContext schemaContext;
    private final String shardName;
    private final ExecutorService executor;

    ShardRecoveryCoordinator(String shardName, SchemaContext schemaContext) {
        this.schemaContext = schemaContext;
        this.shardName = shardName;

        executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors(),
                new ThreadFactoryBuilder().setDaemon(true)
                        .setNameFormat("ShardRecovery-" + shardName + "-%d").build());
    }

    /**
     * Submits a batch of journal log entries.
     *
     * @param logEntries the serialized journal log entries
     * @param resultingTx the write Tx to which to apply the entries
     */
    void submit(List<Object> logEntries, DOMStoreWriteTransaction resultingTx) {
        LogRecoveryTask task = new LogRecoveryTask(logEntries, resultingTx);
        resultingTxList.add(resultingTx);
        executor.execute(task);
    }

    /**
     * Submits a snapshot.
     *
     * @param snapshot the serialized snapshot
     * @param resultingTx the write Tx to which to apply the entries
     */
    void submit(ByteString snapshot, DOMStoreWriteTransaction resultingTx) {
        SnapshotRecoveryTask task = new SnapshotRecoveryTask(snapshot, resultingTx);
        resultingTxList.add(resultingTx);
        executor.execute(task);
    }

    Collection<DOMStoreWriteTransaction> getTransactions() {
        // Shutdown the executor and wait for task completion.
        executor.shutdown();

        try {
            if(executor.awaitTermination(TIME_OUT, TimeUnit.MINUTES))  {
                return resultingTxList;
            } else {
                LOG.error("Recovery for shard {} timed out after {} minutes", shardName, TIME_OUT);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        return Collections.emptyList();
    }

    private static abstract class ShardRecoveryTask implements Runnable {

        final DOMStoreWriteTransaction resultingTx;

        ShardRecoveryTask(DOMStoreWriteTransaction resultingTx) {
            this.resultingTx = resultingTx;
        }
    }

    private class LogRecoveryTask extends ShardRecoveryTask {

        private final List<Object> logEntries;

        LogRecoveryTask(List<Object> logEntries, DOMStoreWriteTransaction resultingTx) {
            super(resultingTx);
            this.logEntries = logEntries;
        }

        @Override
        public void run() {
            for(int i = 0; i < logEntries.size(); i++) {
                MutableCompositeModification.fromSerializable(
                        logEntries.get(i)).apply(resultingTx);
                // Null out to GC quicker.
                logEntries.set(i, null);
            }
        }
    }

    private class SnapshotRecoveryTask extends ShardRecoveryTask {

        private final ByteString snapshot;

        SnapshotRecoveryTask(ByteString snapshot, DOMStoreWriteTransaction resultingTx) {
            super(resultingTx);
            this.snapshot = snapshot;
        }

        @Override
        public void run() {
            try {
                NormalizedNodeMessages.Node serializedNode = NormalizedNodeMessages.Node.parseFrom(snapshot);
                NormalizedNode<?, ?> node = new NormalizedNodeToNodeCodec(schemaContext).decode(
                        serializedNode);

                // delete everything first
                resultingTx.delete(YangInstanceIdentifier.builder().build());

                // Add everything from the remote node back
                resultingTx.write(YangInstanceIdentifier.builder().build(), node);
            } catch (InvalidProtocolBufferException e) {
                LOG.error("Error deserializing snapshot", e);
            }
        }
    }
}
