package org.opendaylight.controller.sal.connector.zeromq;

import org.opendaylight.controller.sal.connector.api.RpcRouter;
import org.opendaylight.controller.sal.core.api.Broker.ProviderSession;
import org.opendaylight.controller.sal.core.api.Broker.RpcRegistration;
import org.opendaylight.controller.sal.core.api.RpcImplementation;
import org.opendaylight.controller.sal.core.api.RpcRegistrationListener;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.zeromq.ZMQ;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.*;
import java.util.concurrent.*;

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

  private String pubPort = System.getProperty("pub.port");//port on which announcements are sent
  private String subPort = System.getProperty("sub.port");//other controller's pub port
  private String pubIp = System.getProperty("pub.ip");    //other controller's ip
  private String rpcPort = System.getProperty("rpc.port");//port on which RPC messages are received


  private ZeroMqRpcRouter() {
  }

  public static ZeroMqRpcRouter getInstance() {
    return _instance;
  }

  public void start() {
    context = ZMQ.context(2);
    serverPool = Executors.newSingleThreadExecutor();
    handlersPool = Executors.newCachedThreadPool();
    routingTable = new ConcurrentHashMap<RpcRouter.RouteIdentifier<QName, QName, InstanceIdentifier>, String>();

    // Start listening for announce and rpc messages
    serverPool.execute(receive());

    
    brokerSession.addRpcRegistrationListener(listener);
    Set<QName> currentlySupported = brokerSession.getSupportedRpcs();
    for(QName rpc : currentlySupported) {
        listener.onRpcImplementationAdded(rpc);
    }


  }

  public void stop() {
    if (handlersPool != null) handlersPool.shutdown();
    if (serverPool != null) serverPool.shutdown();
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
    if (context != null) context.term();


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
          subscriber.connect("tcp://" + pubIp + ":" + subPort);
          System.out.println("Subscribing at[" + "tcp://" + pubIp + ":" + subPort + "]");

          subscriber.subscribe(Message.serialize(Message.MessageType.ANNOUNCE));

          // Initialize poll set
          ZMQ.Poller poller = new ZMQ.Poller(2);
          poller.register(replySocket, ZMQ.Poller.POLLIN);
          poller.register(subscriber, ZMQ.Poller.POLLIN);

          while (!Thread.currentThread().isInterrupted()) {

            poller.poll(250);
            //TODO: Fix this
            if (poller.pollin(0)) {
              //receive rpc request and reply
              try {
                Message req = parseMessage(replySocket);
                Message resp = new Message();
                //Call broker to process the message then reply
                Future<RpcResult<CompositeNode>> rpc = brokerSession.rpc((QName) req.getRoute().getType(), (CompositeNode) req.getPayload());
                RpcResult<CompositeNode> result = rpc.get();
                resp.setType(Message.MessageType.RESPONSE);
                resp.setSender(getLocalIpAddress() + ":" + rpcPort);
                resp.setRoute(req.getRoute());
                resp.setPayload(result.isSuccessful());
                replySocket.send(Message.serialize(resp));

              } catch (IOException ex) {// | ClassNotFoundException ex) {
                System.out.println("Rpc request could not be handled" + ex);
              }
            }
            if (poller.pollin(1)) {
              //get subscription and update routing table
              //try {
              Message.MessageType topic = (Message.MessageType)Message.deserialize(subscriber.recv());
              System.out.println("Topic:[" + topic + "]");

              if (subscriber.hasReceiveMore()) {
                try {
                  Message m = (Message) Message.deserialize(subscriber.recv());
                  System.out.println(m);
                  //TODO: check on msg type or topic. Both should be same. Need to normalize.
                  if (Message.MessageType.ANNOUNCE == m.getType()) updateRoutingTable(m);
                } catch (IOException | ClassNotFoundException e) {
                  e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                }
              }
//
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

  private void updateRoutingTable(Message msg) {
    routingTable.put(msg.getRoute(), msg.getSender());
    RpcRouter.RouteIdentifier<QName, QName, InstanceIdentifier> route = msg.getRoute();
    QName rpcType = route.getType();
    System.out.println("Routing Table\n" + routingTable);

    RpcRegistration registration = brokerSession.addRpcImplementation(rpcType, facade);
  }

  private Message parseMessage(ZMQ.Socket socket) {
    //Message m = new Message();
    //socket.setReceiveBufferSize(40000);
    Message msg = null;
    try {
      byte[] bytes = socket.recv();
      System.out.println("Received bytes:[" + bytes.length + "]");
      msg = (Message) Message.deserialize(bytes);
    } catch (Throwable t) {
      System.out.println("Caught exception");
      t.printStackTrace();
    }
    return msg;
    /*m.setType((Message.MessageType) Message.deserialize(socket.recv()));

    if (socket.hasReceiveMore()) {
      m.setSender((String) Message.deserialize(socket.recv()));
    }
    if (socket.hasReceiveMore()) {
      m.setRoute((RouteIdentifier) Message.deserialize(socket.recv()));
    }
    if (socket.hasReceiveMore()) {
      m.setPayload(Message.deserialize(socket.recv()));
    }
    return m;*/
  }

  @Override
  public Future<RpcReply<Object>> sendRpc(final RpcRequest<QName, QName, InstanceIdentifier, Object> input) {

    return handlersPool.submit(new Callable<RpcReply<Object>>() {

      @Override
      public RpcReply<Object> call() {
        ZMQ.Socket requestSocket = context.socket(ZMQ.REQ);
        Message req = new Message();
        Message resp = null;
        RpcReplyImpl reply = new RpcReplyImpl();
        requestSocket.connect((String) routingTable.get(input.getRoutingInformation().getRoute()));

        req.setType(Message.MessageType.REQUEST);
        req.setSender(getLocalIpAddress() + ":" + rpcPort);
        req.setRoute(input.getRoutingInformation());
        req.setPayload(input.getPayload());
        try {
          requestSocket.send(Message.serialize(req));
          resp = parseMessage(requestSocket);
          reply.setPayload(resp.getPayload());
        } catch (IOException ex) {//| ClassNotFoundException ex) {
          //Log and ignore
          System.out.println("Error in RPC send. Input could not be serialized[" + input + "]");
        }

        return reply;
      }
    });
  }

  public void publish(final Message message) {
    Runnable task = new Runnable() {
      public void run() {
        // Bind to publishing port
        publisher = context.socket(ZMQ.PUB);
        publisher.bind("tcp://*:" + pubPort);
        System.out.println("Publisher started at port[" + pubPort + "]");
        try {
          Message outMessage =  new Message();
          outMessage.setType(Message.MessageType.ANNOUNCE);
          outMessage.setSender("tcp://" + getLocalIpAddress() + ":" + rpcPort);
          outMessage.setRoute(message.getRoute());

          System.out.println("Sending announcement[" + outMessage + "]");
          publisher.sendMore(Message.serialize(Message.MessageType.ANNOUNCE));
          publisher.send(Message.serialize(outMessage));

        } catch (IOException ex) {
          //Log and ignore
          System.out.println("Error in publishing");
          ex.printStackTrace();
        }
        System.out.println("Published message[" + message + "]");
        publisher.close();
      }
    };
    handlersPool.execute(task);
  }

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


  private class RpcFacade implements RpcImplementation {


    @Override
    public Set<QName> getSupportedRpcs() {
      return Collections.emptySet();
    }

    @Override
    public RpcResult<CompositeNode> invokeRpc(QName rpc, CompositeNode input) {

      RpcRequestImpl request = new RpcRequestImpl();
      RouteIdentifierImpl routeId = new RouteIdentifierImpl();
      routeId.setContext(null);
      routeId.setRoute(null);
      routeId.setType(rpc);

      request.setRouteIdentifier(routeId);
      request.setPayload(input);
      // Create message

      Future<org.opendaylight.controller.sal.connector.api.RpcRouter.RpcReply<Object>> ret = sendRpc(request);

      return null;
    }
  }

  private class RpcListener implements RpcRegistrationListener {

    @Override
    public void onRpcImplementationAdded(QName name) {

      Message msg = new Message();
      RouteIdentifierImpl routeId = new RouteIdentifierImpl();
      routeId.setType(name);
      msg.setRoute(routeId);
      publish(msg);
    }

    @Override
    public void onRpcImplementationRemoved(QName name) {
      // TODO Auto-generated method stub

    }
  }

  public void setBrokerSession(ProviderSession session) {
    this.brokerSession = session;

  }

}
