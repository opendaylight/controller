/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.concepts;

import static org.junit.Assert.assertEquals;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import java.io.IOException;
import org.junit.Test;

public class WritableObjectsTest {

    private static void assertRecovery(final long expected) throws IOException {
        final ByteArrayDataOutput out = ByteStreams.newDataOutput();
        WritableObjects.writeLong(out, expected);
        final long actual = WritableObjects.readLong(ByteStreams.newDataInput(out.toByteArray()));
        assertEquals(Long.toUnsignedString(expected, 16), Long.toUnsignedString(actual, 16));
    }

    @Test
    public void testReadWriteLong() throws IOException {
        assertRecovery(0L);
        assertRecovery(1L);
        assertRecovery(255L);
        assertRecovery(256L);

        assertRecovery(Long.MAX_VALUE);
        assertRecovery(Long.MIN_VALUE);

        assertRecovery(0xF000000000000000L);
        assertRecovery(0x0F00000000000000L);
        assertRecovery(0x00F0000000000000L);
        assertRecovery(0x000F000000000000L);
        assertRecovery(0x0000F00000000000L);
        assertRecovery(0x00000F0000000000L);
        assertRecovery(0x000000F000000000L);
        assertRecovery(0x0000000F00000000L);
        assertRecovery(0x00000000F0000000L);
        assertRecovery(0x000000000F000000L);
        assertRecovery(0x0000000000F00000L);
        assertRecovery(0x00000000000F0000L);
        assertRecovery(0x000000000000F000L);
        assertRecovery(0x0000000000000F00L);
        assertRecovery(0x00000000000000F0L);

        assertRecovery(0xF0F0F0F0F0F0F0F0L);
        assertRecovery(0x0FF0F0F0F0F0F0F0L);
        assertRecovery(0x00F0F0F0F0F0F0F0L);
        assertRecovery(0x000FF0F0F0F0F0F0L);
        assertRecovery(0x0000F0F0F0F0F0F0L);
        assertRecovery(0x00000F00F0F0F0F0L);
        assertRecovery(0x000000F0F0F0F0F0L);
        assertRecovery(0x0000000FF0F0F0F0L);
        assertRecovery(0x00000000F0F0F0F0L);
        assertRecovery(0x000000000FF0F0F0L);
        assertRecovery(0x0000000000F0F0F0L);
        assertRecovery(0x00000000000FF0F0L);
        assertRecovery(0x000000000000F0F0L);
        assertRecovery(0x0000000000000FF0L);
        assertRecovery(0x00000000000000F0L);

        assertRecovery(0x8000000000000000L);
        assertRecovery(0x0800000000000000L);
        assertRecovery(0x0080000000000000L);
        assertRecovery(0x0008000000000000L);
        assertRecovery(0x0000800000000000L);
        assertRecovery(0x0000080000000000L);
        assertRecovery(0x0000008000000000L);
        assertRecovery(0x0000000800000000L);
        assertRecovery(0x0000000080000000L);
        assertRecovery(0x0000000008000000L);
        assertRecovery(0x0000000000800000L);
        assertRecovery(0x0000000000080000L);
        assertRecovery(0x0000000000008000L);
        assertRecovery(0x0000000000000800L);
        assertRecovery(0x0000000000000080L);
        assertRecovery(0x0000000000000008L);
    }
}
