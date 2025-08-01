/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.concepts;

import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apache.commons.lang3.SerializationUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

abstract class AbstractEnvelopeTest<E extends Envelope<?>> {
    record EnvelopeDetails<E extends Envelope<?>>(E envelope, int expectedSize) {
        // Nothing else
    }

    private static final FrontendIdentifier FRONTEND =
            new FrontendIdentifier(MemberName.forName("test"), FrontendIdentifierTest.ONE_FRONTEND_TYPE);
    private static final ClientIdentifier CLIENT = new ClientIdentifier(FRONTEND, 0);
    private static final LocalHistoryIdentifier HISTORY = new LocalHistoryIdentifier(CLIENT, 0);

    static final TransactionIdentifier OBJECT = new TransactionIdentifier(HISTORY, 0);

    private E envelope;
    private int expectedSize;

    @BeforeEach
    void beforeEach() throws Exception {
        final var details = createEnvelope();
        envelope = requireNonNull(details.envelope);
        expectedSize = details.expectedSize;
    }

    @Test
    void testProxySerializationDeserialization() {
        final byte[] serializedBytes = SerializationUtils.serialize(envelope);
        assertEquals(expectedSize, serializedBytes.length);
        @SuppressWarnings("unchecked")
        final E deserialize = (E) SerializationUtils.deserialize(serializedBytes);
        checkDeserialized(deserialize);
    }

    private void checkDeserialized(final E deserializedEnvelope) {
        assertEquals(envelope.getSessionId(), deserializedEnvelope.getSessionId());
        assertEquals(envelope.getTxSequence(), deserializedEnvelope.getTxSequence());
        final var expectedMessage = envelope.getMessage();
        final var actualMessage = deserializedEnvelope.getMessage();
        assertEquals(expectedMessage.getSequence(), actualMessage.getSequence());
        assertEquals(expectedMessage.getTarget(), actualMessage.getTarget());
        assertEquals(expectedMessage.getVersion(), actualMessage.getVersion());
        assertEquals(expectedMessage.getClass(), actualMessage.getClass());
        doAdditionalAssertions(envelope, deserializedEnvelope);
    }

    abstract EnvelopeDetails<E> createEnvelope();

    abstract void doAdditionalAssertions(E envelope, E resolvedObject);
}
