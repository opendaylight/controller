/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.connector.remoterpc;

import com.google.common.base.Optional;
import junit.framework.Assert;
import org.junit.After;
import org.junit.Before;
import org.zeromq.ZMQ;
import org.opendaylight.controller.sal.connector.remoterpc.SocketManager;
import org.opendaylight.controller.sal.connector.remoterpc.RpcSocket;
import org.opendaylight.controller.sal.connector.remoterpc.Context;
import org.junit.Test;

public class SocketManagerTest {

  SocketManager manager;

  @Before
  public void setup(){
    manager = new SocketManager();
  }

  @After
  public void tearDown() throws Exception {
    manager.close();
  }

  @Test
  public void getManagedSockets_When2NewAdded_ShouldContain2() throws Exception {

    //Prepare data
    manager.getManagedSocket("tcp://localhost:5554");
    manager.getManagedSocket("tcp://localhost:5555");

    Assert.assertTrue( 2 == manager.getManagedSockets().size());
  }

  @Test
  public void getManagedSockets_When2NewAddedAnd1Existing_ShouldContain2() throws Exception {

    //Prepare data
    manager.getManagedSocket("tcp://localhost:5554");
    manager.getManagedSocket("tcp://localhost:5555");
    manager.getManagedSocket("tcp://localhost:5554"); //ask for the first one

    Assert.assertTrue( 2 == manager.getManagedSockets().size());
  }

  @Test
  public void getManagedSocket_WhenPassedAValidAddress_ShouldReturnARpcSocket() throws Exception {
    String testAddress = "tcp://localhost:5554";
    RpcSocket rpcSocket = manager.getManagedSocket(testAddress);
    Assert.assertEquals(testAddress, rpcSocket.getAddress());
  }

  @Test(expected = IllegalArgumentException.class)
  public void getManagedSocket_WhenPassedInvalidHostAddress_ShouldThrow() throws Exception {
    String testAddress = "tcp://nonexistenthost:5554";
    RpcSocket rpcSocket = manager.getManagedSocket(testAddress);
  }

  @Test(expected = IllegalArgumentException.class)
  public void getManagedSocket_WhenPassedInvalidAddress_ShouldThrow() throws Exception {
    String testAddress = "xxx";
    RpcSocket rpcSocket = manager.getManagedSocket(testAddress);
  }

  @Test
  public void getManagedSocket_WhenPassedAValidZmqSocket_ShouldReturnARpcSocket() throws Exception {
    //Prepare data
    String firstAddress = "tcp://localhost:5554";
    RpcSocket firstRpcSocket = manager.getManagedSocket(firstAddress);
    ZMQ.Socket firstZmqSocket = firstRpcSocket.getSocket();

    String secondAddress = "tcp://localhost:5555";
    RpcSocket secondRpcSocket = manager.getManagedSocket(secondAddress);
    ZMQ.Socket secondZmqSocket = secondRpcSocket.getSocket();

    Assert.assertEquals(firstRpcSocket, manager.getManagedSocketFor(firstZmqSocket).get());
    Assert.assertEquals(secondRpcSocket, manager.getManagedSocketFor(secondZmqSocket).get());
  }

  @Test
  public void getManagedSocket_WhenPassedNonManagedZmqSocket_ShouldReturnNone() throws Exception {
    ZMQ.Socket nonManagedSocket = Context.getInstance().getZmqContext().socket(ZMQ.REQ);
    nonManagedSocket.connect("tcp://localhost:5000");

    //Prepare data
    String firstAddress = "tcp://localhost:5554";
    RpcSocket firstRpcSocket = manager.getManagedSocket(firstAddress);
    ZMQ.Socket firstZmqSocket = firstRpcSocket.getSocket();

    Assert.assertSame(Optional.<RpcSocket>absent(), manager.getManagedSocketFor(nonManagedSocket) );
    Assert.assertSame(Optional.<RpcSocket>absent(), manager.getManagedSocketFor(null) );
  }

  @Test
  public void stop_WhenCalled_ShouldEmptyManagedSockets() throws Exception {
    manager.getManagedSocket("tcp://localhost:5554");
    manager.getManagedSocket("tcp://localhost:5555");
    Assert.assertTrue( 2 == manager.getManagedSockets().size());

    manager.close();
    Assert.assertTrue( 0 == manager.getManagedSockets().size());
  }

  @Test
  public void poller_WhenCalled_ShouldReturnAnInstanceOfPoller() throws Exception {
    Assert.assertTrue (manager.getPoller() instanceof ZMQ.Poller);
  }

}
