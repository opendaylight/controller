/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.concepts;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import org.junit.jupiter.api.Test;

class FrontendTypeTest extends AbstractIdentifierTest<FrontendType> {
    @Override
    FrontendType object() {
        return FrontendType.forName("type-1");
    }

    @Override
    FrontendType differentObject() {
        return FrontendType.forName("type-2");
    }

    @Override
    FrontendType equalObject() {
        return FrontendType.forName("type-1");
    }

    @Override
    int expectedSize() {
        return 88;
    }

    @Test
    void testWriteToReadFrom() throws Exception {
        final var type = FrontendType.forName("type");
        final var baos = new ByteArrayOutputStream();
        final var dos = new DataOutputStream(baos);
        type.writeTo(dos);

        final byte[] bytes = baos.toByteArray();
        assertEquals(8, bytes.length);
        final var read = FrontendType.readFrom(new DataInputStream(new ByteArrayInputStream(bytes)));
        assertEquals(type, read);
    }

    @Test
    void testCompareTo() {
        assertEquals(0, object().compareTo(equalObject()));
        assertThat(object().compareTo(differentObject())).isLessThan(0);
    }
}
