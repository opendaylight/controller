/*
 * Copyright (c) 2016 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.persisted;

import static org.junit.Assert.assertEquals;

import org.apache.commons.lang.SerializationUtils;
import org.junit.Test;
import org.opendaylight.controller.cluster.raft.MockRaftActorContext;

/**
 * Unit tests for SimpleReplicatedLogEntry.
 *
 * @author Thomas Pantelis
 */
public class SimpleReplicatedLogEntryTest {

    @Test
    public void testSerialization() {
        SimpleReplicatedLogEntry expected = new SimpleReplicatedLogEntry(0, 1,
                new MockRaftActorContext.MockPayload("A"));
        SimpleReplicatedLogEntry cloned = (SimpleReplicatedLogEntry) SerializationUtils.clone(expected);

        assertEquals("getTerm", expected.getTerm(), cloned.getTerm());
        assertEquals("getIndex", expected.getIndex(), cloned.getIndex());
        assertEquals("getData", expected.getData(), cloned.getData());
        assertEquals("isMigrated", false, cloned.isMigrated());
    }
}
