/*
 * Copyright (c) 2022 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.persisted;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import org.apache.commons.lang3.SerializationUtils;
import org.junit.Test;

public class NoopPayloadTest {
    @Test
    public void testSerialization() {
        final var bytes = SerializationUtils.serialize(NoopPayload.INSTANCE);
        assertEquals(74, bytes.length);
        assertSame(NoopPayload.INSTANCE, SerializationUtils.deserialize(bytes));
    }
}
