/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.access.commands;

import static org.junit.Assert.assertTrue;

import akka.actor.ActorSystem;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.controller.cluster.access.concepts.LocalHistoryIdentifier;
import org.opendaylight.controller.cluster.access.concepts.ClientIdentifier;
import org.opendaylight.controller.cluster.access.concepts.FrontendIdentifier;
import org.opendaylight.controller.cluster.access.concepts.MemberName;
import org.opendaylight.controller.cluster.access.concepts.FrontendType;

import akka.actor.ActorRef;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.common.QName;

import org.junit.Test;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.AfterClass;
//import org.mockito.Mock;
//import org.mockito.MockitoAnnotations;

import akka.testkit.JavaTestKit;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class ExistsTransactionRequestTest {

    private static final QName FAMILY_QNAME =
            QName
                    .create(
                            "urn:opendaylight:params:xml:ns:yang:controller:md:sal:dom:store:notification-test",
                            "2014-04-17", "family");

    private YangInstanceIdentifier yangInstanceIdentifier = YangInstanceIdentifier
            .of(FAMILY_QNAME);
    //private YangInstanceIdentifier yangInstanceIdentifier = YangInstanceIdentifier.EMPTY;

    private static final FrontendIdentifier IFRONTEND =
            FrontendIdentifier.create(MemberName.forName("test"), FrontendType.forName("test_type"));
    private static final ClientIdentifier ICLIENT = ClientIdentifier.create(IFRONTEND, 0);
    private static final LocalHistoryIdentifier IHISTORY = new LocalHistoryIdentifier(ICLIENT, 0);
    private static final TransactionIdentifier ITRANSACTION = new TransactionIdentifier(IHISTORY, 0);


    private static ActorSystem system;
    private JavaTestKit kit;
    private ActorRef actorRef;
    //= public JavaTestKit(ActorSystem var1) {
      //  this.p = new TestProbe(var1);
    //}

    //public ActorRef getTestActor() {
      //  return this.p.testActor();
    //}

    @BeforeClass
    public static void setup() {
        system = ActorSystem.create();
    }

    @AfterClass
    public static void teardown() {
        JavaTestKit.shutdownActorSystem(system);
        system = null;
    }

    private ExistsTransactionRequest OBJECT;

    @Before
    public void setUp() {
        //MockitoAnnotations.initMocks(this);
        kit = new JavaTestKit(system);
        actorRef = kit.getTestActor();
        OBJECT = new ExistsTransactionRequest(ITRANSACTION, 0, actorRef, yangInstanceIdentifier);
    }

    private static <T> T copy(T o) throws IOException, ClassNotFoundException {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(o);
        }

        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bos.toByteArray()))) {
            return (T) ois.readObject();
        }
    }

    @Test
    public final void testSerialization() throws Exception {
        assertTrue(OBJECT.equals(copy(OBJECT)));
    }

}
