/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.concepts;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.ExtendedActorSystem;
import akka.serialization.JavaSerializer;
import akka.testkit.TestProbe;
import com.google.common.base.MoreObjects;
import org.apache.commons.lang.SerializationUtils;
import org.junit.Before;
import org.junit.Test;

public abstract class AbstractRequestTest<T extends Request<?, T>> {
    private static final ActorSystem SYSTEM = ActorSystem.create("test");
    protected static final ActorRef ACTOR_REF = TestProbe.apply(SYSTEM).ref();

    protected abstract T object();

    @Before
    public void setUp() {
        JavaSerializer.currentSystem().value_$eq((ExtendedActorSystem) SYSTEM);
    }

    @Test
    public void getReplyToTest() {
        assertEquals(ACTOR_REF, object().getReplyTo());
    }

    @Test
    public void addToStringAttributesCommonTest() {
        final var result = object().addToStringAttributes(MoreObjects.toStringHelper(object()));
        assertThat(result.toString(), containsString("replyTo=" + ACTOR_REF));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void serializationTest() {
        final Object deserialize = SerializationUtils.clone(object());

        assertEquals(object().getTarget(), ((T) deserialize).getTarget());
        assertEquals(object().getVersion(), ((T) deserialize).getVersion());
        assertEquals(object().getSequence(), ((T) deserialize).getSequence());
        doAdditionalAssertions(deserialize);
    }

    protected abstract void doAdditionalAssertions(Object deserialize);
}
