/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.access.concepts;

import org.apache.commons.lang.SerializationUtils;
import org.junit.Assert;
import org.junit.Test;

public class FrontendTypeTest {

    @Test
    public void testSerialization() throws Exception {
        FrontendType type = FrontendType.forName("type");
        final byte[] serializedBytes = SerializationUtils.serialize(type);
        final Object deserialized = SerializationUtils.deserialize(serializedBytes);
        Assert.assertTrue(deserialized instanceof FrontendType);
        Assert.assertEquals(type, deserialized);
    }
}