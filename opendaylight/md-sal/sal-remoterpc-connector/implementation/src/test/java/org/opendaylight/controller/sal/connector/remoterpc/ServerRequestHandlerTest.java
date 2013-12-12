/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.connector.remoterpc;

import junit.framework.Assert;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.sal.connector.remoterpc.utils.MessagingUtil;
import org.opendaylight.controller.sal.core.api.Broker;
import org.zeromq.ZMQ;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.mockito.Mockito.mock;

public class ServerRequestHandlerTest {

  ServerRequestHandler handler;
  ZMQ.Context context;
  ExecutorService executorService = Executors.newCachedThreadPool();
  private int workerCount = 2;
  private String mockDealerAddress = "inproc://rpc-request-handler";
  private String mockServerIp = "localhost";
  private int mockServerPort = 5554;

  @Before
  public void setUp() throws Exception {
    context = ZMQ.context(1);
    String mockServerAddress = mockServerIp + ":" + mockServerPort;
    Broker.ProviderSession mockSession = mock(Broker.ProviderSession.class);
    handler = new ServerRequestHandler(context, mockSession, workerCount, mockDealerAddress, mockServerAddress);
    handler.start();
  }

  @After
  public void tearDown() throws Exception {
    executorService.shutdown();
    MessagingUtil.closeZmqContext(context);
    handler.close();
  }

  @Test
  public void testStart() throws Exception {
    //should start workers == workerCount
    Assert.assertEquals(workerCount, handler.getWorkerPool().getPoolSize());

    //killing a thread should recreate another one

    //start router-dealer bridge
    executorService.execute(MessagingUtil.createRouterDealerBridge(context, mockDealerAddress, mockServerPort));
    Thread.sleep(1000); //give sometime for socket initialization

    //this will kill the thread
    final String WORKER_THREAD_NAME = "remote-rpc-worker";
    interruptAThreadWithName(WORKER_THREAD_NAME);

    //send 4 message to router
    for (int i = 0; i < 4; i++)
      executorService.execute(MessagingUtil.sendAnEmptyMessage(context, "tcp://" + mockServerIp + ":" + mockServerPort));

    //worker pool size should not change.
    Assert.assertEquals(workerCount, handler.getWorkerPool().getPoolSize());

    Thread.sleep(10000); //wait for processing to complete
  }

  @Test
  public void testClose() throws Exception {

  }

  /**
   * Interrupts the first thread found whose name starts with the provided name
   *
   * @param name
   */
  private void interruptAThreadWithName(String name) {
    List<Thread> workerThreads = findThreadsWithName(name);
    if (workerThreads.size() > 0) workerThreads.get(0).interrupt();
  }

  /**
   * Find all threads that start with the given name
   *
   * @param name
   * @return
   */
  private List<Thread> findThreadsWithName(String name) {
    Thread[] threads = new Thread[Thread.activeCount()];
    Thread.enumerate(threads);

    List<Thread> foundThreads = new ArrayList();
    for (Thread t : threads) {
      if (t.getName().startsWith(name))
        foundThreads.add(t);
    }

    return foundThreads;
  }
}
