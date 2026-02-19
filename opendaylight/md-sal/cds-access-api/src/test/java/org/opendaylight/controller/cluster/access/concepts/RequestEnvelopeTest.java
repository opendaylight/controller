/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.concepts;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.actor.ExtendedActorSystem;
import org.apache.pekko.serialization.JavaSerializer;
import org.apache.pekko.testkit.TestProbe;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.opendaylight.controller.cluster.access.commands.TransactionPurgeRequest;
import org.opendaylight.controller.cluster.access.commands.TransactionPurgeResponse;

class RequestEnvelopeTest extends AbstractEnvelopeTest<RequestEnvelope> {
    private ActorSystem system;
    private ActorRef replyTo;
    private TestProbe replyToProbe;

    @Override
    @BeforeEach
    void beforeEach() throws Exception {
        system = ActorSystem.apply();
        JavaSerializer.currentSystem().value_$eq((ExtendedActorSystem) system);
        super.beforeEach();
    }


    @AfterEach
    void afterEach() {
        system.terminate();
    }

    @Override
    EnvelopeDetails<RequestEnvelope> createEnvelope() {
        replyToProbe = new TestProbe(system);
        replyTo = replyToProbe.ref();
        final int refSize = replyTo.path().toSerializationFormat().length();

        return new EnvelopeDetails<>(new RequestEnvelope(new TransactionPurgeRequest(OBJECT, 2L, replyTo), 1L, 2L),
            refSize + 179);
    }

    @Override
    void doAdditionalAssertions(final RequestEnvelope envelope, final RequestEnvelope resolvedObject) {
        final var purgeRequest = assertInstanceOf(TransactionPurgeRequest.class, resolvedObject.getMessage());
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
}
