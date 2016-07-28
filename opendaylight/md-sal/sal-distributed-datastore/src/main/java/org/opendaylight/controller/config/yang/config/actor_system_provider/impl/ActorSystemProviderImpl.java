/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yang.config.actor_system_provider.impl;

import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.japi.Effect;
import akka.osgi.BundleDelegatingClassLoader;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import java.util.concurrent.TimeUnit;
import org.opendaylight.controller.cluster.ActorSystemProvider;
import org.opendaylight.controller.cluster.ActorSystemProviderListener;
import org.opendaylight.controller.cluster.common.actor.AkkaConfigurationReader;
import org.opendaylight.controller.cluster.common.actor.FileAkkaConfigurationReader;
import org.opendaylight.controller.cluster.common.actor.QuarantinedMonitorActor;
import org.opendaylight.controller.cluster.datastore.TerminationMonitor;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.util.ListenerRegistry;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.Await;
import scala.concurrent.duration.Duration;

public class ActorSystemProviderImpl implements ActorSystemProvider, AutoCloseable {
    private static final String ACTOR_SYSTEM_NAME = "opendaylight-cluster-data";
    private static final String CONFIGURATION_NAME = "odl-cluster-data";
    static final Logger LOG = LoggerFactory.getLogger(ActorSystemProviderImpl.class);
    private final ActorSystem actorSystem;
    private final ListenerRegistry<ActorSystemProviderListener> listeners = new ListenerRegistry<>();

    public ActorSystemProviderImpl(final BundleContext bundleContext) {
        LOG.info("Creating new ActorSystem");

        final Bundle bundle = bundleContext.getBundle();

        final BundleDelegatingClassLoader classLoader = new BundleDelegatingClassLoader(bundle, Thread.currentThread()
                .getContextClassLoader());

        final AkkaConfigurationReader configurationReader = new FileAkkaConfigurationReader();
        final Config akkaConfig = ConfigFactory.load(configurationReader.read()).getConfig(CONFIGURATION_NAME);

        actorSystem = ActorSystem.create(ACTOR_SYSTEM_NAME, akkaConfig, classLoader);

        actorSystem.actorOf(Props.create(TerminationMonitor.class), TerminationMonitor.ADDRESS);

        actorSystem.actorOf(QuarantinedMonitorActor.props(new Effect() {

            @Override
            public void apply() throws Exception {
                // restart the entire karaf container
                LOG.warn("Restarting karaf container");
                System.setProperty("karaf.restart", "true");
                bundleContext.getBundle(0).stop();
            }
        }), QuarantinedMonitorActor.ADDRESS);

    }

    @Override
    public ActorSystem getActorSystem() {
        return actorSystem;
    }

    @Override
    public ListenerRegistration<ActorSystemProviderListener> registerActorSystemProviderListener(
            final ActorSystemProviderListener listener) {
        return listeners.register(listener);
    }

    @Override
    public void close() {
        LOG.info("Shutting down ActorSystem");

        try {
            Await.result(actorSystem.terminate(), Duration.create(10, TimeUnit.SECONDS));
        } catch (Exception e) {
            LOG.warn("Error awaiting actor termination", e);
        }
    }
}
