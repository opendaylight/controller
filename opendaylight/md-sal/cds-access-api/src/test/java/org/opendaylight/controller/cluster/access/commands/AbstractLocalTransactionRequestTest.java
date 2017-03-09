/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.commands;

import org.apache.commons.lang.SerializationUtils;
import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.controller.cluster.access.ABIVersion;

public abstract class AbstractLocalTransactionRequestTest<T extends AbstractLocalTransactionRequest>
        extends AbstractTransactionRequestTest {
    @Override
    protected abstract T object();

    @Test
    public void cloneAsVersionTest() {
        Assert.assertEquals(object(), object().cloneAsVersion(ABIVersion.BORON));
    }

    @Override
    @Test(expected = UnsupportedOperationException.class)
    public void serializationTest() {
        SerializationUtils.clone(object());
    }
}
