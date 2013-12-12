package org.opendaylight.controller.sal.connector.remoterpc;

import junit.framework.Assert;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.sal.connector.remoterpc.dto.Message;
import org.opendaylight.controller.sal.connector.remoterpc.utils.MessagingUtil;
import org.zeromq.ZMQ;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 *
 */
public class ClientRequestHandlerTest {

  ZMQ.Context context = ZMQ.context(1);
  ExecutorService serverThread;
  final String SERVER_ADDRESS = "localhost:5554";

  ClientRequestHandler handler;

  @Before
  public void setUp() throws Exception {
    serverThread = Executors.newCachedThreadPool();
    serverThread.execute(MessagingUtil.startReplyServer(context, SERVER_ADDRESS));

    handler = new ClientRequestHandler(context);
  }

  @After
  public void tearDown() throws Exception {
    serverThread.shutdown();
    Thread.currentThread().sleep(2000);
  }

  @Test
  public void handle_SingleRemote_ShouldReturnResponse() throws Exception {
    Message request = new Message();
    request.setRecipient(SERVER_ADDRESS);
    Message response = handler.handle(request);
    Assert.assertNotNull(response);
    //should be connected to only 1 remote server
    Assert.assertEquals(1, handler.getWorkerCount());
    Assert.assertEquals(response.getRecipient(), SERVER_ADDRESS);
  }

  @Test
  public void handle_MultiRemote_ShouldReturnResponses() throws Exception {
    ExecutorService threadPool = Executors.newCachedThreadPool();
    final int port = 5555;
    String serverAddress = null;
    for (int i=0;i<5;i++){
      serverAddress = "localhost:" + (port+i);
      threadPool.execute(createEmptyMessageTaskAndHandle(handler, serverAddress));
    }
    Thread.currentThread().sleep(5000);//wait for all messages to get processed
    //should be connected to 5 remote server
    Assert.assertEquals(5, handler.getWorkerCount());
  }

  private Runnable createEmptyMessageTaskAndHandle(final ClientRequestHandler handler, final String serverAddress){

    return new Runnable() {
      @Override
      public void run() {
        Message request = new Message();
        request.setRecipient(serverAddress);
        try {
          Message response = handler.handle(request);
          Assert.assertNotNull(response);
          Assert.assertEquals(response.getRecipient(), serverAddress);
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      }
    };
  }

}
