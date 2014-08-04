/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.remote.rpc;

import akka.actor.ActorSystem;
import akka.osgi.BundleDelegatingClassLoader;
import com.typesafe.config.ConfigFactory;
import org.osgi.framework.BundleContext;


public class ActorSystemFactory {
 private static volatile ActorSystem actorSystem = null;

  public static final ActorSystem getInstance(){
     return actorSystem;
  }

  /**
   * This method should be called only once during initialization
   *
   * @param bundleContext
   */
  public static final void createInstance(final BundleContext bundleContext) {
    if(actorSystem == null) {
      // Create an OSGi bundle classloader for actor system
      BundleDelegatingClassLoader classLoader = new BundleDelegatingClassLoader(bundleContext.getBundle(),
          Thread.currentThread().getContextClassLoader());
      synchronized (ActorSystemFactory.class) {
        // Double check
        if (actorSystem == null) {
          ActorSystem system = ActorSystem.create("opendaylight-cluster-rpc",
              ConfigFactory.load().getConfig("odl-cluster-rpc"), classLoader);
          actorSystem = system;
        }
      }
    } else {
      throw new IllegalStateException("Actor system should be created only once. Use getInstance method to access existing actor system");
    }
  }
}
