/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.connector.remoterpc;

import junit.framework.Assert;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.sal.connector.remoterpc.dto.Message;
import org.opendaylight.controller.sal.connector.remoterpc.utils.MessagingUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.ZMQ;

import java.io.IOException;
import java.util.concurrent.*;

/**
 *
 */
public class ClientRequestHandlerTest {

  private final Logger _logger = LoggerFactory.getLogger(ClientRequestHandlerTest.class);

  ZMQ.Context context;
  ExecutorService serverThread;
  final String SERVER_ADDRESS = "localhost:5553";

  ClientRequestHandler handler;

  @Before
  public void setUp() throws Exception {
    context = ZMQ.context(1);
    serverThread = Executors.newCachedThreadPool();
    handler = new ClientRequestHandler(context);
  }

  @After
  public void tearDown() throws Exception {
    serverThread.shutdown();
    MessagingUtil.closeZmqContext(context);
    handler.close();
  }

 @Test
  public void handle_SingleRemote_ShouldReturnResponse() throws Exception {
    serverThread.execute(MessagingUtil.startReplyServer(context, SERVER_ADDRESS, 1));
    Message request = new Message();
    request.setRecipient(SERVER_ADDRESS);
    Message response = handleMessageWithTimeout(request);
    Assert.assertNotNull(response);
    //should be connected to only 1 remote server
    Assert.assertEquals(1, handler.getWorkerCount());
    Assert.assertEquals(response.getRecipient(), SERVER_ADDRESS);
  }

 // @Test
  public void handle_MultiRemote_ShouldReturnResponses() throws Exception {
    ExecutorService threadPool = Executors.newCachedThreadPool();
    final int port = 5555;
    String serverAddress = null;
    for (int i = 0; i < 5; i++) {
      serverAddress = "localhost:" + (port + i);
      serverThread.execute(MessagingUtil.startReplyServer(context, serverAddress, 1));
      threadPool.execute(createEmptyMessageTaskAndHandle(handler, serverAddress));
    }
    Thread.currentThread().sleep(5000);//wait for all messages to get processed
    //should be connected to 5 remote server
    Assert.assertEquals(5, handler.getWorkerCount());
  }

  private Runnable createEmptyMessageTaskAndHandle(final ClientRequestHandler handler, final String serverAddress) {

    return new Runnable() {
      @Override
      public void run() {
        Message request = new Message();
        request.setRecipient(serverAddress);
        try {
          Message response = handleMessageWithTimeout(request);
          Assert.assertNotNull(response);
          Assert.assertEquals(response.getRecipient(), serverAddress);
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      }
    };
  }

  private Message handleMessageWithTimeout(final Message request) {
    Message response = null;

    FutureTask task = new FutureTask(new Callable<Message>() {

      @Override
      public Message call() {
        try {
          return handler.handle(request);
        } catch (Exception e) {
          _logger.debug("Client handler failed to handle request. Exception is [{}]", e);
        }
        return null;
      }
    });

    serverThread.execute(task);

    try {
      response = (Message) task.get(5L, TimeUnit.SECONDS); //wait for max 5 sec for server to respond
    } catch (Exception e) {/*ignore and continue*/}

    return response;
  }

}
