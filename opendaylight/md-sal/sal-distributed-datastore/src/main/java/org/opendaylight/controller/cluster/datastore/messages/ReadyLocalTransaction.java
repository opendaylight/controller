/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.messages;

import static java.util.Objects.requireNonNull;

import java.util.Optional;
import java.util.SortedSet;
import javax.annotation.Nullable;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.controller.cluster.datastore.DataStoreVersions;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeModification;

/**
 * Message notifying the shard leader to apply modifications which have been
 * prepared locally against its DataTree. This message is not directly serializable,
 * simply because the leader and sender need to be on the same system. When it needs
 * to be sent out to a remote system, it needs to be intercepted by {@link ReadyLocalTransactionSerializer}
 * and turned into {@link BatchedModifications}.
 */
public final class ReadyLocalTransaction {
    private final DataTreeModification modification;
    private final TransactionIdentifier transactionId;
    private final boolean doCommitOnReady;
    @Nullable
    private final SortedSet<String> participatingShardNames;

    // The version of the remote system used only when needing to convert to BatchedModifications.
    private short remoteVersion = DataStoreVersions.CURRENT_VERSION;

    public ReadyLocalTransaction(final TransactionIdentifier transactionId, final DataTreeModification modification,
            final boolean doCommitOnReady, Optional<SortedSet<String>> participatingShardNames) {
        this.transactionId = requireNonNull(transactionId);
        this.modification = requireNonNull(modification);
        this.doCommitOnReady = doCommitOnReady;
        this.participatingShardNames = requireNonNull(participatingShardNames).orElse(null);
    }

    public TransactionIdentifier getTransactionId() {
        return transactionId;
    }

    public DataTreeModification getModification() {
        return modification;
    }

    public boolean isDoCommitOnReady() {
        return doCommitOnReady;
    }

    public short getRemoteVersion() {
        return remoteVersion;
    }

    public void setRemoteVersion(short remoteVersion) {
        this.remoteVersion = remoteVersion;
    }

    public Optional<SortedSet<String>> getParticipatingShardNames() {
        return Optional.ofNullable(participatingShardNames);
    }
}
