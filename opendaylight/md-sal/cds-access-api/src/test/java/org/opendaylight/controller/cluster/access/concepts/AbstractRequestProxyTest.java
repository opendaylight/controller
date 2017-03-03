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
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Constructor;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public abstract class AbstractRequestProxyTest<T extends AbstractRequestProxy> {
    public abstract T object();

    protected static final FrontendIdentifier FRONTEND_IDENTIFIER = FrontendIdentifier.create(
            MemberName.forName("test"), FrontendType.forName("one"));
    protected static final ClientIdentifier CLIENT_IDENTIFIER = ClientIdentifier.create(FRONTEND_IDENTIFIER, 0);
    protected static final LocalHistoryIdentifier HISTORY_IDENTIFIER = new LocalHistoryIdentifier(CLIENT_IDENTIFIER, 0);
    protected static final TransactionIdentifier TRANSACTION_IDENTIFIER = new TransactionIdentifier(HISTORY_IDENTIFIER, 0);
    protected static final ActorSystem SYSTEM = ActorSystem.create("test");
    protected static final ActorRef ACTOR_REF = TestProbe.apply(SYSTEM).ref();

    @Before
    public void setUp() {
        JavaSerializer.currentSystem().value_$eq((ExtendedActorSystem) SYSTEM);
    }

    @Test
    public void externalizableTest() throws Exception {
        final T copy = copy();
        Assert.assertTrue(copy != null);
    }

    @SuppressWarnings("unchecked")
    private T copy() throws Exception {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            object().writeExternal(oos);
        }

        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bos.toByteArray()))) {
            final Constructor constructor = object().getClass().getConstructor();
            constructor.setAccessible(true);
            final T result = (T) constructor.newInstance();
            result.readExternal(ois);
            return result;
        }
    }

    @Test
    public void readTargetTest() throws Exception {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            object().writeExternal(oos);
        }

        final ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bos.toByteArray()));
        Assert.assertNotNull(object().readTarget(ois));
    }
}