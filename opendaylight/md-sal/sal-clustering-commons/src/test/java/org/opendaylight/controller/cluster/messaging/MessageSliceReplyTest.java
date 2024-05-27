/*
 * Copyright (c) 2017 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.messaging;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.actor.ExtendedActorSystem;
import org.apache.pekko.serialization.JavaSerializer;
import org.apache.pekko.testkit.TestProbe;
import org.apache.pekko.testkit.javadsl.TestKit;
import org.apache.commons.lang3.SerializationUtils;
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
        TestKit.shutdownActorSystem(actorSystem, true);
    }

    @Test
    public void testSerialization() {
        testSuccess();
        testFailure();
    }

    private void testSuccess() {
        MessageSliceReply expected = MessageSliceReply.success(new StringIdentifier("test"), 3,
                TestProbe.apply(actorSystem).ref());
        MessageSliceReply cloned = SerializationUtils.clone(expected);

        assertEquals("getIdentifier", expected.getIdentifier(), cloned.getIdentifier());
        assertEquals("getSliceIndex", expected.getSliceIndex(), cloned.getSliceIndex());
        assertEquals("getSendTo", expected.getSendTo(), cloned.getSendTo());
        assertFalse("getFailure present", cloned.getFailure().isPresent());
    }

    private void testFailure() {
        MessageSliceReply expected = MessageSliceReply.failed(new StringIdentifier("test"),
                new MessageSliceException("mock", true), TestProbe.apply(actorSystem).ref());
        MessageSliceReply cloned = SerializationUtils.clone(expected);

        assertEquals("getIdentifier", expected.getIdentifier(), cloned.getIdentifier());
        assertEquals("getSliceIndex", expected.getSliceIndex(), cloned.getSliceIndex());
        assertEquals("getSendTo", expected.getSendTo(), cloned.getSendTo());
        assertTrue("getFailure present", cloned.getFailure().isPresent());
        assertEquals("getFailure message", expected.getFailure().orElseThrow().getMessage(),
                cloned.getFailure().orElseThrow().getMessage());
        assertEquals("getFailure isRetriable", expected.getFailure().orElseThrow().isRetriable(),
                cloned.getFailure().orElseThrow().isRetriable());
    }
}
