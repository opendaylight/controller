/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.remote.rpc;

import akka.actor.ActorSystem;
import akka.osgi.BundleDelegatingClassLoader;
import com.typesafe.config.Config;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcProviderService;
import org.opendaylight.controller.sal.core.api.Broker;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RemoteRpcProviderFactory {
    private static final Logger LOG = LoggerFactory.getLogger(RemoteRpcProviderFactory.class);

    public static RemoteRpcProvider createInstance(
            final Broker broker, final BundleContext bundleContext, final RemoteRpcProviderConfig config){

      final RemoteRpcProvider rpcProvider =
          new RemoteRpcProvider(createActorSystem(bundleContext, config), (DOMRpcProviderService) broker);

      broker.registerProvider(rpcProvider);
      return rpcProvider;
    }

    private static ActorSystem createActorSystem(final BundleContext bundleContext, final RemoteRpcProviderConfig config){

        // Create an OSGi bundle classloader for actor system
        final BundleDelegatingClassLoader classLoader =
                new BundleDelegatingClassLoader(bundleContext.getBundle(),
                        Thread.currentThread().getContextClassLoader());

        final Config actorSystemConfig = config.get();
        if(LOG.isDebugEnabled()) {
            LOG.debug("Actor system configuration\n{}", actorSystemConfig.root().render());
        }
        if (config.isMetricCaptureEnabled()) {
            LOG.info("Instrumentation is enabled in actor system {}. Metrics can be viewed in JMX console.",
                    config.getActorSystemName());
        }

        return ActorSystem.create(config.getActorSystemName(), actorSystemConfig, classLoader);
    }
}
