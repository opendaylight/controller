/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.access.commands;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import akka.actor.ActorRef;
import org.junit.Assert;
import org.opendaylight.controller.cluster.access.concepts.RequestException;
import org.opendaylight.controller.cluster.access.concepts.RequestExceptionTest;

public class NotLeaderExceptionTest extends RequestExceptionTest<NotLeaderException> {

    private static final NotLeaderException OBJECT = new NotLeaderException(ActorRef.noSender());
    private static final NotLeaderException EQUAL_OBJECT = new NotLeaderException(ActorRef.noSender());
    private static final NotLeaderException DIFF_OBJECT = new NotLeaderException(ActorRef.noSender());

    @Override
    protected NotLeaderException object() {
        return OBJECT;
    }

    @Override
    protected NotLeaderException differentObject() {
        return DIFF_OBJECT;
    }

    @Override
    protected NotLeaderException equalObject() {
        return EQUAL_OBJECT;
    }

    @Override
    protected void isRetriable() {
        Assert.assertFalse(OBJECT.isRetriable());
    }

    @Override
    protected void checkMessage() {
        final String message = OBJECT.getMessage();
        assertTrue("Actor null is not the current leader".equals(message));
        assertNull(OBJECT.getCause());
    }

    @Override
    protected RequestException checkFromRealSituation() {
        return null;
    }
}
