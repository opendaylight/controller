/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.opendaylight.controller.cluster.access.ABIVersion.POTASSIUM;
import static org.opendaylight.controller.cluster.access.ABIVersion.TEST_FUTURE_VERSION;
import static org.opendaylight.controller.cluster.access.ABIVersion.TEST_PAST_VERSION;

import com.google.common.io.ByteStreams;
import java.io.IOException;
import org.junit.jupiter.api.Test;

class ABIVersionTest {
    @Test
    void testInvalidVersions() {
        assertThat(TEST_PAST_VERSION.compareTo(TEST_FUTURE_VERSION)).isLessThan(0);
        assertThat(TEST_PAST_VERSION.compareTo(POTASSIUM)).isLessThan(0);
        assertThat(TEST_FUTURE_VERSION.compareTo(POTASSIUM)).isGreaterThan(0);
    }

    @Test
    void testMagnesiumVersion() throws Exception {
        assertEquals((short)10, POTASSIUM.shortValue());
        assertEquals(POTASSIUM, ABIVersion.valueOf(POTASSIUM.shortValue()));
        assertEquals(POTASSIUM, ABIVersion.readFrom(ByteStreams.newDataInput(writeVersion(POTASSIUM))));
    }

    @Test
    void testInvalidPastVersion() {
        final var ex = assertThrows(PastVersionException.class,
            () -> ABIVersion.valueOf(TEST_PAST_VERSION.shortValue()));
        assertEquals("Version 0 is too old", ex.getMessage());
    }

    @Test
    void testInvalidFutureVersion() {
        final var ex = assertThrows(FutureVersionException.class,
            () -> ABIVersion.valueOf(TEST_FUTURE_VERSION.shortValue()));
        assertEquals("Version 65535 is too new", ex.getMessage());
    }

    private static byte[] writeVersion(final ABIVersion version) {
        final var bado = ByteStreams.newDataOutput();
        bado.writeShort(version.shortValue());
        return bado.toByteArray();
    }

    @Test
    void testBadRead() {
        final var in = ByteStreams.newDataInput(writeVersion(TEST_PAST_VERSION));
        final var ex = assertThrows(IOException.class, () -> ABIVersion.readFrom(in));
        assertEquals("Unsupported version", ex.getMessage());
        assertInstanceOf(PastVersionException.class, ex.getCause());
    }
}
