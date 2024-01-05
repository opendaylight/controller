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
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.akka.queue.Envelope;

public abstract class AbstractEnvelopeTest<E extends Envelope<?>> {
    protected record EnvelopeDetails<E extends Envelope<?>>(E envelope, int expectedSize) {
        // Nothing else
    }

    private static final FrontendIdentifier FRONTEND =
            new FrontendIdentifier(MemberName.forName("test"), FrontendIdentifierTest.ONE_FRONTEND_TYPE);
    private static final ClientIdentifier CLIENT = new ClientIdentifier(FRONTEND, 0);
    private static final LocalHistoryIdentifier HISTORY = new LocalHistoryIdentifier(CLIENT, 0);
    protected static final TransactionIdentifier OBJECT = new TransactionIdentifier(HISTORY, 0);

    private E envelope;
    private int expectedSize;

    @Before
    public void setUp() throws Exception {
        final var details = createEnvelope();
        envelope = requireNonNull(details.envelope);
        expectedSize = details.expectedSize;
    }

    @Test
    public void testProxySerializationDeserialization() {
        final byte[] serializedBytes = SerializationUtils.serialize(envelope);
        assertEquals(expectedSize, serializedBytes.length);
        @SuppressWarnings("unchecked")
        final E deserialize = (E) SerializationUtils.deserialize(serializedBytes);
        checkDeserialized(deserialize);
    }

    private void checkDeserialized(final E deserializedEnvelope) {
        assertEquals(envelope.sessionId(), deserializedEnvelope.sessionId());
        assertEquals(envelope.txSequence(), deserializedEnvelope.txSequence());
        final var expectedMessage = envelope.message();
        final var actualMessage = deserializedEnvelope.message();
        assertEquals(expectedMessage.sequence(), actualMessage.sequence());
        assertEquals(expectedMessage.target(), actualMessage.target());
        assertEquals(expectedMessage.version(), actualMessage.version());
        assertEquals(expectedMessage.getClass(), actualMessage.getClass());
        doAdditionalAssertions(envelope, deserializedEnvelope);
    }

    protected abstract EnvelopeDetails<E> createEnvelope();

    protected abstract void doAdditionalAssertions(E envelope, E resolvedObject);
}
