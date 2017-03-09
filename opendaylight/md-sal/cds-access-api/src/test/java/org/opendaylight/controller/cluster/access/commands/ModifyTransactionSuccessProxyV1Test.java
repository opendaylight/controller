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

public class ModifyTransactionSuccessProxyV1Test
        extends AbstractTransactionSuccessProxyTest<ModifyTransactionSuccessProxyV1> {
    private static final ModifyTransactionSuccess REQUEST = new ModifyTransactionSuccess(TRANSACTION_IDENTIFIER, 0);
    private static final ModifyTransactionSuccessProxyV1 OBJECT = new ModifyTransactionSuccessProxyV1(REQUEST);

    @Override
    ModifyTransactionSuccessProxyV1 object() {
        return OBJECT;
    }

    @Test
    public void createSuccess() throws Exception {
        final ModifyTransactionSuccess result = OBJECT.createSuccess(TRANSACTION_IDENTIFIER, 0);
        Assert.assertNotNull(result);
    }
}