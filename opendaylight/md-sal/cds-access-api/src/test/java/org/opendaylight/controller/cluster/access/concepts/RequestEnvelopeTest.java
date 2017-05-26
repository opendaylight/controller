/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.concepts;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.ExtendedActorSystem;
import akka.serialization.JavaSerializer;
import akka.testkit.TestProbe;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.opendaylight.controller.cluster.access.commands.TransactionPurgeRequest;
import org.opendaylight.controller.cluster.access.commands.TransactionPurgeResponse;

public class RequestEnvelopeTest extends AbstractEnvelopeTest<RequestEnvelope> {

    private ActorSystem system;
    private ActorRef replyTo;
    private TestProbe replyToProbe;

    @Override
    @Before
    public void setUp() throws Exception {
        system = ActorSystem.apply();
        JavaSerializer.currentSystem().value_$eq((ExtendedActorSystem) system);
        super.setUp();
    }

    @Override
    protected RequestEnvelope createEnvelope() {
        replyToProbe = new TestProbe(system);
        replyTo = replyToProbe.ref();
        final TransactionPurgeRequest message = new TransactionPurgeRequest(OBJECT, 2L, replyTo);
        return new RequestEnvelope(message, 1L, 2L);
    }

    @Override
    protected void doAdditionalAssertions(final RequestEnvelope envelope, final RequestEnvelope resolvedObject) {
        final Request<?, ?> actual = resolvedObject.getMessage();
        Assert.assertTrue(actual instanceof TransactionPurgeRequest);
        final TransactionPurgeRequest purgeRequest = (TransactionPurgeRequest) actual;
        Assert.assertEquals(replyTo, purgeRequest.getReplyTo());
        final TransactionPurgeResponse response = new TransactionPurgeResponse(OBJECT, 2L);
        resolvedObject.sendSuccess(response, 11L);
        final SuccessEnvelope successEnvelope = replyToProbe.expectMsgClass(SuccessEnvelope.class);
        Assert.assertEquals(response, successEnvelope.getMessage());
        final RuntimeRequestException failResponse = new RuntimeRequestException("fail", new RuntimeException());
        resolvedObject.sendFailure(failResponse, 11L);
        final FailureEnvelope failureEnvelope = replyToProbe.expectMsgClass(FailureEnvelope.class);
        Assert.assertEquals(failResponse, failureEnvelope.getMessage().getCause());
    }

    @After
    public void tearDown() throws Exception {
        system.terminate();
    }
}