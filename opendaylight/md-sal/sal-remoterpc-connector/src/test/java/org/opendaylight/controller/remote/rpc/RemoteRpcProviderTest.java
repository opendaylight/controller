/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */


package org.opendaylight.controller.remote.rpc;


import akka.actor.ActorSystem;
import akka.testkit.JavaTestKit;
import com.typesafe.config.Config;
import org.junit.AfterClass;
import org.junit.BeforeClass;

public class RemoteRpcProviderTest {

  static ActorSystem system;
  static RemoteRpcProviderConfig moduleConfig;

  @BeforeClass
  public static void setup() throws InterruptedException {
    moduleConfig = new RemoteRpcProviderConfig.Builder("odl-cluster-rpc").build();
    final Config config = moduleConfig.get();
    system = ActorSystem.create("odl-cluster-rpc", config);

  }

  @AfterClass
  public static void teardown() {
    JavaTestKit.shutdownActorSystem(system);
    system = null;
  }

}
