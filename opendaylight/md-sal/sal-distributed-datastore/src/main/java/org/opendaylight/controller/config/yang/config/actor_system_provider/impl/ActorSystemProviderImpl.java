/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yang.config.actor_system_provider.impl;

import akka.actor.ActorSystem;
import akka.osgi.BundleDelegatingClassLoader;
import com.typesafe.config.ConfigFactory;
import java.util.concurrent.TimeUnit;
import org.opendaylight.controller.cluster.ActorSystemProvider;
import org.opendaylight.controller.cluster.common.actor.AkkaConfigurationReader;
import org.opendaylight.controller.cluster.common.actor.FileAkkaConfigurationReader;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.duration.Duration;

public class ActorSystemProviderImpl implements ActorSystemProvider, AutoCloseable {
    private static final String ACTOR_SYSTEM_NAME = "opendaylight-cluster-data";
    private static final String CONFIGURATION_NAME = "odl-cluster-data";
    static final Logger LOG = LoggerFactory.getLogger(ActorSystemProviderImpl.class);

    private final ActorSystem actorSystem;

    public ActorSystemProviderImpl(BundleContext bundleContext) {
        LOG.info("Creating new ActorSystem");

        BundleDelegatingClassLoader classLoader = new BundleDelegatingClassLoader(bundleContext.getBundle(),
                Thread.currentThread().getContextClassLoader());

        AkkaConfigurationReader configurationReader = new FileAkkaConfigurationReader();
        actorSystem = ActorSystem.create(ACTOR_SYSTEM_NAME,
                ConfigFactory.load(configurationReader.read()).getConfig(CONFIGURATION_NAME), classLoader);
    }

    @Override
    public ActorSystem getActorSystem() {
        return actorSystem;
    }

    @Override
    public void close() {
        LOG.info("Shutting down ActorSystem");

        actorSystem.shutdown();
        try {
            actorSystem.awaitTermination(Duration.create(10, TimeUnit.SECONDS));
        } catch (Exception e) {
            LOG.warn("Error awaiting actor termination", e);
        }
    }
}