/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore;

import akka.actor.ActorSystem;
import akka.testkit.JavaTestKit;
import java.io.IOException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.opendaylight.controller.cluster.access.concepts.ClientIdentifier;
import org.opendaylight.controller.cluster.access.concepts.FrontendIdentifier;
import org.opendaylight.controller.cluster.access.concepts.FrontendType;
import org.opendaylight.controller.cluster.access.concepts.LocalHistoryIdentifier;
import org.opendaylight.controller.cluster.access.concepts.MemberName;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;

public abstract class AbstractActorTest {
    protected static final MemberName MEMBER_NAME = MemberName.forName("member-1");
    protected static final FrontendIdentifier<?> FRONTEND_ID = FrontendIdentifier.create(MEMBER_NAME, new FrontendType() {
        private static final long serialVersionUID = 1L;

        @Override
        public String toSimpleString() {
            return ShardTransactionTest.class.getSimpleName();
        }

        private Object readResolve() {
            return FRONTEND_ID;
        }
    });

    private static ActorSystem system;

    private final ClientIdentifier<?> clientId = ClientIdentifier.create(FRONTEND_ID, 0);
    private final LocalHistoryIdentifier<?> historyId = new LocalHistoryIdentifier<>(clientId, 0);
    private int txCounter = 0;

    @BeforeClass
    public static void setUpClass() throws IOException {

        System.setProperty("shard.persistent", "false");
        system = ActorSystem.create("test");
    }

    @AfterClass
    public static void tearDownClass() throws IOException {
        JavaTestKit.shutdownActorSystem(system);
        system = null;
    }

    protected static ActorSystem getSystem() {
        return system;
    }

    protected final TransactionIdentifier<?> nextTransactionId() {
        return new TransactionIdentifier<>(historyId, txCounter++);
    }
}
