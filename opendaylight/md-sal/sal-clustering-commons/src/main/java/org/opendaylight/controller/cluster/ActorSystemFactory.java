/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster;

import akka.actor.ActorSystem;
import akka.osgi.BundleDelegatingClassLoader;
import com.typesafe.config.ConfigFactory;
import java.util.Hashtable;
import org.opendaylight.controller.cluster.common.actor.AkkaConfigurationReader;
import org.opendaylight.controller.cluster.common.actor.FileAkkaConfigurationReader;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ActorSystemFactory {
    private static final String ACTOR_SYSTEM_NAME = "opendaylight-cluster-data";
    private static final String CONFIGURATION_NAME = "odl-cluster-data";
    private static final AkkaConfigurationReader configurationReader = new FileAkkaConfigurationReader();
    private static final Logger LOG = LoggerFactory.getLogger(ActorSystemFactory.class);
    private static volatile ActorSystem INSTANCE;

    public static ActorSystem createActorSystem(BundleContext bundleContext){
        if (INSTANCE == null) {
            synchronized (ActorSystemFactory.class) {
                if(INSTANCE == null){
                    LOG.info("Creating new actorSystem");
                    BundleDelegatingClassLoader classLoader = new BundleDelegatingClassLoader(bundleContext.getBundle(),
                            Thread.currentThread().getContextClassLoader());

                    INSTANCE = ActorSystem.create(ACTOR_SYSTEM_NAME,
                            ConfigFactory.load(configurationReader.read()).getConfig(CONFIGURATION_NAME), classLoader);

                    bundleContext.registerService(ActorSystem.class, INSTANCE, new Hashtable<String, Object>());
                }
            }
        }

        return INSTANCE;
    }
}
