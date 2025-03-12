/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.messages;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import org.apache.commons.lang3.SerializationUtils;
import org.junit.jupiter.api.Test;
import org.opendaylight.controller.cluster.raft.RaftVersions;

/**
 * Unit tests for AppendEntriesReply.
 *
 * @author Thomas Pantelis
 */
class AppendEntriesReplyTest {
    @Test
    void testSerialization() {
        final var expected = new AppendEntriesReply("follower", 5, true, 100, 4, (short)6, true, true,
            RaftVersions.CURRENT_VERSION);

        final var bytes = SerializationUtils.serialize(expected);
        assertEquals(98, bytes.length);
        final var cloned = assertInstanceOf(AppendEntriesReply.class, SerializationUtils.deserialize(bytes));

        assertEquals(expected.getTerm(), cloned.getTerm());
        assertEquals(expected.getFollowerId(), cloned.getFollowerId());
        assertEquals(expected.getLogLastTerm(), cloned.getLogLastTerm());
        assertEquals(expected.getLogLastIndex(), cloned.getLogLastIndex());
        assertEquals(expected.getPayloadVersion(), cloned.getPayloadVersion());
        assertEquals(expected.getRaftVersion(), cloned.getRaftVersion());
        assertEquals(expected.isForceInstallSnapshot(), cloned.isForceInstallSnapshot());
        assertEquals(expected.isNeedsLeaderAddress(), cloned.isNeedsLeaderAddress());
    }
}
