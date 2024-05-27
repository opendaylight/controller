/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.persisted;

import static org.junit.Assert.assertEquals;

import org.apache.commons.lang3.SerializationUtils;
import org.junit.Test;

/**
 * Unit tests for DeleteEntries.
 *
 * @author Thomas Pantelis
 */
public class DeleteEntriesTest {
    @Test
    public void testSerialization() {
        final var expected = new DeleteEntries(5);
        final var bytes = SerializationUtils.serialize(expected);
        assertEquals(79, bytes.length);
        final var cloned = (DeleteEntries) SerializationUtils.deserialize(bytes);

        assertEquals("getFromIndex", expected.getFromIndex(), cloned.getFromIndex());
    }
}
