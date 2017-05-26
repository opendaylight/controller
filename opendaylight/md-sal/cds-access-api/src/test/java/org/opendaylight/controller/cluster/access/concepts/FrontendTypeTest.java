/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.concepts;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import org.junit.Assert;
import org.junit.Test;

public class FrontendTypeTest extends AbstractIdentifierTest<FrontendType> {

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

    @Test
    public void testWriteToReadFrom() throws Exception {
        final FrontendType type = FrontendType.forName("type");
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final DataOutputStream dos = new DataOutputStream(baos);
        type.writeTo(dos);
        final FrontendType read =
                FrontendType.readFrom(new DataInputStream(new ByteArrayInputStream(baos.toByteArray())));
        Assert.assertEquals(type, read);
    }

    @Test
    public void testCompareTo() throws Exception {
        Assert.assertTrue(object().compareTo(equalObject()) == 0);
        Assert.assertTrue(object().compareTo(differentObject()) < 0);
    }
}