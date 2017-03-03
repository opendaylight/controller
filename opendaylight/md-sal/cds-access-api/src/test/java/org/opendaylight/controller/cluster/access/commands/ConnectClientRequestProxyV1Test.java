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
import org.opendaylight.controller.cluster.access.concepts.AbstractRequestProxyTest;
import org.opendaylight.controller.cluster.access.concepts.Request;

public class ConnectClientRequestProxyV1Test extends AbstractRequestProxyTest<ConnectClientRequestProxyV1> {

    private static final ConnectClientRequest REQUEST = new ConnectClientRequest(
            CLIENT_IDENTIFIER, ACTOR_REF, ABIVersion.TEST_PAST_VERSION, ABIVersion.TEST_FUTURE_VERSION);
    private static final ConnectClientRequestProxyV1 OBJECT = new ConnectClientRequestProxyV1(REQUEST);

    @Override
    public ConnectClientRequestProxyV1 object() {
        return OBJECT;
    }

    @Test
    public void createRequestTest() {
<<<<<<< 9bfa0a2b934b9152e47ab7f715c8cd496e266fc1
        final Request request = object().createRequest(CLIENT_IDENTIFIER, 0, ACTOR_REF);
        Assert.assertNotNull(request);
=======
        final Request purgeRequest = object().createRequest(CLIENT_IDENTIFIER, 0, ACTOR_REF);
        Assert.assertNotNull(purgeRequest);
>>>>>>> Unit tests for AbstractRequestProxy derived classes
    }
}