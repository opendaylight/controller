/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.connector.remoterpc.router.zeromq;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.opendaylight.controller.sal.connector.api.RpcRouter;
import org.opendaylight.controller.sal.connector.remoterpc.router.zeromq.Message.MessageType;
import org.opendaylight.controller.sal.core.api.Broker.ProviderSession;
import org.opendaylight.controller.sal.core.api.Broker.RpcRegistration;
import org.opendaylight.controller.sal.core.api.RpcImplementation;
import org.opendaylight.controller.sal.core.api.RpcRegistrationListener;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.ZMQ;

/**
 * ZeroMq based implementation of RpcRouter
 * TODO:
 *    1. Make it multi VM aware
 *    2. Make rpc request handling async and non-blocking. Note zmq socket is not thread safe
 *    3. sendRpc() should use connection pooling
 *    4. Read properties from config file using existing(?) ODL properties framework
 */
public class ZeroMqRpcRouter implements RpcRouter<QName, QName, InstanceIdentifier, Object> {

  private ExecutorService serverPool;
  private static ExecutorService handlersPool;

  private Map<RpcRouter.RouteIdentifier<QName, QName, InstanceIdentifier>, String> routingTable;

  private ProviderSession brokerSession;

  private ZMQ.Context context;
  private ZMQ.Socket publisher;
  private ZMQ.Socket subscriber;
  private ZMQ.Socket replySocket;

  private static ZeroMqRpcRouter _instance = new ZeroMqRpcRouter();

  private final RpcFacade facade = new RpcFacade();
  private final RpcListener listener = new RpcListener();

  private final String localIp = getLocalIpAddress();

  private String pubPort = System.getProperty("pub.port");// port on which announcements are sent
  private String subPort = System.getProperty("sub.port");// other controller's pub port
  private String pubIp = System.getProperty("pub.ip"); // other controller's ip
  private String rpcPort = System.getProperty("rpc.port");// port on which RPC messages are received

  private Logger _logger = LoggerFactory.getLogger(ZeroMqRpcRouter.class);

  //Prevent instantiation
  private ZeroMqRpcRouter() {
  }

  public static ZeroMqRpcRouter getInstance() {
    return _instance;
  }

  public void start() {
    context = ZMQ.context(2);
    publisher = context.socket(ZMQ.PUB);
    int ret = publisher.bind("tcp://*:" + pubPort);
    // serverPool = Executors.newSingleThreadExecutor();
    serverPool = Executors.newCachedThreadPool();
    handlersPool = Executors.newCachedThreadPool();
    routingTable = new ConcurrentHashMap<RpcRouter.RouteIdentifier<QName, QName, InstanceIdentifier>, String>();

    // Start listening for announce and rpc messages
    serverPool.execute(receive());

    brokerSession.addRpcRegistrationListener(listener);

    Set<QName> currentlySupported = brokerSession.getSupportedRpcs();
    for (QName rpc : currentlySupported) {
      listener.onRpcImplementationAdded(rpc);
    }

  }

  public void stop() {
    if (handlersPool != null)
      handlersPool.shutdown();
    if (serverPool != null)
      serverPool.shutdown();
    if (publisher != null) {
      publisher.setLinger(0);
      publisher.close();
    }
    if (replySocket != null) {
      replySocket.setLinger(0);
      replySocket.close();
    }
    if (subscriber != null) {
      subscriber.setLinger(0);
      subscriber.close();
    }
    if (context != null)
      context.term();

  }

  private Runnable receive() {
    return new Runnable() {
      public void run() {
        try {
          // Bind to RPC reply socket
          replySocket = context.socket(ZMQ.REP);
          replySocket.bind("tcp://*:" + rpcPort);

          // Bind to publishing controller
          subscriber = context.socket(ZMQ.SUB);
          String pubAddress = "tcp://" + pubIp + ":" + subPort;
          subscriber.connect(pubAddress);
          _logger.debug("{} Subscribing at[{}]", Thread.currentThread().getName(), pubAddress);

          //subscribe for announcements
          //TODO: Message type would be changed. Update this
          subscriber.subscribe(Message.serialize(Message.MessageType.ANNOUNCE));

          // Poller enables listening on multiple sockets using a single thread
          ZMQ.Poller poller = new ZMQ.Poller(2);
          poller.register(replySocket, ZMQ.Poller.POLLIN);
          poller.register(subscriber, ZMQ.Poller.POLLIN);

          //TODO: Add code to restart the thread after exception
          while (!Thread.currentThread().isInterrupted()) {

            poller.poll();

            if (poller.pollin(0)) {
              handleRpcCall();
            }
            if (poller.pollin(1)) {
              handleAnnouncement();
            }
          }
        } catch (Exception e) {
          e.printStackTrace();
        }
        replySocket.setLinger(0);
        replySocket.close();
        subscriber.setLinger(0);
        subscriber.close();
      }
    };
  }

  /**
   * @throws IOException
   * @throws ClassNotFoundException
   */
  private void handleAnnouncement() throws IOException, ClassNotFoundException {

    _logger.info("Announcement received");
    Message.MessageType topic = (MessageType) Message.deserialize(subscriber.recv());

    if (subscriber.hasReceiveMore()) {
      try {
        Message m = (Message) Message.deserialize(subscriber.recv());
        _logger.debug("Announcement message [{}]", m);

        // TODO: check on msg type or topic. Both
        // should be same. Need to normalize.
        if (Message.MessageType.ANNOUNCE == m.getType())
          updateRoutingTable(m);
      } catch (IOException | ClassNotFoundException e) {
        e.printStackTrace();
      }
    }

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
          //.payload(result)    TODO: enable and test
          .build();

      replySocket.send(Message.serialize(response));

      _logger.debug("Sent rpc response [{}]", response);

    } catch (IOException ex) {
      //TODO: handle exception and send error codes to caller
      ex.printStackTrace();
    }
  }


  @Override
  public Future<RpcReply<Object>> sendRpc(
      final RpcRequest<QName, QName, InstanceIdentifier, Object> input) {

    return handlersPool.submit(new Callable<RpcReply<Object>>() {

      @Override
      public RpcReply<Object> call() {
        ZMQ.Socket requestSocket = context.socket(ZMQ.REQ);

        // TODO pick the ip and port from routing table based on routing identifier
        requestSocket.connect("tcp://" + pubIp + ":5554");

        Message requestMessage = new Message.MessageBuilder()
            .type(MessageType.REQUEST)
            .sender(localIp + ":" + rpcPort)
            .route(input.getRoutingInformation())
            .payload(input.getPayload())
            .build();

        _logger.debug("Sending rpc request [{}]", requestMessage);

        RpcReply<Object> reply = null;

        try {

          requestSocket.send(Message.serialize(requestMessage));
          final Message response = parseMessage(requestSocket);

          _logger.debug("Received response [{}]", response);

          reply = new RpcReply<Object>() {

            @Override
            public Object getPayload() {
              return response.getPayload();
            }
          };
        } catch (IOException ex) {
          // TODO: Pass exception back to the caller
          ex.printStackTrace();
        }

        return reply;
      }
    });
  }

  /**
   * TODO: Remove this implementation and use RoutingTable implementation to send announcements
   * Publishes a notice to other controllers in the cluster
   *
   * @param notice
   */
  public void publish(final Message notice) {
    Runnable task = new Runnable() {
      public void run() {

        try {

          publisher.sendMore(Message.serialize(Message.MessageType.ANNOUNCE));
          publisher.send(Message.serialize(notice));
          _logger.debug("Announcement sent [{}]", notice);
        } catch (IOException ex) {
          _logger.error("Error in sending announcement [{}]", notice);
          ex.printStackTrace();
        }
      }
    };
    handlersPool.execute(task);
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
   * TODO: Change to use external routing table implementation
   *
   * @param msg
   */
  private void updateRoutingTable(Message msg) {
    routingTable.put(msg.getRoute(), msg.getSender());
    RpcRouter.RouteIdentifier<QName, QName, InstanceIdentifier> route = msg.getRoute();

    // Currently only registers rpc implementation.
    // TODO: do registration for instance based routing
    QName rpcType = route.getType();
    RpcRegistration registration = brokerSession.addRpcImplementation(rpcType, facade);
    _logger.debug("Routing table updated");
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

  private class RpcFacade implements RpcImplementation {

    @Override
    public Set<QName> getSupportedRpcs() {
      return Collections.emptySet();
    }

    @Override
    public RpcResult<CompositeNode> invokeRpc(QName rpc, CompositeNode input) {

      RouteIdentifierImpl routeId = new RouteIdentifierImpl();
      routeId.setType(rpc);

      RpcRequestImpl request = new RpcRequestImpl();
      request.setRouteIdentifier(routeId);
      request.setPayload(input);

      final Future<RpcReply<Object>> ret = sendRpc(request);

      //TODO: Review result handling
      RpcResult<CompositeNode> result = new RpcResult<CompositeNode>() {
        @Override
        public boolean isSuccessful() {
          try {
            ret.get();
          } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            return false;
          }
          return true;
        }

        @Override
        public CompositeNode getResult() {
          return null;
        }

        @Override
        public Collection<RpcError> getErrors() {
          return Collections.EMPTY_LIST;
        }
      };
      return result;
    }
  }

  /**
   * Listener for rpc registrations
   */
  private class RpcListener implements RpcRegistrationListener {

    @Override
    public void onRpcImplementationAdded(QName name) {

      _logger.debug("Announcing registration for [{}]", name);
      RouteIdentifierImpl routeId = new RouteIdentifierImpl();
      routeId.setType(name);

      //TODO: Make notice immutable and change message type
      Message notice = new Message.MessageBuilder()
          .type(MessageType.ANNOUNCE)
          .sender("tcp://" + localIp + ":" + rpcPort)
          .route(routeId)
          .build();

      publish(notice);
    }

    @Override
    public void onRpcImplementationRemoved(QName name) {
      // TODO: send a rpc-deregistrtation notice

    }
  }

  public void setBrokerSession(ProviderSession session) {
    this.brokerSession = session;

  }

}
