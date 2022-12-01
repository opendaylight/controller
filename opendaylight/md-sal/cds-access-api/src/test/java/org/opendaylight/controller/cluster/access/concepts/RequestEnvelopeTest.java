/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.concepts;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.ExtendedActorSystem;
import akka.serialization.JavaSerializer;
import akka.testkit.TestProbe;
import org.junit.After;
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
    protected EnvelopeDetails<RequestEnvelope> createEnvelope() {
        replyToProbe = new TestProbe(system);
        replyTo = replyToProbe.ref();
        final int refSize = replyTo.path().toSerializationFormat().length();

        return new EnvelopeDetails<>(new RequestEnvelope(new TransactionPurgeRequest(OBJECT, 2L, replyTo), 1L, 2L),
            refSize + 179);
    }

    @Override
    protected void doAdditionalAssertions(final RequestEnvelope envelope, final RequestEnvelope resolvedObject) {
        final Request<?, ?> actual = resolvedObject.getMessage();
        assertThat(actual, instanceOf(TransactionPurgeRequest.class));
        final var purgeRequest = (TransactionPurgeRequest) actual;
        assertEquals(replyTo, purgeRequest.getReplyTo());
        final var response = new TransactionPurgeResponse(OBJECT, 2L);
        resolvedObject.sendSuccess(response, 11L);
        final var successEnvelope = replyToProbe.expectMsgClass(SuccessEnvelope.class);
        assertEquals(response, successEnvelope.getMessage());
        final var failResponse = new RuntimeRequestException("fail", new RuntimeException());
        resolvedObject.sendFailure(failResponse, 11L);
        final var failureEnvelope = replyToProbe.expectMsgClass(FailureEnvelope.class);
        assertEquals(failResponse, failureEnvelope.getMessage().getCause());
    }

    @After
    public void tearDown() {
        system.terminate();
    }
}