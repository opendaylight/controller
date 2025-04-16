/*
 * Copyright (c) 2017 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.behaviors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import org.apache.commons.lang3.SerializationUtils;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for FollowerIdentifier.
 *
 * @author Thomas Pantelis
 */
class FollowerIdentifierTest {
    @Test
    void testSerialization() {
        final var expected = new FollowerIdentifier("follower1");
        final var bytes = SerializationUtils.serialize(expected);
        assertEquals(87, bytes.length);
        assertEquals(expected, assertInstanceOf(FollowerIdentifier.class, SerializationUtils.deserialize(bytes)));
    }
}
