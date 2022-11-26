/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.persisted;

import static java.util.Objects.requireNonNull;
import static org.junit.Assert.assertEquals;

import org.apache.commons.lang3.SerializationUtils;
import org.junit.Test;
import org.opendaylight.controller.cluster.datastore.AbstractTest;

abstract class AbstractIdentifiablePayloadTest<T extends AbstractIdentifiablePayload<?>> extends AbstractTest {
    private final T object;
    private final int expectedSize;

    AbstractIdentifiablePayloadTest(final T object, final int expectedSize) {
        this.object = requireNonNull(object);
        this.expectedSize = expectedSize;
    }

    @Test
    public void testSerialization() {
        final byte[] bytes = SerializationUtils.serialize(object);
        assertEquals(expectedSize, bytes.length);
        final T cloned = SerializationUtils.deserialize(bytes);
        assertEquals(object.getIdentifier(), cloned.getIdentifier());
    }
}
