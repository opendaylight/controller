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

/**
 * Unit tests for AppendEntriesReply.
 *
 * @author Thomas Pantelis
 */
public class AppendEntriesReplyTest {

    @Test
    public void testSerialization() {
        AppendEntriesReply expected = new AppendEntriesReply("follower", 5, true, 100, 4, (short)6);
        AppendEntriesReply cloned = (AppendEntriesReply) SerializationUtils.clone(expected);

        assertEquals("getTerm", expected.getTerm(), cloned.getTerm());
        assertEquals("getFollowerId", expected.getFollowerId(), cloned.getFollowerId());
        assertEquals("getLogLastTerm", expected.getLogLastTerm(), cloned.getLogLastTerm());
        assertEquals("getLogLastIndex", expected.getLogLastIndex(), cloned.getLogLastIndex());
        assertEquals("getPayloadVersion", expected.getPayloadVersion(), cloned.getPayloadVersion());
        assertEquals("getRaftVersion", expected.getRaftVersion(), cloned.getRaftVersion());
    }
}
