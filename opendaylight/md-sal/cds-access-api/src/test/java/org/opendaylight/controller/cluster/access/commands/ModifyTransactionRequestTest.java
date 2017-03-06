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
import org.opendaylight.controller.cluster.access.ABIVersion;

//FIXME add modifications
public class ModifyTransactionRequestTest extends AbstractTransactionRequestTest<ModifyTransactionRequest> {
    private static final ModifyTransactionRequest OBJECT = new ModifyTransactionRequest(
            TRANSACTION_IDENTIFIER, 0, ACTOR_REF, Lists.newArrayList(), PersistenceProtocol.ABORT);

    @Override
    ModifyTransactionRequest object() {
        return OBJECT;
    }

    @Test
    public void getPersistenceProtocol() throws Exception {

    }

    @Test
    public void getModifications() throws Exception {

    }

    @Test
    public void addToStringAttributes() throws Exception {

    }

    @Test
    public void externalizableProxy() throws Exception {
        final ModifyTransactionRequestProxyV1 proxy = OBJECT.externalizableProxy(ABIVersion.BORON);
        Assert.assertNotNull(proxy);
    }

    @Test
    public void cloneAsVersion() throws Exception {
        final ModifyTransactionRequest clone = OBJECT.cloneAsVersion(ABIVersion.BORON);
        Assert.assertEquals(OBJECT, clone);
    }
}