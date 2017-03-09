/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Externalizable;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Constructor;

public class TestUtils {

    private TestUtils() {
        throw new UnsupportedOperationException("This is utility class");
    }

    @SuppressWarnings("unchecked")
    public static <T extends Externalizable> T copy(final T object) throws Exception {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (final ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            object.writeExternal(oos);
        }

        try (final ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bos.toByteArray()))) {
            final Constructor constructor = object.getClass().getConstructor();
            constructor.setAccessible(true);
            final T result = (T) constructor.newInstance();
            result.readExternal(ois);
            return result;
        }
    }

}
