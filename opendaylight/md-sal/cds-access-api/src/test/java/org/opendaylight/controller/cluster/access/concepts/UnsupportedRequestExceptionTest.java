/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.concepts;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import akka.actor.ActorRef;
import org.opendaylight.controller.cluster.access.commands.CreateLocalHistoryRequest;

public class UnsupportedRequestExceptionTest extends RequestExceptionTest<UnsupportedRequestException> {

    private static final FrontendIdentifier FRONTEND =
            new FrontendIdentifier(MemberName.forName("test"), FrontendIdentifierTest.ONE_FRONTEND_TYPE);
    private static final ClientIdentifier CLIENT = new ClientIdentifier(FRONTEND, 0);

    private static final LocalHistoryIdentifier LOCAL_HISTORY = new LocalHistoryIdentifier(CLIENT, 0);
    private static final Request<?, ?> REQUEST = new CreateLocalHistoryRequest(LOCAL_HISTORY, ActorRef.noSender());
    private static final RequestException OBJECT = new UnsupportedRequestException(REQUEST);

    @Override
    protected void isRetriable() {
        assertFalse(OBJECT.isRetriable());
    }

    @Override
    protected void checkMessage() {
        final String message = OBJECT.getMessage();
        assertTrue("Unsupported request ".equals(message));
        assertNull(OBJECT.getCause());
    }

}
