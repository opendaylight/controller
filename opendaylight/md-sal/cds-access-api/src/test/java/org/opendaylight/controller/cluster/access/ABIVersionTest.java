/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.opendaylight.controller.cluster.access.ABIVersion.MAGNESIUM;
import static org.opendaylight.controller.cluster.access.ABIVersion.TEST_FUTURE_VERSION;
import static org.opendaylight.controller.cluster.access.ABIVersion.TEST_PAST_VERSION;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import java.io.IOException;
import org.junit.Test;

public class ABIVersionTest {
    @Test
    public void testInvalidVersions() {
        assertTrue(TEST_PAST_VERSION.compareTo(TEST_FUTURE_VERSION) < 0);
        assertTrue(TEST_PAST_VERSION.compareTo(MAGNESIUM) < 0);
        assertTrue(TEST_FUTURE_VERSION.compareTo(MAGNESIUM) > 0);
    }

    @Test
    public void testMagnesiumVersion() throws Exception {
        assertEquals((short)8, MAGNESIUM.shortValue());
        assertEquals(MAGNESIUM, ABIVersion.valueOf(MAGNESIUM.shortValue()));
        assertEquals(MAGNESIUM, ABIVersion.readFrom(ByteStreams.newDataInput(writeVersion(MAGNESIUM))));
    }

    @Test
    public void testInvalidPastVersion() {
        assertThrows(PastVersionException.class, () -> ABIVersion.valueOf(TEST_PAST_VERSION.shortValue()));
    }

    @Test
    public void testInvalidFutureVersion() {
        assertThrows(FutureVersionException.class, () -> ABIVersion.valueOf(TEST_FUTURE_VERSION.shortValue()));
    }

    private static byte[] writeVersion(final ABIVersion version) {
        final ByteArrayDataOutput bado = ByteStreams.newDataOutput();
        bado.writeShort(version.shortValue());
        return bado.toByteArray();
    }

    @Test
    public void testBadRead() {
        final var in = ByteStreams.newDataInput(writeVersion(TEST_PAST_VERSION));
        assertThrows(IOException.class, () -> ABIVersion.readFrom(in));
    }
}
