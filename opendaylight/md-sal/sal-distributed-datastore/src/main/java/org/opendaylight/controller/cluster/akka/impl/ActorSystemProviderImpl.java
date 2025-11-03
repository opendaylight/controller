/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.akka.impl;

import com.typesafe.config.Config;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.actor.Props;
import org.apache.pekko.actor.Terminated;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.controller.cluster.ActorSystemProvider;
import org.opendaylight.controller.cluster.ActorSystemProviderListener;
import org.opendaylight.controller.cluster.common.actor.QuarantinedMonitorActor;
import org.opendaylight.controller.cluster.datastore.TerminationMonitor;
import org.opendaylight.yangtools.concepts.ObjectRegistration;
import org.opendaylight.yangtools.util.ObjectRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.FiniteDuration;

public class ActorSystemProviderImpl implements ActorSystemProvider, AutoCloseable {
    private static final String ACTOR_SYSTEM_NAME = "opendaylight-cluster-data";
    private static final Logger LOG = LoggerFactory.getLogger(ActorSystemProviderImpl.class);

    private final @NonNull ActorSystem actorSystem;
    private final ObjectRegistry<ActorSystemProviderListener> listeners =
        ObjectRegistry.createConcurrent("ActorSystemProvider listeners");

    public ActorSystemProviderImpl(
            final ClassLoader classLoader, final Props quarantinedMonitorActorProps, final Config akkaConfig) {
        LOG.info("Creating new ActorSystem");

        actorSystem = ActorSystem.create(ACTOR_SYSTEM_NAME, akkaConfig, classLoader);
        actorSystem.actorOf(Props.create(TerminationMonitor.class), TerminationMonitor.ADDRESS);
        actorSystem.actorOf(quarantinedMonitorActorProps, QuarantinedMonitorActor.ADDRESS);
    }

    @Override
    public ActorSystem getActorSystem() {
        return actorSystem;
    }

    @Override
    public ObjectRegistration<ActorSystemProviderListener> registerActorSystemProviderListener(
            final ActorSystemProviderListener listener) {
        return listeners.register(listener);
    }

    public Future<Terminated> asyncClose() {
        LOG.info("Shutting down ActorSystem");

        actorSystem.getWhenTerminated().whenComplete((success, failure) -> {
            if (failure != null) {
                LOG.warn("ActorSystem failed to shut down", failure);
            } else {
                LOG.info("ActorSystem shut down");
            }
        });

        return actorSystem.terminate();
    }

    public void close(final FiniteDuration wait) throws TimeoutException, InterruptedException {
        Await.result(asyncClose(), wait);
    }

    @Override
    public void close() throws TimeoutException, InterruptedException {
        close(FiniteDuration.create(10, TimeUnit.SECONDS));
    }
}
