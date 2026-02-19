/*
 * Copyright (c) 2020 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.akka.osgi.impl;

import java.util.concurrent.TimeoutException;
import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.osgi.BundleDelegatingClassLoader;
import org.opendaylight.controller.cluster.ActorSystemProvider;
import org.opendaylight.controller.cluster.ActorSystemProviderListener;
import org.opendaylight.controller.cluster.akka.impl.ActorSystemProviderImpl;
import org.opendaylight.controller.cluster.akka.impl.AkkaConfigFactory;
import org.opendaylight.controller.cluster.common.actor.AkkaConfigurationReader;
import org.opendaylight.yangtools.concepts.AccessControllerCompat;
import org.opendaylight.yangtools.concepts.ObjectRegistration;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.Await;
import scala.concurrent.duration.Duration;

@Component(immediate = true)
public final class OSGiActorSystemProvider implements ActorSystemProvider {
    private static final Logger LOG = LoggerFactory.getLogger(OSGiActorSystemProvider.class);

    private ActorSystemProviderImpl delegate;

    @Activate
    public OSGiActorSystemProvider(@Reference final AkkaConfigurationReader reader, final BundleContext bundleContext) {
        LOG.info("Actor System provider starting");
        final var akkaConfig = AkkaConfigFactory.createAkkaConfig(reader);
        final var bundle = bundleContext.getBundle();
        delegate = new ActorSystemProviderImpl(
            AccessControllerCompat.get(() ->
                new BundleDelegatingClassLoader(bundle, Thread.currentThread().getContextClassLoader())),
            QuarantinedMonitorActorPropsFactory.createProps(bundleContext, akkaConfig), akkaConfig);
        LOG.info("Actor System provider started");
    }

    @Deactivate
    void deactivate() throws TimeoutException, InterruptedException {
        LOG.info("Actor System provider stopping");
        Await.result(delegate.asyncClose(), Duration.Inf());
        delegate = null;
        LOG.info("Actor System provider stopped");
    }

    @Override
    public ActorSystem getActorSystem() {
        return delegate.getActorSystem();
    }

    @Override
    public ObjectRegistration<ActorSystemProviderListener> registerActorSystemProviderListener(
            final ActorSystemProviderListener listener) {
        return delegate.registerActorSystemProviderListener(listener);
    }
}
