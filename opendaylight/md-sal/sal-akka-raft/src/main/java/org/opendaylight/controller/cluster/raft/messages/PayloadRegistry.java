/*
 * Copyright (c) 2023 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.messages;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import org.opendaylight.controller.cluster.datastore.persisted.AbortTransactionPayload;
import org.opendaylight.controller.cluster.datastore.persisted.CloseLocalHistoryPayload;
import org.opendaylight.controller.cluster.datastore.persisted.CommitTransactionPayload;
import org.opendaylight.controller.cluster.datastore.persisted.CreateLocalHistoryPayload;
import org.opendaylight.controller.cluster.datastore.persisted.DisableTrackingPayload;
import org.opendaylight.controller.cluster.datastore.persisted.PurgeLocalHistoryPayload;
import org.opendaylight.controller.cluster.datastore.persisted.PurgeTransactionPayload;
import org.opendaylight.controller.cluster.datastore.persisted.SkipTransactionsPayload;
import org.opendaylight.controller.cluster.example.messages.KeyValue;
import org.opendaylight.controller.cluster.io.ChunkedByteArray;
import org.opendaylight.controller.cluster.io.ChunkedOutputStream;
import org.opendaylight.controller.cluster.raft.persisted.NoopPayload;
import org.opendaylight.controller.cluster.raft.persisted.ServerConfigurationPayload;
import org.opendaylight.yangtools.concepts.Either;

public class PayloadRegistry {
    static final byte ABORT_TRANSACTION_PAYLOAD = 0x00;
    static final byte CLOSE_LOCAL_HISTORY_PAYLOAD = 0x01;
    static final byte COMMIT_TRANSACTION_PAYLOAD = 0x02;
    static final byte CREATE_LOCAL_HISTORY_PAYLOAD = 0x03;
    static final byte DISABLE_TRACKING_PAYLOAD = 0x04;
    static final byte KEY_VALUE = 0x05;
    static final byte NOOP_PAYLOAD = 0x06;
    static final byte PURGE_LOCAL_HISTORY_PAYLOAD = 0x07;
    static final byte PURGE_TRANSACTION_PAYLOAD = 0x08;
    static final byte SERVER_CONFIGURATION_PAYLOAD = 0x09;
    static final byte SKIP_TRANSACTIONS_PAYLOAD = 0x0A;
    static final byte REQUEST_VOTE_REPLY = 0x0B;
    static final byte REQUEST_VOTE = 0x0C;
    static final byte INSTALL_SNAPSHOT_REPLY = 0x0D;
    static final byte INSTALL_SNAPSHOT = 0x0E;
    static final byte APPEND_ENTRIES_REPLY = 0x0F;
    static final byte APPEND_ENTRIES = 0x10;
    static final byte ADD_SERVER_REPLY = 0x11;
    static final byte REMOVE_SERVER_REPLY = 0x12;
    static final byte SERVER_CHANGE_REPLY = 0x13;
    static final byte ADD_SERVER = 0x14;
    static final byte CHANGE_SERVERS_VOTING_STATUS = 0x15;
    static final byte REMOVE_SERVER = 0x16;
    static final byte REQUEST_LEADERSHIP = 0x17;
    static final byte SERVER_REMOVED = 0x18;
    static final byte UNINITIALIZED_FOLLOWER_SNAPSHOT_REPLY = 0x19;

    public static void writePayloadTo(final SerializableMessage message, final DataOutput out) throws IOException {
        if (message instanceof AbortTransactionPayload) {
            out.write(ABORT_TRANSACTION_PAYLOAD);
            message.writeTo(out);
        } else if (message instanceof CloseLocalHistoryPayload) {
            out.write(CLOSE_LOCAL_HISTORY_PAYLOAD);
            message.writeTo(out);
        } else if (message instanceof CommitTransactionPayload) {
            out.write(COMMIT_TRANSACTION_PAYLOAD);
            message.writeTo(out);
        } else if (message instanceof CreateLocalHistoryPayload) {
            out.write(CREATE_LOCAL_HISTORY_PAYLOAD);
            message.writeTo(out);
        } else if (message instanceof DisableTrackingPayload) {
            out.write(DISABLE_TRACKING_PAYLOAD);
            message.writeTo(out);
        } else if (message instanceof KeyValue) {
            out.write(KEY_VALUE);
            message.writeTo(out);
        } else if (message instanceof NoopPayload) {
            out.write(NOOP_PAYLOAD);
            message.writeTo(out);
        } else if (message instanceof PurgeLocalHistoryPayload) {
            out.write(PURGE_LOCAL_HISTORY_PAYLOAD);
            message.writeTo(out);
        } else if (message instanceof PurgeTransactionPayload) {
            out.write(PURGE_TRANSACTION_PAYLOAD);
            message.writeTo(out);
        } else if (message instanceof ServerConfigurationPayload) {
            out.write(SERVER_CONFIGURATION_PAYLOAD);
            message.writeTo(out);
        } else if (message instanceof SkipTransactionsPayload) {
            out.write(SKIP_TRANSACTIONS_PAYLOAD);
            message.writeTo(out);
        } else if (message instanceof RequestVoteReply) {
            out.write(REQUEST_VOTE_REPLY);
            message.writeTo(out);
        } else if (message instanceof RequestVote) {
            out.write(REQUEST_VOTE);
            message.writeTo(out);
        } else if (message instanceof InstallSnapshotReply) {
            out.write(INSTALL_SNAPSHOT_REPLY);
            message.writeTo(out);
        } else if (message instanceof InstallSnapshot) {
            out.write(INSTALL_SNAPSHOT);
            message.writeTo(out);
        } else if (message instanceof AppendEntriesReply) {
            out.write(APPEND_ENTRIES_REPLY);
            message.writeTo(out);
        } else if (message instanceof AppendEntries) {
            out.write(APPEND_ENTRIES);
            message.writeTo(out);
        } else if (message instanceof AddServerReply) {
            out.write(ADD_SERVER_REPLY);
            message.writeTo(out);
        } else if (message instanceof RemoveServerReply) {
            out.write(REMOVE_SERVER_REPLY);
            message.writeTo(out);
        } else if (message instanceof ServerChangeReply) {
            out.write(SERVER_CHANGE_REPLY);
            message.writeTo(out);
        } else if (message instanceof AddServer) {
            out.write(ADD_SERVER);
            message.writeTo(out);
        } else if (message instanceof ChangeServersVotingStatus) {
            out.write(CHANGE_SERVERS_VOTING_STATUS);
            message.writeTo(out);
        } else if (message instanceof RemoveServer) {
            out.write(REMOVE_SERVER);
            message.writeTo(out);
        } else if (message instanceof RequestLeadership) {
            out.write(REQUEST_LEADERSHIP);
            message.writeTo(out);
        } else if (message instanceof ServerRemoved) {
            out.write(SERVER_REMOVED);
            message.writeTo(out);
        } else if (message instanceof UnInitializedFollowerSnapshotReply) {
            out.write(UNINITIALIZED_FOLLOWER_SNAPSHOT_REPLY);
            message.writeTo(out);
        }
    }

    public static Object readPayloadFrom(final DataInput in) throws IOException {
        var type = in.readByte();
        return switch (type) {
            case ABORT_TRANSACTION_PAYLOAD -> AbortTransactionPayload.readFrom(in);
            case CLOSE_LOCAL_HISTORY_PAYLOAD -> CloseLocalHistoryPayload.readFrom(in);
            case COMMIT_TRANSACTION_PAYLOAD -> CommitTransactionPayload.readFrom(in);
            case CREATE_LOCAL_HISTORY_PAYLOAD -> CreateLocalHistoryPayload.readFrom(in);
            case DISABLE_TRACKING_PAYLOAD -> DisableTrackingPayload.readFrom(in);
            case KEY_VALUE -> KeyValue.readFrom(in);
            case NOOP_PAYLOAD -> NoopPayload.readFrom(in);
            case PURGE_LOCAL_HISTORY_PAYLOAD -> PurgeLocalHistoryPayload.readFrom(in);
            case PURGE_TRANSACTION_PAYLOAD -> PurgeTransactionPayload.readFrom(in);
            case SERVER_CONFIGURATION_PAYLOAD -> ServerConfigurationPayload.readFrom(in);
            case SKIP_TRANSACTIONS_PAYLOAD -> SkipTransactionsPayload.readFrom(in);
            case REQUEST_VOTE_REPLY -> RequestVoteReply.readFrom(in);
            case REQUEST_VOTE -> RequestVote.readFrom(in);
            case INSTALL_SNAPSHOT_REPLY -> InstallSnapshotReply.readFrom(in);
            case INSTALL_SNAPSHOT -> InstallSnapshot.readFrom(in);
            case APPEND_ENTRIES_REPLY -> AppendEntriesReply.readFrom(in);
            case APPEND_ENTRIES -> AppendEntries.readFrom(in);
            case ADD_SERVER_REPLY -> AddServerReply.readFrom(in);
            case REMOVE_SERVER_REPLY -> RemoveServerReply.readFrom(in);
            case SERVER_CHANGE_REPLY -> ServerChangeReply.readFrom(in);
            case ADD_SERVER -> AddServer.readFrom(in);
            case CHANGE_SERVERS_VOTING_STATUS -> ChangeServersVotingStatus.readFrom(in);
            case REMOVE_SERVER -> RemoveServer.readFrom(in);
            case REQUEST_LEADERSHIP -> RequestLeadership.readFrom(in);
            case SERVER_REMOVED -> ServerRemoved.readFrom(in);
            case UNINITIALIZED_FOLLOWER_SNAPSHOT_REPLY -> UnInitializedFollowerSnapshotReply.readFrom(in);
            default -> throw new IOException("Unknown type " + type);
        };
    }

    public static Either<byte[], ChunkedByteArray> getFragmented(final SerializableMessage message, int maxsize) throws IOException {
        final ChunkedOutputStream bos = new ChunkedOutputStream(512, maxsize);
        final DataOutput out = new DataOutputStream(bos);
        writePayloadTo(message, out);

        return bos.toVariant();
    }

}