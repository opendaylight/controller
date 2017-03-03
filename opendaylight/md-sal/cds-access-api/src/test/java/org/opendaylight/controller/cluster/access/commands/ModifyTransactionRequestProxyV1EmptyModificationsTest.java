/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.commands;

import com.google.common.collect.Lists;
import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.controller.cluster.access.concepts.AbstractRequestProxyTest;
import org.opendaylight.controller.cluster.access.concepts.Request;

public class ModifyTransactionRequestProxyV1EmptyModificationsTest
        extends AbstractRequestProxyTest<ModifyTransactionRequestProxyV1> {

    private static final PersistenceProtocol PROTOCOL = PersistenceProtocol.ABORT;
    private static final ModifyTransactionRequest REQUEST = new ModifyTransactionRequest(
            TRANSACTION_IDENTIFIER, 0, ACTOR_REF, Lists.newArrayList(), PROTOCOL);
    private static final ModifyTransactionRequestProxyV1 OBJECT = new ModifyTransactionRequestProxyV1(REQUEST);

    @Override
    public ModifyTransactionRequestProxyV1 object() {
        return OBJECT;
    }

    @Test
    public void createRequestTest() {
        final Request purgeRequest = object().createRequest(TRANSACTION_IDENTIFIER, 0, ACTOR_REF);
        Assert.assertNotNull(purgeRequest);
    }
}