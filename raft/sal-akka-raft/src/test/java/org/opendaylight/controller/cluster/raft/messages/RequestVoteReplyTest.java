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
 * Unit tests for RequestVoteReply.
 *
 * @author Thomas Pantelis
 */
class RequestVoteReplyTest {
    @Test
    void testSerialization() {
        final var expected = new RequestVoteReply(5, true);
        final var bytes = SerializationUtils.serialize(expected);
        assertEquals(78, bytes.length);
        final var cloned = assertInstanceOf(RequestVoteReply.class, SerializationUtils.deserialize(bytes));

        assertEquals(expected.getTerm(), cloned.getTerm());
        assertEquals(expected.isVoteGranted(), cloned.isVoteGranted());
    }
}
