/*
 * Copyright (c) 2018 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.persisted;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.NotSerializableException;
import org.apache.pekko.actor.ExtendedActorSystem;
import org.apache.pekko.testkit.javadsl.TestKit;
import org.junit.Test;
import org.opendaylight.controller.cluster.raft.MockRaftActorContext;

/**
 * Unit tests for SimpleReplicatedLogEntrySerializer.
 *
 * @author Thomas Pantelis
 */
public class SimpleReplicatedLogEntrySerializerTest {

    @Test
    public void testToAndFromBinary() throws NotSerializableException {
        SimpleReplicatedLogEntry expected = new SimpleReplicatedLogEntry(0, 1,
                new MockRaftActorContext.MockPayload("A"));

        final ExtendedActorSystem system = (ExtendedActorSystem) ExtendedActorSystem.create("test");
        final Object deserialized;
        try {
            final SimpleReplicatedLogEntrySerializer serializer = new SimpleReplicatedLogEntrySerializer(system);
            final byte[] bytes = serializer.toBinary(expected);
            deserialized = serializer.fromBinary(bytes, SimpleReplicatedLogEntry.class);
        } finally {
            TestKit.shutdownActorSystem(system);
        }

        assertNotNull("fromBinary returned null", deserialized);
        assertEquals("fromBinary return type", SimpleReplicatedLogEntry.class, deserialized.getClass());

        SimpleReplicatedLogEntry actual = (SimpleReplicatedLogEntry)deserialized;
        assertEquals("getTerm", expected.term(), actual.term());
        assertEquals("getIndex", expected.index(), actual.index());
        assertEquals("getData", expected.getData(), actual.getData());
    }
}
