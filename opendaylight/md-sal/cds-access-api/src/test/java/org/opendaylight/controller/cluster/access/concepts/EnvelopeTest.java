/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.access.concepts;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public abstract class EnvelopeTest<E extends Envelope, P extends AbstractEnvelopeProxy> {
    private static final FrontendIdentifier FRONTEND =
            new FrontendIdentifier(MemberName.forName("test"), FrontendIdentifierTest.ONE_FRONTEND_TYPE);
    private static final ClientIdentifier CLIENT = new ClientIdentifier(FRONTEND, 0);
    private static final LocalHistoryIdentifier HISTORY = new LocalHistoryIdentifier(CLIENT, 0);

    protected static final TransactionIdentifier OBJECT = new TransactionIdentifier(HISTORY, 0);

    private E envelope;
    private P proxy;

    @Before
    public void setUp() throws Exception {
        envelope = createEnvelope();
        proxy = createProxy(envelope);
    }

    @Test
    public void testProxySerializationDeserialization() throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        proxy.writeExternal(oos);
        oos.flush();
        P deserialized = (P) proxy.getClass().newInstance();
        ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bos.toByteArray()));
        deserialized.readExternal(ois);
        final Object o = deserialized.readResolve();
        E deserializedEnvelope = (E) o;

        checkDeserialized(deserialized, deserializedEnvelope);
    }

    private void checkDeserialized(P deserialized, E deserializedEnvelope) {
        Assert.assertEquals(envelope.getSessionId(), deserializedEnvelope.getSessionId());
        Assert.assertEquals(envelope.getTxSequence(), deserializedEnvelope.getTxSequence());
        final Message expectedMessage = envelope.getMessage();
        final Message actualMessage = deserializedEnvelope.getMessage();
        Assert.assertEquals(expectedMessage.getSequence(), actualMessage.getSequence());
        Assert.assertEquals(expectedMessage.getTarget(), actualMessage.getTarget());
        Assert.assertEquals(expectedMessage.getVersion(), actualMessage.getVersion());
        Assert.assertEquals(expectedMessage.getClass(), actualMessage.getClass());
        doAdditionalAssertions(envelope, deserialized, deserializedEnvelope);
    }

    protected abstract E createEnvelope();

    protected abstract P createProxy(E envelope);

    protected abstract void doAdditionalAssertions(E envelope, P deserializedProxy, E resolvedObject);
}
