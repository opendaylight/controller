/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore;

import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.osgi.BundleDelegatingClassLoader;
import com.google.common.base.Preconditions;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.osgi.framework.BundleContext;

import java.io.File;

public class ActorSystemFactory {

    public static final String AKKA_CONF_PATH = "./configuration/initial/akka.conf";
    public static final String ACTOR_SYSTEM_NAME = "opendaylight-cluster-data";
    public static final String CONFIGURATION_NAME = "odl-cluster-data";

    private static volatile ActorSystem actorSystem = null;

    public static final ActorSystem getInstance(){
        return actorSystem;
    }

    /**
     * This method should be called only once during initialization
     *
     * @param bundleContext
     */
    public static final ActorSystem createInstance(final BundleContext bundleContext) {
        if(actorSystem == null) {
            // Create an OSGi bundle classloader for actor system
            BundleDelegatingClassLoader classLoader = new BundleDelegatingClassLoader(bundleContext.getBundle(),
                Thread.currentThread().getContextClassLoader());
            synchronized (ActorSystemFactory.class) {
                // Double check

                if (actorSystem == null) {
                    ActorSystem system = ActorSystem.create(ACTOR_SYSTEM_NAME,
                        ConfigFactory.load(readAkkaConfiguration()).getConfig(CONFIGURATION_NAME), classLoader);
                    system.actorOf(Props.create(TerminationMonitor.class), "termination-monitor");
                    actorSystem = system;
                }
            }
        }

        return actorSystem;
    }


    private static final Config readAkkaConfiguration(){
        File defaultConfigFile = new File(AKKA_CONF_PATH);
        Preconditions.checkState(defaultConfigFile.exists(), "akka.conf is missing");
        return ConfigFactory.parseFile(defaultConfigFile);
    }
}
