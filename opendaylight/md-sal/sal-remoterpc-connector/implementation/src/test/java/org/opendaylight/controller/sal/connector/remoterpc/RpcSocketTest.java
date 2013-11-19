/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.connector.remoterpc;

import junit.framework.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendaylight.controller.sal.connector.remoterpc.dto.MessageWrapper;
import org.opendaylight.controller.sal.connector.remoterpc.RpcSocket;
import org.opendaylight.controller.sal.connector.remoterpc.dto.Message;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.zeromq.ZMQ;

import java.util.concurrent.TimeoutException;

import static org.mockito.Mockito.doNothing;

@RunWith(PowerMockRunner.class)
@PrepareForTest(RpcSocket.class)
public class RpcSocketTest {
  RpcSocket rpcSocket = new RpcSocket("tcp://localhost:5554", new ZMQ.Poller(1));
  RpcSocket spy = PowerMockito.spy(rpcSocket);

  @Test(expected = TimeoutException.class)
  public void send_WhenQueueGetsFull_ShouldThrow() throws Exception {

    doNothing().when(spy).process();

    //10 is queue size
    for (int i=0;i<10;i++){
      spy.send(getEmptyMessageWrapper());
    }

    //sending 11th message should throw
    spy.send(getEmptyMessageWrapper());
  }

  @Test
  public void testProcess() throws Exception {

    PowerMockito.doNothing().when(spy, "sendMessage");
    spy.send(getEmptyMessageWrapper());

    //Next message should get queued
    spy.send(getEmptyMessageWrapper());

    //queue size should be 2
    Assert.assertEquals(2, spy.queueSize());

    spy.process();
    //sleep for 2 secs (timeout)
    //message send would be retried
    Thread.sleep(2000);
    spy.process();
    Thread.sleep(2000);
    spy.process();
    Thread.sleep(2000);
    spy.process(); //retry fails, next message will get picked up

    Assert.assertEquals(1, spy.queueSize());
  }

  @Test
  public void testClose() throws Exception {

  }

  @Test
  public void testReceive() throws Exception {
    PowerMockito.doReturn(null).when(spy, "parseMessage");
    PowerMockito.doNothing().when(spy, "process");
    spy.send(getEmptyMessageWrapper());

    //There should be 1 message waiting in the queue
    Assert.assertEquals(1, spy.queueSize());

    spy.receive();
    //This should complete message processing
    //The message should be removed from the queue
    Assert.assertEquals(0, spy.queueSize());

  }

  private MessageWrapper getEmptyMessageWrapper(){
    return new MessageWrapper(new Message(), null);
  }
}
