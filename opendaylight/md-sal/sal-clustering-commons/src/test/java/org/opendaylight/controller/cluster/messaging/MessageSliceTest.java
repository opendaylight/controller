/*
 * Copyright (c) 2017 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.messaging;

import static org.junit.Assert.assertArrayEquals;
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
 * Unit tests for MessageSlice.
 *
 * @author Thomas Pantelis
 */
public class MessageSliceTest {
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
        byte[] data = new byte[1000];
        for (int i = 0, j = 0; i < data.length; i++) {
            data[i] = (byte)j;
            if (++j >= 255) {
                j = 0;
            }
        }

        MessageSlice expected = new MessageSlice(new StringIdentifier("test"), data, 2, 3, 54321,
                TestProbe.apply(actorSystem).ref());
        MessageSlice cloned = (MessageSlice) SerializationUtils.clone(expected);

        assertEquals("getIdentifier", expected.getIdentifier(), cloned.getIdentifier());
        assertEquals("getSliceIndex", expected.getSliceIndex(), cloned.getSliceIndex());
        assertEquals("getTotalSlices", expected.getTotalSlices(), cloned.getTotalSlices());
        assertEquals("getLastSliceHashCode", expected.getLastSliceHashCode(), cloned.getLastSliceHashCode());
        assertArrayEquals("getData", expected.getData(), cloned.getData());
        assertEquals("getReplyTo", expected.getReplyTo(), cloned.getReplyTo());
    }
}
