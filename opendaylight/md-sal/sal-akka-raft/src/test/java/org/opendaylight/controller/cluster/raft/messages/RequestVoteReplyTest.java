/*
 * Copyright (c) 2017 Inocybe Technologies and others.  All rights reserved.
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
 * Unit tests for RequestVoteReply.
 *
 * @author Thomas Pantelis
 */
public class RequestVoteReplyTest {

    @Test
    public void testSerialization() {
        RequestVoteReply expected = new RequestVoteReply(5, true);
        RequestVoteReply cloned = (RequestVoteReply) SerializationUtils.clone(expected);

        assertEquals("getTerm", expected.getTerm(), cloned.getTerm());
        assertEquals("isVoteGranted", expected.isVoteGranted(), cloned.isVoteGranted());
    }
}
