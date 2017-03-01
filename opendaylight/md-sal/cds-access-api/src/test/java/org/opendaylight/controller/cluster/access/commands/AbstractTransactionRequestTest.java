/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.commands;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import org.junit.Test;
import org.opendaylight.yangtools.concepts.Identifier;

public abstract class AbstractTransactionRequestTest<T extends AbstractLocalTransactionRequest> {
    abstract T object();

    abstract T differentObject();

    abstract T equalObject();

    @Test
    public final void testEquals() {
        assertTrue(object().equals(object()));
        assertTrue(object().equals(equalObject()));
        assertFalse(object().equals(null));
        assertFalse(object().equals("dummy"));
        assertFalse(object().equals(differentObject()));
    }

    @Test
    public final void testHashCode() {
        assertEquals(object().hashCode(), equalObject().hashCode());
    }

    @SuppressWarnings("unchecked")
    private static <T> T copy(T obj) throws IOException, ClassNotFoundException {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(obj);
        }

        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bos.toByteArray()))) {
            return (T) ois.readObject();
        }
    }

    @Test(expected = UnsupportedOperationException.class)
    public final void testSerialization() throws Exception {
        assertTrue(object().equals(copy(object())));
        assertTrue(object().equals(copy(equalObject())));
        assertFalse(differentObject().equals(copy(object())));
    }

    @Test
    public final void testWriteExternal() {}

    @Test
    public final void testReadExternal() {}
}
