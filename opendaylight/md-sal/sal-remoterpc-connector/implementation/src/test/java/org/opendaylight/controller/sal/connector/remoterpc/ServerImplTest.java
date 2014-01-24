/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.connector.remoterpc;


import com.google.common.base.Optional;
import junit.framework.Assert;
import org.junit.*;
import org.opendaylight.controller.sal.connector.api.RpcRouter;
import org.opendaylight.controller.sal.connector.remoterpc.api.RoutingTable;
import org.opendaylight.controller.sal.connector.remoterpc.utils.MessagingUtil;
import org.opendaylight.controller.sal.core.api.Broker;
import org.opendaylight.controller.sal.core.api.RpcRegistrationListener;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.zeromq.ZMQ;
import zmq.Ctx;
import zmq.SocketBase;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ServerImplTest {

  private static ZMQ.Context context;
  private ServerImpl server;
  private Broker.ProviderSession brokerSession;
  private RoutingTableProvider routingTableProvider;
  private RpcRegistrationListener listener;

  ExecutorService pool;

  //Server configuration
  private final int HANDLER_COUNT = 2;
  private final int HWM = 200;
  private final int port = 5554;
  //server address
  private final String SERVER_ADDRESS = "tcp://localhost:5554";

  //@BeforeClass
  public static void init() {
    context = ZMQ.context(1);
  }

  //@AfterClass
  public static void destroy() {
    MessagingUtil.closeZmqContext(context);
  }

  @Before
  public void setup() throws InterruptedException {
    context = ZMQ.context(1);
    brokerSession = mock(Broker.ProviderSession.class);
    routingTableProvider = mock(RoutingTableProvider.class);
    listener = mock(RpcRegistrationListener.class);

    server = new ServerImpl(port);
    server.setBrokerSession(brokerSession);

    RoutingTable<RpcRouter.RouteIdentifier, String> mockRoutingTable = new MockRoutingTable<String, String>();
    Optional<RoutingTable<RpcRouter.RouteIdentifier, String>> optionalRoutingTable = Optional.fromNullable(mockRoutingTable);
    when(routingTableProvider.getRoutingTable()).thenReturn(optionalRoutingTable);

    when(brokerSession.addRpcRegistrationListener(listener)).thenReturn(null);
    when(brokerSession.getSupportedRpcs()).thenReturn(Collections.EMPTY_SET);
    when(brokerSession.rpc(null, mock(CompositeNode.class))).thenReturn(null);
    server.start();
    Thread.sleep(5000);//wait for server to start
  }

  @After
  public void tearDown() throws InterruptedException {

    if (pool != null)
      pool.shutdown();

    if (server != null)
      server.stop();

    MessagingUtil.closeZmqContext(context);

    Thread.sleep(5000);//wait for server to stop
    Assert.assertEquals(ServerImpl.State.STOPPED, server.getStatus());
  }

  @Test
  public void getBrokerSession_Call_ShouldReturnBrokerSession() throws Exception {
    Optional<Broker.ProviderSession> mayBeBroker = server.getBrokerSession();

    if (mayBeBroker.isPresent())
      Assert.assertEquals(brokerSession, mayBeBroker.get());
    else
      Assert.fail("Broker does not exist in Remote RPC Server");

  }

  @Test
  public void start_Call_ShouldSetServerStatusToStarted() throws Exception {
    Assert.assertEquals(ServerImpl.State.STARTED, server.getStatus());

  }

  @Test
  public void start_Call_ShouldCreateNZmqSockets() throws Exception {
    final int EXPECTED_COUNT = 2 + HANDLER_COUNT; //1 ROUTER + 1 DEALER + HANDLER_COUNT

    Optional<ZMQ.Context> mayBeContext = server.getZmqContext();
    if (mayBeContext.isPresent())
      Assert.assertEquals(EXPECTED_COUNT, findSocketCount(mayBeContext.get()));
    else
      Assert.fail("ZMQ Context does not exist in Remote RPC Server");
  }

  @Test
  public void start_Call_ShouldCreate1ServerThread() {
    final String SERVER_THREAD_NAME = "remote-rpc-server";
    final int EXPECTED_COUNT = 1;
    List<Thread> serverThreads = findThreadsWithName(SERVER_THREAD_NAME);
    Assert.assertEquals(EXPECTED_COUNT, serverThreads.size());
  }

  @Test
  public void start_Call_ShouldCreateNHandlerThreads() {
    //final String WORKER_THREAD_NAME = "remote-rpc-worker";
    final int EXPECTED_COUNT = HANDLER_COUNT;

    Optional<ServerRequestHandler> serverRequestHandlerOptional = server.getHandler();
    if (serverRequestHandlerOptional.isPresent()){
      ServerRequestHandler handler = serverRequestHandlerOptional.get();
      ThreadPoolExecutor workerPool = handler.getWorkerPool();
      Assert.assertEquals(EXPECTED_COUNT, workerPool.getPoolSize());
    } else {
      Assert.fail("Server is in illegal state. ServerHandler does not exist");
    }

  }

  @Test
  public void testStop() throws Exception {

  }

  @Test
  public void testOnRouteUpdated() throws Exception {

  }

  @Test
  public void testOnRouteDeleted() throws Exception {

  }

  private int findSocketCount(ZMQ.Context context)
      throws NoSuchFieldException, IllegalAccessException {
    Field ctxField = context.getClass().getDeclaredField("ctx");
    ctxField.setAccessible(true);
    Ctx ctx = Ctx.class.cast(ctxField.get(context));

    Field socketListField = ctx.getClass().getDeclaredField("sockets");
    socketListField.setAccessible(true);
    List<SocketBase> sockets = List.class.cast(socketListField.get(ctx));

    return sockets.size();
  }

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
