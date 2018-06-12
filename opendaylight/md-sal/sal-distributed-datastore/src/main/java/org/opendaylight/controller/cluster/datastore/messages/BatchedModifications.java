/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.messages;

import static java.util.Objects.requireNonNull;

import com.google.common.base.Preconditions;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;
import javax.annotation.Nullable;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.controller.cluster.datastore.DataStoreVersions;
import org.opendaylight.controller.cluster.datastore.modification.MutableCompositeModification;

/**
 * Message used to batch write, merge, delete modification operations to the  ShardTransaction actor.
 *
 * @author Thomas Pantelis
 */
public class BatchedModifications extends MutableCompositeModification {
    private static final long serialVersionUID = 1L;

    private boolean ready;
    private boolean doCommitOnReady;
    private int totalMessagesSent;
    private TransactionIdentifier transactionId;
    @Nullable
    private SortedSet<String> participatingShardNames;

    public BatchedModifications() {
    }

    public BatchedModifications(TransactionIdentifier transactionId, short version) {
        super(version);
        this.transactionId = requireNonNull(transactionId, "transactionID can't be null");
    }

    public boolean isReady() {
        return ready;
    }

    public void setReady(Optional<SortedSet<String>> possibleParticipatingShardNames) {
        this.ready = true;
        this.participatingShardNames = requireNonNull(possibleParticipatingShardNames).orElse(null);
        Preconditions.checkArgument(this.participatingShardNames == null || this.participatingShardNames.size() > 1);
    }

    public void setReady() {
        setReady(Optional.empty());
    }

    public Optional<SortedSet<String>> getParticipatingShardNames() {
        return Optional.ofNullable(participatingShardNames);
    }

    public boolean isDoCommitOnReady() {
        return doCommitOnReady;
    }

    public void setDoCommitOnReady(boolean doCommitOnReady) {
        this.doCommitOnReady = doCommitOnReady;
    }

    public int getTotalMessagesSent() {
        return totalMessagesSent;
    }

    public void setTotalMessagesSent(int totalMessagesSent) {
        this.totalMessagesSent = totalMessagesSent;
    }

    public TransactionIdentifier getTransactionId() {
        return transactionId;
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        super.readExternal(in);
        transactionId = TransactionIdentifier.readFrom(in);
        ready = in.readBoolean();
        totalMessagesSent = in.readInt();
        doCommitOnReady = in.readBoolean();

        if (getVersion() >= DataStoreVersions.FLUORINE_VERSION) {
            final int count = in.readInt();
            if (count != 0) {
                SortedSet<String> shardNames = new TreeSet<>();
                for (int i = 0; i < count; i++) {
                    shardNames.add((String) in.readObject());
                }

                participatingShardNames = shardNames;
            }
        }
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        super.writeExternal(out);
        transactionId.writeTo(out);
        out.writeBoolean(ready);
        out.writeInt(totalMessagesSent);
        out.writeBoolean(doCommitOnReady);

        if (getVersion() >= DataStoreVersions.FLUORINE_VERSION) {
            if (participatingShardNames != null) {
                out.writeInt(participatingShardNames.size());
                for (String shardName: participatingShardNames) {
                    out.writeObject(shardName);
                }
            } else {
                out.writeInt(0);
            }
        }
    }

    @Override
    public String toString() {
        return "BatchedModifications [transactionId=" + transactionId
                + ", ready=" + isReady()
                + ", participatingShardNames=" + participatingShardNames
                + ", totalMessagesSent=" + totalMessagesSent
                + ", modifications size=" + getModifications().size() + "]";
    }
}
