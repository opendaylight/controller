/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.remote.rpc;


import akka.actor.ActorSystem;
import junit.framework.Assert;
import org.junit.After;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ActorSystemFactoryTest {
  ActorSystem system = null;

  @Test
  public void testActorSystemCreation(){
    BundleContext context = mock(BundleContext.class);
    when(context.getBundle()).thenReturn(mock(Bundle.class));
    ActorSystemFactory.createInstance(context);
    system = ActorSystemFactory.getInstance();
    Assert.assertNotNull(system);
    // Check illegal state exception

    try {
      ActorSystemFactory.createInstance(context);
      fail("Illegal State exception should be thrown, while creating actor system second time");
    } catch (IllegalStateException e) {
    }
  }

  @After
  public void cleanup() throws InterruptedException {
    if(system != null) {
      system.shutdown();
    }
  }

}
