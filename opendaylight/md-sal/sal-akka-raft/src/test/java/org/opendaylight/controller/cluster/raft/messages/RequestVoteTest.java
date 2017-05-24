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
 * Unit tests for RequestVote.
 *
 * @author Thomas Pantelis
 */
public class RequestVoteTest {

    @Test
    public void testSerialization() {
        RequestVote expected = new RequestVote(4, "candidateId", 3, 2);
        RequestVote cloned = (RequestVote) SerializationUtils.clone(expected);

        assertEquals("getTerm", expected.getTerm(), cloned.getTerm());
        assertEquals("getCandidateId", expected.getCandidateId(), cloned.getCandidateId());
        assertEquals("getLastLogIndex", expected.getLastLogIndex(), cloned.getLastLogIndex());
        assertEquals("getLastLogTerm", expected.getLastLogTerm(), cloned.getLastLogTerm());
    }
}
