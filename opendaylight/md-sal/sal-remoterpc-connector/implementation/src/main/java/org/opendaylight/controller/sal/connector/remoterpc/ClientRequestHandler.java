/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.connector.remoterpc;

import com.google.common.base.Preconditions;
import org.opendaylight.controller.sal.connector.remoterpc.dto.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.ZMQ;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 *
 */
class ClientRequestHandler implements AutoCloseable{

  private Logger _logger = LoggerFactory.getLogger(ClientRequestHandler.class);
  private final String DEFAULT_NAME = "remoterpc-client-worker";
  private final String INPROC_PROTOCOL_PREFIX = "inproc://";
  private final String TCP_PROTOCOL_PREFIX = "tcp://";

  private ZMQ.Context context;

  /*
   * Worker thread pool. Each thread runs a ROUTER-DEALER pair
   */
  private ExecutorService workerPool;

  /*
   * Set of remote servers this client is currently connected to
   */
  private Map<String, String> connectedServers;

  protected ClientRequestHandler(ZMQ.Context context) {
    this.context = context;
    connectedServers = new ConcurrentHashMap<String, String>();
    start();
  }

  /**
   * Starts a pool of worker as needed. A worker thread that has not been used for 5 min
   * is terminated and removed from the pool. If thread dies due to an exception, its
   * restarted.
   */
  private void start(){

    workerPool = new ThreadPoolExecutor(0, Integer.MAX_VALUE,
        5L, TimeUnit.MINUTES,
        new SynchronousQueue<Runnable>()){

      @Override
      protected void afterExecute(Runnable r, Throwable t) {
        if (isTerminating() || isTerminated() || isShutdown())
          return;

        Worker worker = (Worker) r;
        Preconditions.checkState( worker != null );
        String remoteServerAddress = worker.getRemoteServerAddress();
        connectedServers.remove(remoteServerAddress);

        if ( t != null ){
          _logger.debug("Exception caught while terminating worker [{},{}]. " +
              "Restarting worker...", t.getClass(), t.getMessage());

          connectedServers.put(remoteServerAddress, remoteServerAddress);
          this.execute(r);
        }
        super.afterExecute(r, null);
      }
    };
  }

  public Message handle(Message request) throws IOException, ClassNotFoundException, InterruptedException {

    String remoteServerAddress = request.getRecipient();
    //if we already have router-dealer bridge setup for this address the send request
    //otherwise first create the bridge and then send request
    if ( connectedServers.containsKey(remoteServerAddress) )
      return sendMessage(request, remoteServerAddress);
    else{
      workerPool.execute(new Worker(remoteServerAddress));
      connectedServers.put(remoteServerAddress, remoteServerAddress);
      //give little time for sockets to get initialized
      //TODO: Add socket ping-pong message to ensure socket init rather than thread.sleep.
      Thread.sleep(1000);
      return sendMessage(request, remoteServerAddress);
    }
  }

  private Message sendMessage(Message request, String address) throws IOException, ClassNotFoundException {
    Message response = null;
    ZMQ.Socket socket = context.socket(ZMQ.REQ);

    try {
      socket.connect( INPROC_PROTOCOL_PREFIX + address);
      socket.send(Message.serialize(request));
      _logger.debug("Request sent. Waiting for reply...");
      byte[] reply = socket.recv(0);
      _logger.debug("Response received");
      response = (Message) Message.deserialize(reply);
    } finally {
      socket.close();
    }
    return response;
  }

  /**
   * This gets called automatically if used with try-with-resources
   */
  @Override
  public void close(){
    workerPool.shutdown();
    _logger.info("Request Handler closed");
  }

  /**
   * Total number of workers in the pool. Number of workers represent
   * number of remote servers {@link org.opendaylight.controller.sal.connector.remoterpc.ClientImpl} is connected to.
   *
   * @return worker count
   */
  public int getWorkerCount(){

    if (workerPool == null) return 0;

    return ((ThreadPoolExecutor)workerPool).getActiveCount();
  }
  /**
   * Handles RPC request
   */
  private class Worker implements Runnable {
    private String name;
    private String remoteServer;  //<servername:rpc-port>

    public Worker(String address){
      this.name = DEFAULT_NAME + "[" + address + "]";
      this.remoteServer = address;
    }

    public String getRemoteServerAddress(){
      return this.remoteServer;
    }

    @Override
    public void run() {
      Thread.currentThread().setName(name);
      _logger.debug("Starting ... ");

      ZMQ.Socket router = context.socket(ZMQ.ROUTER);
      ZMQ.Socket dealer = context.socket(ZMQ.DEALER);

      try {
        int success = router.bind(INPROC_PROTOCOL_PREFIX + remoteServer);
        Preconditions.checkState(-1 != success, "Could not bind to " + remoteServer);

        dealer.connect(TCP_PROTOCOL_PREFIX + remoteServer);

        _logger.info("Worker started for [{}]", remoteServer);

        //TODO: Add capture handler
        //This code will block until the zmq context is terminated.
        ZMQ.proxy(router, dealer, null);

      } catch (Exception e) {
        _logger.debug("Ignoring exception [{}, {}]", e.getClass(), e.getMessage());
      } finally {
        try {
          router.close();
          dealer.close();
        } catch (Exception x) {
          _logger.debug("Exception while closing socket [{}]", x);
        }
        _logger.debug("Closing...");
      }
    }
  }
}
