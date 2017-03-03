/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.commands;

import org.junit.Test;
import org.opendaylight.controller.cluster.access.concepts.AbstractRequestProxyTest;

public class TransactionAbortRequestProxyV1Test extends AbstractRequestProxyTest<TransactionAbortRequestProxyV1> {
    private static final TransactionAbortRequestProxyV1 OBJECT = null;

    @Override
    public TransactionAbortRequestProxyV1 object() {
        return OBJECT;
    }

    @Test
    public void createRequest() throws Exception {

    }
}