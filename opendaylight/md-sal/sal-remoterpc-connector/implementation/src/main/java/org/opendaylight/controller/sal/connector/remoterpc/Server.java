/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.connector.remoterpc;

import org.opendaylight.controller.sal.connector.remoterpc.api.RouteChangeListener;
import org.opendaylight.controller.sal.connector.remoterpc.api.RoutingTable;
import org.opendaylight.controller.sal.connector.remoterpc.api.RoutingTableException;
import org.opendaylight.controller.sal.connector.remoterpc.api.SystemException;
import org.opendaylight.controller.sal.connector.remoterpc.dto.Message;
import org.opendaylight.controller.sal.connector.remoterpc.dto.Message.MessageType;
import org.opendaylight.controller.sal.connector.remoterpc.dto.RouteIdentifierImpl;
import org.opendaylight.controller.sal.core.api.Broker.ProviderSession;
import org.opendaylight.controller.sal.core.api.RpcRegistrationListener;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.ZMQ;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * ZeroMq based implementation of RpcRouter
 * TODO:
 * 1. Make rpc request handling async and non-blocking. Note zmq socket is not thread safe
 * 2. Read properties from config file using existing(?) ODL properties framework
 */
public class Server  implements RouteChangeListener<String, Set>{

  private Logger _logger = LoggerFactory.getLogger(Server.class);

  private ExecutorService serverPool;
  //private RoutingTable<RpcRouter.RouteIdentifier, String> routingTable;
  private RoutingTable<String, String> routingTable;
  private Set<QName> remoteServices;
  private ProviderSession brokerSession;
  private ZMQ.Context context;
  private ZMQ.Socket replySocket;

  private static Server _instance = new Server();
  private final RpcListener listener = new RpcListener();

  private final String localIp = getLocalIpAddress();

  // port on which rpc messages are received
  private String rpcPort =
      (System.getProperty("rpc.port") != null) ? System.getProperty("rpc.port") : "5554";

  //Prevent instantiation
  private Server() {/*NOOPS*/}

  public static Server getInstance() {
    return _instance;
  }

  public RoutingTable<String, String> getRoutingTable(){
    return this.routingTable;
  }

  public void setRoutingTable(RoutingTable<String, String> routingTable) {
    this.routingTable = routingTable;
  }

  public void setBrokerSession(ProviderSession session) {
    this.brokerSession = session;
  }

  public void start() {
    context = ZMQ.context(1);
    serverPool = Executors.newSingleThreadExecutor();
    remoteServices = new HashSet<QName>();

    // Start listening rpc requests
    serverPool.execute(receive());

    _logger.debug("Start listening for RPC registrations");
    brokerSession.addRpcRegistrationListener(listener);
    //routingTable.registerRouteChangeListener(routeChangeListener);

    Set<QName> currentlySupported = brokerSession.getSupportedRpcs();
    for (QName rpc : currentlySupported) {
      listener.onRpcImplementationAdded(rpc);
    }
  }

  public void stop() {
    //TODO: un-subscribe

//    if (context != null)
//      context.term();
//
//    _logger.debug("ZMQ Context is terminated.");

    if (serverPool != null)
      serverPool.shutdown();

    _logger.debug("Thread pool is closed.");
  }

  private Runnable receive() {
    return new Runnable() {
      public void run() {

        // Bind to RPC reply socket
        replySocket = context.socket(ZMQ.REP);
        replySocket.bind("tcp://*:" + rpcPort);

        // Poller enables listening on multiple sockets using a single thread
        ZMQ.Poller poller = new ZMQ.Poller(1);
        poller.register(replySocket, ZMQ.Poller.POLLIN);
        try {
          //TODO: Add code to restart the thread after exception
          while (!Thread.currentThread().isInterrupted()) {

            poller.poll();

            if (poller.pollin(0)) {
              handleRpcCall();
            }
          }
        } catch (Exception e) {
          //log and continue
          _logger.error("Unhandled exception [{}]", e);
        } finally {
          poller.unregister(replySocket);
          replySocket.close();
        }

      }
    };
  }

  /**
   * @throws InterruptedException
   * @throws ExecutionException
   */
  private void handleRpcCall() throws InterruptedException, ExecutionException {
    try {
      Message request = parseMessage(replySocket);

      _logger.debug("Received rpc request [{}]", request);

      // Call broker to process the message then reply
      Future<RpcResult<CompositeNode>> rpc = brokerSession.rpc(
          (QName) request.getRoute().getType(), (CompositeNode) request.getPayload());

      RpcResult<CompositeNode> result = rpc.get();

      Message response = new Message.MessageBuilder()
          .type(MessageType.RESPONSE)
          .sender(localIp + ":" + rpcPort)
          .route(request.getRoute())
          .payload(result.getResult())
          .build();

      _logger.debug("Sending response [{}]", response);
      replySocket.send(Message.serialize(response));

      _logger.debug("Sent rpc response [{}]", response);

    } catch (IOException ex) {
      //TODO: handle exception and send error codes to caller
      ex.printStackTrace();
    }
  }

  /**
   * Finds IPv4 address of the local VM
   * TODO: This method is non-deterministic. There may be more than one IPv4 address. Cant say which
   * address will be returned. Read IP from a property file or enhance the code to make it deterministic.
   * Should we use IP or hostname?
   *
   * @return
   */
  private String getLocalIpAddress() {
    String hostAddress = null;
    Enumeration e = null;
    try {
      e = NetworkInterface.getNetworkInterfaces();
    } catch (SocketException e1) {
      e1.printStackTrace();
    }
    while (e.hasMoreElements()) {

      NetworkInterface n = (NetworkInterface) e.nextElement();

      Enumeration ee = n.getInetAddresses();
      while (ee.hasMoreElements()) {
        InetAddress i = (InetAddress) ee.nextElement();
        if ((i instanceof Inet4Address) && (i.isSiteLocalAddress()))
          hostAddress = i.getHostAddress();
      }
    }
    return hostAddress;

  }

  /**
   * @param socket
   * @return
   */
  private Message parseMessage(ZMQ.Socket socket) {

    Message msg = null;
    try {
      byte[] bytes = socket.recv();
      _logger.debug("Received bytes:[{}]", bytes.length);
      msg = (Message) Message.deserialize(bytes);
    } catch (Throwable t) {
      t.printStackTrace();
    }
    return msg;
  }

  @Override
  public void onRouteUpdated(String key, Set values) {
    RouteIdentifierImpl rId = new RouteIdentifierImpl();
    try{
      _logger.debug("Updating key/value {}-{}", key, values);
      brokerSession.addRpcImplementation(
          (QName) rId.fromString(key).getType(), Client.getInstance());

    }catch(Exception e){
      _logger.info("Route update failed {}", e);
    }
  }

  @Override
  public void onRouteDeleted(String key) {
    //TODO: Broker session needs to be updated to support this
    throw new UnsupportedOperationException();
  }

  /**
   * Listener for rpc registrations
   */
  private class RpcListener implements RpcRegistrationListener {

    @Override
    public void onRpcImplementationAdded(QName name) {

      //if the service name exists in the set, this notice
      //has bounced back from the broker. It should be ignored
      if (remoteServices.contains(name))
        return;

      _logger.debug("Adding registration for [{}]", name);
      RouteIdentifierImpl routeId = new RouteIdentifierImpl();
      routeId.setType(name);
      String address = "tcp://" + localIp + ":" + rpcPort;

      try {
        routingTable.addGlobalRoute(routeId.toString(), address);
        _logger.debug("Route added [{}-{}]", name, address);

      } catch (RoutingTableException | SystemException e) {
        //TODO: This can be thrown when route already exists in the table. Broker
        //needs to handle this.
        _logger.error("Unhandled exception while adding global route to routing table [{}]", e);

      }
    }

    @Override
    public void onRpcImplementationRemoved(QName name) {

      _logger.debug("Removing registration for [{}]", name);
      RouteIdentifierImpl routeId = new RouteIdentifierImpl();
      routeId.setType(name);

      try{
        routingTable.removeGlobalRoute(routeId.toString());
      }catch(Exception e){
        _logger.info("Route delete failed {}", e);
      }
    }
  }

}
