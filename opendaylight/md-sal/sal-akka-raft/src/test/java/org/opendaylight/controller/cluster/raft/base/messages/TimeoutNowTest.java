/*
 * Copyright (c) 2017 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.base.messages;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.apache.commons.lang3.SerializationUtils;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for TimeoutNow.
 *
 * @author Thomas Pantelis
 */
class TimeoutNowTest {
    @Test
    void test() {
        final var bytes = SerializationUtils.serialize(TimeoutNow.INSTANCE);
        assertEquals(86, bytes.length);
        assertSame(TimeoutNow.INSTANCE, SerializationUtils.deserialize(bytes));
    }
}
