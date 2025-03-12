/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
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

/**
 * Unit tests for InstallSnapshotReply.
 *
 * @author Thomas Pantelis
 */
class InstallSnapshotReplyTest {
    @Test
    void testSerialization() {
        final var expected = new InstallSnapshotReply(5L, "follower", 1, true);
        final var bytes = SerializationUtils.serialize(expected);
        assertEquals(95, bytes.length);
        final var cloned = assertInstanceOf(InstallSnapshotReply.class, SerializationUtils.deserialize(bytes));

        assertEquals(expected.getTerm(), cloned.getTerm());
        assertEquals(expected.getFollowerId(), cloned.getFollowerId());
        assertEquals(expected.getChunkIndex(), cloned.getChunkIndex());
        assertEquals(expected.isSuccess(), cloned.isSuccess());
    }
}
