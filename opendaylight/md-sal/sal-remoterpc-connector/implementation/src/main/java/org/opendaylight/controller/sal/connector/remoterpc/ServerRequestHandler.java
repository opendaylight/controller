/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.connector.remoterpc;

import org.opendaylight.controller.sal.connector.remoterpc.dto.Message;
import org.opendaylight.controller.sal.connector.remoterpc.util.XmlUtils;
import org.opendaylight.controller.sal.core.api.Broker;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.ZMQ;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
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

          Message request = parseMessage(socket);
          _logger.debug("Received rpc request [{}]", request);

          if (request != null) {
            // Call broker to process the message then reply
            Future<RpcResult<CompositeNode>> rpc = null;
            RpcResult<CompositeNode> result = null;

            //TODO Call this in a new thread with timeout
            try {
              rpc = broker.rpc(
                  (QName) request.getRoute().getType(),
                  XmlUtils.xmlToCompositeNode((String) request.getPayload()));

              result = (rpc != null) ? rpc.get() : null;

            } catch (Exception e) {
              _logger.debug("Broker threw  [{}]", e);
            }

            CompositeNode payload = (result != null) ? result.getResult() : null;

            Message response = new Message.MessageBuilder()
                .type(Message.MessageType.RESPONSE)
                .sender(serverAddress)
                .route(request.getRoute())
                .payload(XmlUtils.compositeNodeToXml(payload))
                .build();

            _logger.debug("Sending rpc response [{}]", response);

            try {
              socket.send(Message.serialize(response));
            } catch (Exception e) {
              _logger.debug("rpc response send failed for message [{}]", response);
              _logger.debug("{}", e);
            }
          }
        }
      } catch (Exception e) {
        printException(e);
      } finally {
        closeSocket(socket);
      }
    }

    /**
     * @param socket
     * @return
     */
    private Message parseMessage(ZMQ.Socket socket) throws Exception {
      byte[] bytes = socket.recv(); //this blocks
      _logger.debug("Received bytes:[{}]", bytes.length);
      return (Message) Message.deserialize(bytes);
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
}
