/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.connector.remoterpc;

import org.opendaylight.controller.sal.connector.api.RpcRouter;
import org.opendaylight.controller.sal.connector.remoterpc.dto.Message;
import org.opendaylight.controller.sal.connector.remoterpc.dto.RouteIdentifierImpl;
import org.opendaylight.controller.sal.connector.remoterpc.util.XmlUtils;
import org.opendaylight.controller.sal.core.api.Broker;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.ZMQ;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collection;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 */
public class ServerRequestHandler implements AutoCloseable{

  private Logger _logger = LoggerFactory.getLogger(ServerRequestHandler.class);
  private final String DEFAULT_NAME = "remote-rpc-worker";
  private String dealerAddress;
  private String serverAddress;
  private int workerCount;
  private ZMQ.Context context;
  private Broker.ProviderSession broker;

  private RequestHandlerThreadPool workerPool;
  private final AtomicInteger threadId = new AtomicInteger();

  public ServerRequestHandler(ZMQ.Context context,
                              Broker.ProviderSession session,
                              int workerCount,
                              String dealerAddress,
                              String serverAddress) {
    this.context       = context;
    this.dealerAddress = dealerAddress;
    this.serverAddress = serverAddress;
    this.broker        = session;
    this.workerCount   = workerCount;
  }

  public ThreadPoolExecutor getWorkerPool(){
    return workerPool;
  }

  public void start(){
    workerPool = new RequestHandlerThreadPool(
        workerCount, workerCount,
        0L, TimeUnit.MILLISECONDS,
        new LinkedBlockingQueue<Runnable>());
    //unbound is ok. Task will never be submitted

    for (int i=0;i<workerCount;i++){
      workerPool.execute(new Worker(threadId.incrementAndGet()));
    }
  }

  /**
   * This gets called automatically if used with try-with-resources
   * @throws Exception
   */
  @Override
  public void close() throws Exception {
    if (workerPool != null)
      workerPool.shutdown();
    _logger.info("Request Handler closed");
  }

  /**
   * Worker to handles RPC request
   */
  private class Worker implements Runnable {
    private String name;

    public Worker(int id){
      this.name = DEFAULT_NAME + "-" + id;
    }

    @Override
    public void run() {
      Thread.currentThread().setName(name);
      _logger.debug("Starting... ");
      ZMQ.Socket socket = null;

      try {
        socket = context.socket(ZMQ.REP);
        socket.connect(dealerAddress);

        while (!Thread.currentThread().isInterrupted()) {

          MessageHandler handler = new MessageHandler(socket);
          handler.receiveMessage();

          if (handler.hasMessageForBroker()) {

            Message request = handler.getMessage();
            Future<RpcResult<CompositeNode>> rpc = null;
            RpcResult<CompositeNode> result = null;

            //TODO Call this in a new thread with timeout
            try {
              rpc = broker.rpc(
                  (QName) request.getRoute().getType(),
                  XmlUtils.xmlToCompositeNode((String) request.getPayload()));

              result = (rpc != null) ? rpc.get() : null;

              handler.sendResponse(result);

            } catch (Exception e) {
              _logger.debug("Broker threw  [{}]", e);
              handler.sendError(e.getMessage());
            }
          }

        }
      } catch (Exception e) {
        printException(e);
      } finally {
        closeSocket(socket);
      }
    }

    private void printException(Exception e) {
      try (StringWriter s = new StringWriter();
           PrintWriter p = new PrintWriter(s)) {
        e.printStackTrace(p);
        _logger.debug(s.toString());
      } catch (IOException e1) {/*Ignore and continue*/ }
    }

    private void closeSocket(ZMQ.Socket socket) {
      try {
        if (socket != null) socket.close();
      } catch (Exception x) {
        _logger.debug("Exception while closing socket [{}]", x);
      } finally {
        if (socket != null) socket.close();
      }
      _logger.debug("Closing...");
    }
  }


  /**
   *
   */
  public class RequestHandlerThreadPool extends ThreadPoolExecutor{

    public RequestHandlerThreadPool(int corePoolSize,
                                    int maximumPoolSize,
                                    long keepAliveTime,
                                    TimeUnit unit,
                                    BlockingQueue<Runnable> workQueue) {
      super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
    }

    @Override
    protected void afterExecute(Runnable r, Throwable t) {
      if (isTerminating() || isTerminated() || isShutdown())
        return;

      if ( t != null ){
        _logger.debug("Exception caught while terminating worker [{},{}]", t.getClass(), t.getMessage());
      }

      this.execute(new Worker(threadId.incrementAndGet()));
      super.afterExecute(r, null);
    }
  }

  class MessageHandler{
    private ZMQ.Socket socket;
    private Message message;          //parsed message received on zmq server port
    private boolean messageForBroker = false; //if the message is valid and not a "ping" message

    public MessageHandler(ZMQ.Socket socket){
      this.socket = socket;
    }

    void receiveMessage(){
      byte[] bytes = socket.recv(); //this blocks
      _logger.debug("Received bytes:[{}]", bytes.length);

      Object objectRecvd = null;
      try{
        objectRecvd = Message.deserialize(bytes);
      }catch (Exception e){
        sendError(e.getMessage());
        return;
      }

      if (!(objectRecvd instanceof Message)) {
        sendError("Invalid message received");
        return;
      }

      message = (Message) objectRecvd;

      _logger.info("Received request [{}]", message);

      if (Message.MessageType.PING == message.getType()){
        sendPong();
        return;
      }

      messageForBroker = true;
    }

    boolean hasMessageForBroker(){
      return messageForBroker;
    }

    Message getMessage(){
      return message;
    }

    void sendResponse(RpcResult<CompositeNode> result){
      CompositeNode payload = (result != null) ? result.getResult() : null;

      String recipient = null;
      RpcRouter.RouteIdentifier routeId = null;

      if (message != null) {
        recipient = message.getSender();
        routeId   = message.getRoute();
      }

      Message response = new Message.MessageBuilder()
          .type(Message.MessageType.RESPONSE)
          .sender(serverAddress)
          .recipient(recipient)
          .route(routeId)
          .payload(XmlUtils.compositeNodeToXml(payload))
          .build();

      send(response);
    }

    private void sendError(String msg){
      Message errorResponse = new Message.MessageBuilder()
          .type(Message.MessageType.ERROR)
          .sender(serverAddress)
          .payload(msg)
          .build();

      send(errorResponse);
    }

    private void sendPong(){
      Message pong = new Message.MessageBuilder()
          .type(Message.MessageType.PONG)
          .sender(serverAddress)
          .build();

      send(pong);
    }

    private void send(Message msg){
      byte[] serializedMessage = null;
      try {
        serializedMessage = Message.serialize(msg);
      } catch (Exception e) {
        _logger.debug("Unexpected error during serialization of response [{}]", msg);
        return;
      }

      if (serializedMessage != null)
        if (socket.send(serializedMessage))
          _logger.info("Response sent [{}]", msg);
        else  _logger.debug("Failed to send serialized message");
    }
  }
}
