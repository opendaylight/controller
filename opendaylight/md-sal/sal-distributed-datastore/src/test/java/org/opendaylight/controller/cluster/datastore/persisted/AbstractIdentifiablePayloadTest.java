/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.persisted;

import org.apache.commons.lang3.SerializationUtils;
import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.controller.cluster.datastore.AbstractTest;

public abstract class AbstractIdentifiablePayloadTest<T extends AbstractIdentifiablePayload> extends AbstractTest {

    abstract T object();

    @Test
    public void testSerialization() {
        final T object = object();
        final T cloned = SerializationUtils.clone(object);
        Assert.assertEquals(object.getIdentifier(), cloned.getIdentifier());
    }
}
