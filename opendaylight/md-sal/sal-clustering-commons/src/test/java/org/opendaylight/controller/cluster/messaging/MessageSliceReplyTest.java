/*
 * Copyright (c) 2017 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.messaging;

import static org.junit.Assert.assertEquals;

import akka.actor.ActorSystem;
import akka.actor.ExtendedActorSystem;
import akka.serialization.JavaSerializer;
import akka.testkit.JavaTestKit;
import akka.testkit.TestProbe;
import org.apache.commons.lang.SerializationUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for MessageSliceReply.
 *
 * @author Thomas Pantelis
 */
public class MessageSliceReplyTest {
    private final ActorSystem actorSystem = ActorSystem.create("test");

    @Before
    public void setUp() {
        JavaSerializer.currentSystem().value_$eq((ExtendedActorSystem) actorSystem);
    }

    @After
    public void tearDown() {
        JavaTestKit.shutdownActorSystem(actorSystem, Boolean.TRUE);
    }

    @Test
    public void testSerialization() {
        MessageSliceReply expected = new MessageSliceReply(new StringIdentifier("test"), 3, true,
                TestProbe.apply(actorSystem).ref());
        MessageSliceReply cloned = (MessageSliceReply) SerializationUtils.clone(expected);

        assertEquals("getIdentifier", expected.getIdentifier(), cloned.getIdentifier());
        assertEquals("getSliceIndex", expected.getSliceIndex(), cloned.getSliceIndex());
        assertEquals("isSuccess", expected.isSuccess(), cloned.isSuccess());
        assertEquals("getSendTo", expected.getSendTo(), cloned.getSendTo());
    }
}
