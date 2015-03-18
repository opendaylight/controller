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
import com.typesafe.config.ConfigFactory;
import org.junit.AfterClass;
import org.junit.BeforeClass;

public class RpcListenerTest {

  static ActorSystem system;


  @BeforeClass
  public static void setup() throws InterruptedException {
    system = ActorSystem.create("opendaylight-rpc", ConfigFactory.load().getConfig("odl-cluster-rpc"));
  }

  @AfterClass
  public static void teardown() {
    JavaTestKit.shutdownActorSystem(system);
    system = null;
  }

}
