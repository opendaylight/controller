/*
 * Copyright (c) 2017 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.behaviors;

import static org.junit.Assert.assertEquals;

import org.apache.commons.lang3.SerializationUtils;
import org.junit.Test;

/**
 * Unit tests for FollowerIdentifier.
 *
 * @author Thomas Pantelis
 */
public class FollowerIdentifierTest {
    @Test
    public void testSerialization() {
        final var expected = new FollowerIdentifier("follower1");
        final var bytes = SerializationUtils.serialize(expected);
        assertEquals(87, bytes.length);
        final var cloned = (FollowerIdentifier) SerializationUtils.deserialize(bytes);
        assertEquals("cloned", expected, cloned);
    }
}
