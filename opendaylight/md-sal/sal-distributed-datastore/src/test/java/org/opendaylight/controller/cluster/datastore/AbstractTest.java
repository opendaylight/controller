/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import akka.actor.ActorSystem;
import akka.testkit.JavaTestKit;
import com.typesafe.config.ConfigFactory;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.After;
import org.opendaylight.controller.cluster.access.concepts.ClientIdentifier;
import org.opendaylight.controller.cluster.access.concepts.FrontendIdentifier;
import org.opendaylight.controller.cluster.access.concepts.FrontendType;
import org.opendaylight.controller.cluster.access.concepts.LocalHistoryIdentifier;
import org.opendaylight.controller.cluster.access.concepts.MemberName;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import scala.compat.java8.FutureConverters;
import scala.concurrent.Await;
import scala.concurrent.duration.Duration;

public abstract class AbstractTest {
    protected static final MemberName MEMBER_NAME = MemberName.forName("member-1");
    protected static final MemberName MEMBER_2_NAME = MemberName.forName("member-2");

    private static final FrontendType FRONTEND_TYPE = FrontendType.forName(ShardTransactionTest.class.getSimpleName());

    protected static final FrontendIdentifier FRONTEND_ID = FrontendIdentifier.create(MEMBER_NAME, FRONTEND_TYPE);

    private static final ClientIdentifier CLIENT_ID = ClientIdentifier.create(FRONTEND_ID, 0);
    private static final LocalHistoryIdentifier HISTORY_ID = new LocalHistoryIdentifier(CLIENT_ID, 0);
    private static final AtomicLong HISTORY_COUNTER = new AtomicLong();
    private static final AtomicLong TX_COUNTER = new AtomicLong();

    private final Collection<ActorSystem> actorSystems = new ArrayList<>();

    protected static void setUpStatic() {
        HISTORY_COUNTER.set(1L);
        TX_COUNTER.set(1L);
    }

    protected static TransactionIdentifier nextTransactionId() {
        return new TransactionIdentifier(HISTORY_ID, TX_COUNTER.getAndIncrement());
    }

    protected static LocalHistoryIdentifier nextHistoryId() {
        return new LocalHistoryIdentifier(CLIENT_ID, HISTORY_COUNTER.incrementAndGet());
    }

    protected static <T> T waitOnAsyncTask(final CompletionStage<T> completionStage, final Duration timeout)
            throws Exception {
        return Await.result(FutureConverters.toScala(completionStage), timeout);
    }

    @After
    public void actorSystemCleanup() {
        for (final ActorSystem system : actorSystems) {
            JavaTestKit.shutdownActorSystem(system, null, Boolean.TRUE);
        }
    }

    protected ActorSystem newActorSystem(String name, String config) {
        ActorSystem system = ActorSystem.create(name, ConfigFactory.load().getConfig(config));
        actorSystems.add(system);
        return system;
    }
}
