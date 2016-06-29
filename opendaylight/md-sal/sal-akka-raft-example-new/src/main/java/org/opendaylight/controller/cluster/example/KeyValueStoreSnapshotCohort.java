package org.opendaylight.controller.cluster.example;

import akka.actor.ActorRef;
import com.google.protobuf.ByteString;
import org.opendaylight.controller.cluster.raft.RaftActorSnapshotCohort;
import org.opendaylight.controller.cluster.raft.base.messages.CaptureSnapshotReply;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

class KeyValueStoreSnapshotCohort implements RaftActorSnapshotCohort {

    Logger LOG = LoggerFactory.getLogger(KeyValueStoreSnapshotCohort.class);
    private final String id;
    private final Map<String, String> kvStore;

    KeyValueStoreSnapshotCohort(String id, Map<String, String> kvStore) {
        this.id = id;
        this.kvStore = kvStore;
    }

    @Override
    public void createSnapshot(ActorRef actorRef) {
        LOG.info("id: {} createSnapshot: actorRef: {}", id, actorRef.toString());
        ByteString bs = null;
        try {
            bs = fromObject(kvStore);
        } catch (Exception e) {
            System.err.println("Exception in creating snapshot: " + e.getStackTrace());
        }
        if (bs != null) {
            actorRef.tell(new CaptureSnapshotReply(bs.toByteArray()), null);
        } else {
            actorRef.tell(new CaptureSnapshotReply(new byte[]{(byte)0}), null);
        }
    }

    @Override
    public void applySnapshot(byte[] snapshotBytes) {
        LOG.info("id: {} applySnapshot: snapshotBytes size: {}", id, snapshotBytes.length);
        kvStore.clear();
        try {
            kvStore.putAll((HashMap<String, String>) toObject(snapshotBytes));
        } catch (Exception e) {
            System.out.println("Exception in applying snapshot: " + e.getStackTrace());
        }
        System.out.println("Snapshot applied to state : {}" + kvStore.size());
    }

    private static ByteString fromObject(Object snapshot) throws Exception {
        ByteArrayOutputStream b = null;
        ObjectOutputStream o = null;
        try {
            b = new ByteArrayOutputStream();
            o = new ObjectOutputStream(b);
            o.writeObject(snapshot);
            byte[] snapshotBytes = b.toByteArray();
            return ByteString.copyFrom(snapshotBytes);
        } finally {
            if (o != null) {
                o.flush();
                o.close();
            }
            if (b != null) {
                b.close();
            }
        }
    }

    private static Object toObject(byte [] bs) throws ClassNotFoundException, IOException {
        Object obj = null;
        ByteArrayInputStream bis = null;
        ObjectInputStream ois = null;
        try {
            bis = new ByteArrayInputStream(bs);
            ois = new ObjectInputStream(bis);
            obj = ois.readObject();
        } finally {
            if (bis != null) {
                bis.close();
            }
            if (ois != null) {
                ois.close();
            }
        }
        return obj;
    }
}
