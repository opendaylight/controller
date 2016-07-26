/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.messages;

import static org.junit.Assert.assertEquals;
import org.apache.commons.lang.SerializationUtils;
import org.junit.Test;

/**
 * Unit tests for InstallSnapshotReply.
 *
 * @author Thomas Pantelis
 */
public class InstallSnapshotReplyTest {

    @Test
    public void testSerialization() {
        InstallSnapshotReply expected = new InstallSnapshotReply(5L, "follower", 1, true);
        InstallSnapshotReply cloned = (InstallSnapshotReply) SerializationUtils.clone(expected);

        assertEquals("getTerm", expected.getTerm(), cloned.getTerm());
        assertEquals("getFollowerId", expected.getFollowerId(), cloned.getFollowerId());
        assertEquals("getChunkIndex", expected.getChunkIndex(), cloned.getChunkIndex());
        assertEquals("isSuccess", expected.isSuccess(), cloned.isSuccess());
    }
}
