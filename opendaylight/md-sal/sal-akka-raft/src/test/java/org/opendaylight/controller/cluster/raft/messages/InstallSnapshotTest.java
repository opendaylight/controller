/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.messages;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Optional;
import java.util.OptionalInt;
import org.apache.commons.lang.SerializationUtils;
import org.junit.Test;
import org.opendaylight.controller.cluster.raft.RaftVersions;
import org.opendaylight.controller.cluster.raft.persisted.ServerConfigurationPayload;
import org.opendaylight.controller.cluster.raft.persisted.ServerInfo;

/**
 * Unit tests for InstallSnapshot.
 *
 * @author Thomas Pantelis
 */
public class InstallSnapshotTest {

    @Test
    public void testSerialization() {
        byte[] data = new byte[1000];
        for (int i = 0, j = 0; i < data.length; i++) {
            data[i] = (byte)j;
            if (++j >= 255) {
                j = 0;
            }
        }

        ServerConfigurationPayload serverConfig = new ServerConfigurationPayload(Arrays.asList(
                new ServerInfo("leader", true), new ServerInfo("follower", false)));
        InstallSnapshot expected = new InstallSnapshot(3L, "leaderId", 11L, 2L, data, 5, 6, OptionalInt.of(54321),
            Optional.of(serverConfig));

        Object serialized = expected.toSerializable(RaftVersions.CURRENT_VERSION);
        assertEquals("Serialized type", InstallSnapshot.class, serialized.getClass());

        var bytes = SerializationUtils.serialize((Serializable) serialized);
        assertEquals(1302, bytes.length);
        var actual = (InstallSnapshot) SerializationUtils.deserialize(bytes);

        verifyInstallSnapshot(expected, actual);

        expected = new InstallSnapshot(3L, "leaderId", 11L, 2L, data, 5, 6);
        bytes = SerializationUtils.serialize((Serializable) expected.toSerializable(RaftVersions.CURRENT_VERSION));
        assertEquals(1165, bytes.length);
        actual = (InstallSnapshot) SerializationUtils.deserialize(bytes);
        verifyInstallSnapshot(expected, actual);
    }

    private static void verifyInstallSnapshot(final InstallSnapshot expected, final InstallSnapshot actual) {
        assertEquals("getTerm", expected.getTerm(), actual.getTerm());
        assertEquals("getChunkIndex", expected.getChunkIndex(), actual.getChunkIndex());
        assertEquals("getTotalChunks", expected.getTotalChunks(), actual.getTotalChunks());
        assertEquals("getLastIncludedTerm", expected.getLastIncludedTerm(), actual.getLastIncludedTerm());
        assertEquals("getLastIncludedIndex", expected.getLastIncludedIndex(), actual.getLastIncludedIndex());
        assertEquals("getLeaderId", expected.getLeaderId(), actual.getLeaderId());
        assertEquals("getChunkIndex", expected.getChunkIndex(), actual.getChunkIndex());
        assertArrayEquals("getData", expected.getData(), actual.getData());

        assertEquals("getLastChunkHashCode present", expected.getLastChunkHashCode().isPresent(),
                actual.getLastChunkHashCode().isPresent());
        if (expected.getLastChunkHashCode().isPresent()) {
            assertEquals("getLastChunkHashCode", expected.getLastChunkHashCode(),
                    actual.getLastChunkHashCode());
        }

        assertEquals("getServerConfig present", expected.getServerConfig().isPresent(),
                actual.getServerConfig().isPresent());
        if (expected.getServerConfig().isPresent()) {
            assertEquals("getServerConfig", expected.getServerConfig().get().getServerConfig(),
                    actual.getServerConfig().get().getServerConfig());
        }
    }
}
