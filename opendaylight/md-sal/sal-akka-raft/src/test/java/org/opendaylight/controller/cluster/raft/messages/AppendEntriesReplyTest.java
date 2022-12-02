/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.messages;

import static org.junit.Assert.assertEquals;

import org.apache.commons.lang.SerializationUtils;
import org.junit.Test;
import org.opendaylight.controller.cluster.raft.RaftVersions;

/**
 * Unit tests for AppendEntriesReply.
 *
 * @author Thomas Pantelis
 */
public class AppendEntriesReplyTest {
    @Test
    public void testSerialization() {
        final var expected = new AppendEntriesReply("follower", 5, true, 100, 4, (short)6, true, true,
            RaftVersions.CURRENT_VERSION);

        final var bytes = SerializationUtils.serialize(expected);
        assertEquals(143, bytes.length);
        final var cloned = (AppendEntriesReply) SerializationUtils.deserialize(bytes);

        assertEquals("getTerm", expected.getTerm(), cloned.getTerm());
        assertEquals("getFollowerId", expected.getFollowerId(), cloned.getFollowerId());
        assertEquals("getLogLastTerm", expected.getLogLastTerm(), cloned.getLogLastTerm());
        assertEquals("getLogLastIndex", expected.getLogLastIndex(), cloned.getLogLastIndex());
        assertEquals("getPayloadVersion", expected.getPayloadVersion(), cloned.getPayloadVersion());
        assertEquals("getRaftVersion", expected.getRaftVersion(), cloned.getRaftVersion());
        assertEquals("isForceInstallSnapshot", expected.isForceInstallSnapshot(), cloned.isForceInstallSnapshot());
        assertEquals("isNeedsLeaderAddress", expected.isNeedsLeaderAddress(), cloned.isNeedsLeaderAddress());
    }

    @Test
    @Deprecated
    public void testPreFluorineSerialization() {
        final var expected = new AppendEntriesReply("follower", 5, true, 100, 4, (short)6, true, true,
            RaftVersions.BORON_VERSION);

        final var bytes = SerializationUtils.serialize(expected);
        assertEquals(141, bytes.length);
        final var cloned = (AppendEntriesReply) SerializationUtils.deserialize(bytes);

        assertEquals("getTerm", expected.getTerm(), cloned.getTerm());
        assertEquals("getFollowerId", expected.getFollowerId(), cloned.getFollowerId());
        assertEquals("getLogLastTerm", expected.getLogLastTerm(), cloned.getLogLastTerm());
        assertEquals("getLogLastIndex", expected.getLogLastIndex(), cloned.getLogLastIndex());
        assertEquals("getPayloadVersion", expected.getPayloadVersion(), cloned.getPayloadVersion());
        assertEquals("getRaftVersion", expected.getRaftVersion(), cloned.getRaftVersion());
        assertEquals("isForceInstallSnapshot", expected.isForceInstallSnapshot(), cloned.isForceInstallSnapshot());
        assertEquals("isNeedsLeaderAddress", false, cloned.isNeedsLeaderAddress());
    }
}
