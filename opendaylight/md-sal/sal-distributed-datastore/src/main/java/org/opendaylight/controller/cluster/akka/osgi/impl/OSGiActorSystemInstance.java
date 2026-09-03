/*
 * Copyright (c) 2020 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.akka.osgi.impl;

import static java.util.Objects.requireNonNull;

import java.util.concurrent.TimeoutException;
import org.apache.pekko.actor.Address;
import org.apache.pekko.osgi.BundleDelegatingClassLoader;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.controller.cluster.akka.impl.ClusterActorSystemInstance;
import org.opendaylight.controller.cluster.akka.impl.AkkaConfigFactory;
import org.opendaylight.controller.cluster.common.actor.AkkaConfigurationReader;
import org.opendaylight.yangtools.concepts.AccessControllerCompat;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.duration.Duration;

@Component(immediate = true)
public final class OSGiActorSystemInstance extends ClusterActorSystemInstance {
    private static final Logger LOG = LoggerFactory.getLogger(OSGiActorSystemInstance.class);

    @NonNullByDefault
    private record KarafCallbacks(BundleContext bundleContext) implements Callbacks {
        KarafCallbacks {
            requireNonNull(bundleContext);
        }

        @Override
        public void onLocalDown() {
            restartKaraf();
        }

        @Override
        public void onRemoteQuarantined(final Address quarantinedBy) {
            restartKaraf();
        }

        private void restartKaraf() {
            // restart the entire karaf container
            LOG.warn("Restarting karaf container");
            System.setProperty("karaf.restart.jvm", "true");
            System.setProperty("karaf.restart", "true");
            try {
                bundleContext.getBundle(0).stop();
            } catch (BundleException | IllegalStateException | SecurityException e) {
                LOG.error("Failed to stop framework bundle", e);
            }
        }
    }

    @Activate
    public OSGiActorSystemInstance(@Reference final AkkaConfigurationReader reader, final BundleContext bundleContext) {
        super(AkkaConfigFactory.createAkkaConfig(reader), new KarafCallbacks(bundleContext),
            AccessControllerCompat.get(() -> new BundleDelegatingClassLoader(bundleContext.getBundle(),
                Thread.currentThread().getContextClassLoader())));
    }

    @Deactivate
    void deactivate() throws TimeoutException, InterruptedException {
        LOG.info("Actor System provider stopping");
        shutdownAndWait(Duration.Inf());
        LOG.info("Actor System provider stopped");
    }
}
