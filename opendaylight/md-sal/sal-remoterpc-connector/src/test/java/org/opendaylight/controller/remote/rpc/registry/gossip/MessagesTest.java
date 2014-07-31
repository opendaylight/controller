/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.remote.rpc.registry.gossip;

import akka.actor.ActorSystem;
import akka.actor.Address;
import akka.testkit.TestProbe;
import com.typesafe.config.ConfigFactory;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Map;
public class MessagesTest {

    @Test
    public void testSerialization() throws Exception {

        ActorSystem system = ActorSystem.create("opendaylight-rpc",
                ConfigFactory.load().getConfig("unit-test"));

        TestProbe mockActor = new TestProbe(system);

        Address mockAddress= mockActor.ref().path().address();
        Map<Address, Long> versions = new HashMap<>();
        versions.put(mockAddress, System.currentTimeMillis());

        System.out.println("Before serialization" + versions);

        Messages.GossiperMessages.GossipStatus status = new Messages.GossiperMessages.GossipStatus(mockAddress, versions);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(bos);
        out.writeObject(status);

        ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
        ObjectInputStream in = new ObjectInputStream(bis);
        Messages.GossiperMessages.GossipStatus copied = ( Messages.GossiperMessages.GossipStatus ) in.readObject();

        System.out.println("After serialization" + copied.getVersions());


        system.shutdown();
    }

}