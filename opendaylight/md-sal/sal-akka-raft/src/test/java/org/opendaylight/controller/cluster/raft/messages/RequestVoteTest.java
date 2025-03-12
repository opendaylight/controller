/*
 * Copyright (c) 2017 Inocybe Technologies and others.  All rights reserved.
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
 * Unit tests for RequestVote.
 *
 * @author Thomas Pantelis
 */
class RequestVoteTest {
    @Test
    void testSerialization() {
        final var expected = new RequestVote(4, "candidateId", 3, 2);
        final var bytes = SerializationUtils.serialize(expected);
        assertEquals(97, bytes.length);
        final var cloned = assertInstanceOf(RequestVote.class, SerializationUtils.deserialize(bytes));

        assertEquals(expected.getTerm(), cloned.getTerm());
        assertEquals(expected.getCandidateId(), cloned.getCandidateId());
        assertEquals(expected.getLastLogIndex(), cloned.getLastLogIndex());
        assertEquals(expected.getLastLogTerm(), cloned.getLastLogTerm());
    }
}
