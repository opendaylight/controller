/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.commands;

import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.controller.cluster.access.ABIVersion;

public class ModifyTransactionSuccessTest extends AbstractTransactionSuccessTest<ModifyTransactionSuccess> {
    private static final ModifyTransactionSuccess OBJECT = new ModifyTransactionSuccess(
            TRANSACTION_IDENTIFIER, 0);

    @Test
    public void externalizableProxy() throws Exception {
        final AbstractTransactionSuccessProxy<ModifyTransactionSuccess> proxy = OBJECT.externalizableProxy(
                ABIVersion.BORON);
        Assert.assertNotNull(proxy);
    }

    @Test
    public void cloneAsVersion() throws Exception {
        final ModifyTransactionSuccess clone = OBJECT.cloneAsVersion(ABIVersion.BORON);
        Assert.assertEquals(OBJECT, clone);
    }
}