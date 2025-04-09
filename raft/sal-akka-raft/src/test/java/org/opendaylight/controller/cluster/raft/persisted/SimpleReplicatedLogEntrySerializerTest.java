/*
 * Copyright (c) 2018 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.persisted;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.io.NotSerializableException;
import org.apache.pekko.actor.ExtendedActorSystem;
import org.apache.pekko.testkit.javadsl.TestKit;
import org.junit.jupiter.api.Test;
import org.opendaylight.controller.cluster.raft.MockCommand;

/**
 * Unit tests for SimpleReplicatedLogEntrySerializer.
 *
 * @author Thomas Pantelis
 */
class SimpleReplicatedLogEntrySerializerTest {
    @Test
    void testToAndFromBinary() throws NotSerializableException {
        final var expected = new SimpleReplicatedLogEntry(0, 1, new MockCommand("A"));

        final var system = (ExtendedActorSystem) ExtendedActorSystem.create("test");
        final SimpleReplicatedLogEntry actual;
        try {
            final var serializer = new SimpleReplicatedLogEntrySerializer(system);
            final byte[] bytes = serializer.toBinary(expected);
            actual = assertInstanceOf(SimpleReplicatedLogEntry.class,
                serializer.fromBinary(bytes, SimpleReplicatedLogEntry.class));
        } finally {
            TestKit.shutdownActorSystem(system);
        }

        assertEquals(expected.term(), actual.term());
        assertEquals(expected.index(), actual.index());
        assertEquals(expected.command(), actual.command());
    }
}
