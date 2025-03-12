/*
 * Copyright (c) 2017 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.client.messages;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.apache.commons.lang3.SerializationUtils;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for Shutdown.
 *
 * @author Thomas Pantelis
 */
class ShutdownTest {
    @Test
    void test() {
        final var bytes = SerializationUtils.serialize(Shutdown.INSTANCE);
        assertEquals(86, bytes.length);
        assertSame(Shutdown.INSTANCE, SerializationUtils.deserialize(bytes));
    }
}
