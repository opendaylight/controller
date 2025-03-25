/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.messages;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.util.OptionalInt;
import org.apache.commons.lang3.SerializationUtils;
import org.junit.jupiter.api.Test;
import org.opendaylight.controller.cluster.raft.RaftVersions;
import org.opendaylight.controller.cluster.raft.persisted.ClusterConfig;
import org.opendaylight.controller.cluster.raft.persisted.ServerInfo;

/**
 * Unit tests for InstallSnapshot.
 *
 * @author Thomas Pantelis
 */
class InstallSnapshotTest {
    @Test
    void testCurrentSerialization() {
        testSerialization(RaftVersions.CURRENT_VERSION, 1262, 1125);
    }

    @Test
    void testFluorineSerialization() {
        testSerialization(RaftVersions.FLUORINE_VERSION, 1302, 1165);
    }

    private static void testSerialization(final short raftVersion, final int fullSize, final int emptySize) {
        byte[] data = new byte[1000];
        for (int i = 0, j = 0; i < data.length; i++) {
            data[i] = (byte)j;
            if (++j >= 255) {
                j = 0;
            }
        }

        var serverConfig = new ClusterConfig(new ServerInfo("leader", true), new ServerInfo("follower", false));
        assertInstallSnapshot(fullSize, new InstallSnapshot(3L, "leaderId", 11L, 2L, data, 5, 6, OptionalInt.of(54321),
            serverConfig, raftVersion));

        assertInstallSnapshot(emptySize, new InstallSnapshot(3L, "leaderId", 11L, 2L, data, 5, 6, OptionalInt.empty(),
            null, raftVersion));
    }

    private static void assertInstallSnapshot(final int expectedSize, final InstallSnapshot expected) {
        final var bytes = SerializationUtils.serialize(expected);
        assertEquals(expectedSize, bytes.length);
        assertInstallSnapshot(expected, assertInstanceOf(InstallSnapshot.class, SerializationUtils.deserialize(bytes)));
    }

    private static void assertInstallSnapshot(final InstallSnapshot expected, final InstallSnapshot actual) {
        assertEquals(expected.getTerm(), actual.getTerm());
        assertEquals(expected.getChunkIndex(), actual.getChunkIndex());
        assertEquals(expected.getTotalChunks(), actual.getTotalChunks());
        assertEquals(expected.getLastIncludedTerm(), actual.getLastIncludedTerm());
        assertEquals(expected.getLastIncludedIndex(), actual.getLastIncludedIndex());
        assertEquals(expected.getLeaderId(), actual.getLeaderId());
        assertEquals(expected.getChunkIndex(), actual.getChunkIndex());
        assertArrayEquals(expected.getData(), actual.getData());

        assertEquals(expected.getLastChunkHashCode(), actual.getLastChunkHashCode());
        assertEquals(expected.serverConfig(), actual.serverConfig());
    }
}
