package org.opendaylight.controller.cluster.example;

import org.opendaylight.controller.cluster.example.messages.KeyValue;
import org.opendaylight.controller.cluster.raft.RaftActorRecoveryCohort;
import org.opendaylight.controller.cluster.raft.protobuff.client.messages.Payload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Created by django on 6/28/16.
 */
public class KeyValueStoreRecoveryCohort implements RaftActorRecoveryCohort {
    Logger LOG = LoggerFactory.getLogger(KeyValueStoreRecoveryCohort.class);

    private final String id;
    private final Map<String, String> kvStore;
    private List<KeyValue> batch;
    private int batchIndex = 0;

    public KeyValueStoreRecoveryCohort(String id, Map<String, String> kvStore) {
        this.id = id;
        this.kvStore = kvStore;
    }

    @Override
    public void startLogRecoveryBatch(int maxBatchSize) {
        LOG.info("id: {}, Max Batch size: {}", id, maxBatchSize);
        batch = new ArrayList<>(maxBatchSize);
    }

    @Override
    public void appendRecoveredLogEntry(Payload data) {
        if (data instanceof KeyValue) {
            LOG.info("id: {}, appendRecoveredLogEntry: {}", id, data.toString());
            batch.add(batchIndex++, (KeyValue) data);
        }
    }

    @Override
    public void applyRecoverySnapshot(byte[] snapshotBytes) {
        LOG.info("id: {}, applyRecoverySnapshot: snapshotBytes size: {}", id, snapshotBytes.length);
    }

    @Override
    public void applyCurrentLogRecoveryBatch() {
        LOG.info("id: {}, applyCurrentRecoveryBatch", id);
        for (int i = 0; i < batchIndex; i++) {
            KeyValue kv = batch.get(i);
            kvStore.put(kv.getKey(), kv.getValue());
        }
        batch = null;
        batchIndex = 0;
    }

    @Nullable
    @Override
    public byte[] getRestoreFromSnapshot() {
        LOG.info("id: {}, getRestoreFromSnapshot", id);
        return new byte[]{'0'};
    }
}
