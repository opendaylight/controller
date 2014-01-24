/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.connector.remoterpc;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.Sets;
import org.opendaylight.controller.md.sal.common.api.routing.RouteChange;
import org.opendaylight.controller.sal.connector.remoterpc.api.RouteChangeListener;
import org.opendaylight.controller.sal.connector.remoterpc.api.RoutingTable;
import org.opendaylight.controller.sal.connector.remoterpc.api.RoutingTableException;
import org.opendaylight.controller.sal.connector.remoterpc.api.SystemException;
import org.opendaylight.controller.sal.connector.remoterpc.dto.RouteIdentifierImpl;
import org.opendaylight.controller.sal.core.api.Broker.ProviderSession;
import org.opendaylight.controller.sal.core.api.RpcRegistrationListener;
import org.opendaylight.controller.sal.core.api.RpcRoutingContext;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.ZMQ;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * ZeroMq based implementation of RpcRouter. It implements RouteChangeListener of RoutingTable
 * so that it gets route change notifications from routing table.
 */
public class ServerImpl implements RemoteRpcServer, RouteChangeListener<String, String> {

  private Logger _logger = LoggerFactory.getLogger(ServerImpl.class);

  private ExecutorService serverPool;
  protected ServerRequestHandler handler;

  private Set<QName> remoteServices;
  private ProviderSession brokerSession;
  private ZMQ.Context context;

  private final RpcListener listener = new RpcListener();

  private final String HANDLER_INPROC_ADDRESS = "inproc://rpc-request-handler";
  private final int HANDLER_WORKER_COUNT = 2;
  private final int HWM = 200;//high water mark on sockets
  private volatile State status = State.STOPPED;

  private String serverAddress;
  private int port;

  private ClientImpl client;

  private  RoutingTableProvider routingTableProvider;

  public static enum State {
    STARTING, STARTED, STOPPED;
  }

  public ServerImpl(int port) {
    this.port = port;
    this.serverAddress = new StringBuilder(findIpAddress()).
                              append(":").
                              append(port).
                              toString();
  }

  public RoutingTableProvider getRoutingTableProvider() {
    return routingTableProvider;
  }

  public void setRoutingTableProvider(RoutingTableProvider routingTableProvider) {
    this.routingTableProvider = routingTableProvider;
  }

  public ClientImpl getClient(){
    return this.client;
  }

  public void setClient(ClientImpl client) {
    this.client = client;
  }

  public State getStatus() {
    return this.status;
  }

  public Optional<ServerRequestHandler> getHandler() {
    return Optional.fromNullable(this.handler);
  }

  public void setBrokerSession(ProviderSession session) {
    this.brokerSession = session;
  }

  public Optional<ProviderSession> getBrokerSession() {
    return Optional.fromNullable(this.brokerSession);
  }

  public Optional<ZMQ.Context> getZmqContext() {
    return Optional.fromNullable(this.context);
  }

  public String getServerAddress() {
    return serverAddress;
  }

  public String getHandlerAddress() {
    return HANDLER_INPROC_ADDRESS;
  }

  /**
   *
   */
  public void start() {
    Preconditions.checkState(State.STOPPED == this.getStatus(),
        "Remote RPC Server is already running");

    status = State.STARTING;
    context = ZMQ.context(1);
    remoteServices = new HashSet<QName>();//
    serverPool = Executors.newSingleThreadExecutor();//main server thread
    serverPool.execute(receive()); // Start listening rpc requests
    brokerSession.addRpcRegistrationListener(listener);

    announceLocalRpcs();

    registerRemoteRpcs();

    status = State.STARTED;
    _logger.info("Remote RPC Server started [{}]", getServerAddress());
  }

  public void stop(){
    close();
  }

  /**
   *
   */
  @Override
  public void close() {

    if (State.STOPPED == this.getStatus()) return; //do nothing

    unregisterLocalRpcs();

    if (serverPool != null)
      serverPool.shutdown();

    closeZmqContext();

    status = State.STOPPED;
    _logger.info("Remote RPC Server stopped");
  }

  /**
   * Closes ZMQ Context. It tries to gracefully terminate the context. If
   * termination takes more than a second, its forcefully shutdown.
   */
  private void closeZmqContext() {
    ExecutorService exec = Executors.newSingleThreadExecutor();
    FutureTask zmqTermination = new FutureTask(new Runnable() {

      @Override
      public void run() {
        try {
          if (context != null)
            context.term();
          _logger.debug("ZMQ Context terminated gracefully!");
        } catch (Exception e) {
          _logger.debug("ZMQ Context termination threw exception [{}]. Continuing shutdown...", e);
        }
      }
    }, null);

    exec.execute(zmqTermination);

    try {
      zmqTermination.get(5L, TimeUnit.SECONDS);
    } catch (Exception e) {/*ignore and continue with shutdown*/}

    exec.shutdownNow();
  }

  /**
   * Main listener thread that spawns {@link ServerRequestHandler} as workers.
   *
   * @return
   */
  private Runnable receive() {
    return new Runnable() {

      @Override
      public void run() {
        Thread.currentThread().setName("remote-rpc-server");
        _logger.debug("Remote RPC Server main thread starting...");

        //socket clients connect to (frontend)
        ZMQ.Socket clients = context.socket(ZMQ.ROUTER);

        //socket RequestHandlers connect to (backend)
        ZMQ.Socket workers = context.socket(ZMQ.DEALER);

        try (SocketPair capturePair = new SocketPair();
             ServerRequestHandler requestHandler = new ServerRequestHandler(context,
                 brokerSession,
                 HANDLER_WORKER_COUNT,
                 HANDLER_INPROC_ADDRESS,
                 getServerAddress());) {

          handler = requestHandler;
          clients.setHWM(HWM);
          clients.bind("tcp://*:" + port);
          workers.setHWM(HWM);
          workers.bind(HANDLER_INPROC_ADDRESS);
          //start worker threads
          _logger.debug("Remote RPC Server worker threads starting...");
          requestHandler.start();
          //start capture thread
          // handlerPool.execute(new CaptureHandler(capturePair.getReceiver()));
          //  Connect work threads to client threads via a queue
          ZMQ.proxy(clients, workers, null);//capturePair.getSender());

        } catch (Exception e) {
          _logger.debug("Unhandled exception [{}, {}]", e.getClass(), e.getMessage());
        } finally {
          if (clients != null) clients.close();
          if (workers != null) workers.close();
          _logger.info("Remote RPC Server stopped");
        }
      }
    };
  }

  /**
   * Register the remote RPCs from the routing table into broker
   */
  private void registerRemoteRpcs(){
    Optional<RoutingTable<String, String>> routingTableOptional = routingTableProvider.getRoutingTable();

    Preconditions.checkState(routingTableOptional.isPresent(), "Routing table is absent");

    Set<Map.Entry> remoteRoutes =
            routingTableProvider.getRoutingTable().get().getAllRoutes();

    //filter out all entries that contains local address
    //we dont want to register local RPCs as remote
    Predicate<Map.Entry> notLocalAddressFilter = new Predicate<Map.Entry>(){
      public boolean apply(Map.Entry remoteRoute){
        return !getServerAddress().equalsIgnoreCase((String)remoteRoute.getValue());
      }
    };

    //filter the entries created by current node
    Set<Map.Entry> filteredRemoteRoutes = Sets.filter(remoteRoutes, notLocalAddressFilter);

    for (Map.Entry route : filteredRemoteRoutes){
      onRouteUpdated((String) route.getKey(), "");//value is not needed by broker
    }
  }

  /**
   * Un-Register the local RPCs from the routing table
   */
  private void unregisterLocalRpcs(){
    Set<QName> currentlySupported = brokerSession.getSupportedRpcs();
    for (QName rpc : currentlySupported) {
      listener.onRpcImplementationRemoved(rpc);
    }
  }

  /**
   * Publish all the locally registered RPCs in the routing table
   */
  private void announceLocalRpcs(){
    Set<QName> currentlySupported = brokerSession.getSupportedRpcs();
    for (QName rpc : currentlySupported) {
      listener.onRpcImplementationAdded(rpc);
    }
  }

  /**
   * @param key
   * @param value
   */
  @Override
  public void onRouteUpdated(String key, String value) {
    RouteIdentifierImpl rId = new RouteIdentifierImpl();
    try {
      _logger.debug("Updating key/value {}-{}", key, value);
      brokerSession.addRpcImplementation(
          (QName) rId.fromString(key).getType(), client);

      //TODO: Check with Tony for routed rpc
      //brokerSession.addRoutedRpcImplementation((QName) rId.fromString(key).getRoute(), client);
    } catch (Exception e) {
      _logger.info("Route update failed {}", e);
    }
  }

  /**
   * @param key
   */
  @Override
  public void onRouteDeleted(String key) {
    //TODO: Broker session needs to be updated to support this
    throw new UnsupportedOperationException();
  }

  /**
   * Finds IPv4 address of the local VM
   * TODO: This method is non-deterministic. There may be more than one IPv4 address. Cant say which
   * address will be returned. Read IP from a property file or enhance the code to make it deterministic.
   * Should we use IP or hostname?
   *
   * @return
   */
  private String findIpAddress() {
    Enumeration e = null;
    try {
      e = NetworkInterface.getNetworkInterfaces();
    } catch (SocketException e1) {
      _logger.error("Failed to get list of interfaces", e1);
      throw new RuntimeException("Failed to acquire list of interfaces", e1);
    }
    while (e.hasMoreElements()) {

      NetworkInterface n = (NetworkInterface) e.nextElement();

      Enumeration ee = n.getInetAddresses();
      while (ee.hasMoreElements()) {
        InetAddress i = (InetAddress) ee.nextElement();
        _logger.debug("Trying address {}", i);
        if ((i instanceof Inet4Address) && (i.isSiteLocalAddress())) {
          String hostAddress = i.getHostAddress();
          _logger.debug("Settled on host address {}", hostAddress);
          return hostAddress;
        }
      }
    }

    _logger.error("Failed to find a suitable host address");
    return null;
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

      RoutingTable<String, String> routingTable = getRoutingTable();

      try {
        routingTable.addGlobalRoute(routeId.toString(), getServerAddress());
        _logger.debug("Route added [{}-{}]", name, getServerAddress());

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

      RoutingTable<String, String> routingTable = getRoutingTable();

      try {
        routingTable.removeGlobalRoute(routeId.toString());
      } catch (RoutingTableException | SystemException e) {
        _logger.error("Route delete failed {}", e);
      }
    }

    private RoutingTable<String, String> getRoutingTable(){
      Optional<RoutingTable<String, String>> routingTable =
          routingTableProvider.getRoutingTable();

      checkNotNull(routingTable.isPresent(), "Routing table is null");

      return routingTable.get();
    }
  }

  /*
   * Listener for Route changes in broker. Broker notifies this listener in the event
   * of any change (add/delete). Listener then updates the routing table.
   */
  private class BrokerRouteChangeListener
      implements org.opendaylight.controller.md.sal.common.api.routing.RouteChangeListener<RpcRoutingContext, InstanceIdentifier>{

    @Override
    public void onRouteChange(RouteChange<RpcRoutingContext, InstanceIdentifier> routeChange) {

    }
  }

}
