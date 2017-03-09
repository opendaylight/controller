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

public class ExistsTransactionSuccessProxyV1Test
        extends AbstractTransactionSuccessProxyTest<ExistsTransactionSuccessProxyV1> {
    private static final ExistsTransactionSuccessProxyV1 OBJECT = new ExistsTransactionSuccessProxyV1();

    @Override
    ExistsTransactionSuccessProxyV1 object() {
        return OBJECT;
    }

    @Test
    public void createSuccess() throws Exception {
        final ExistsTransactionSuccess result = OBJECT.createSuccess(TRANSACTION_IDENTIFIER, 0);
        Assert.assertNotNull(result);
    }
}