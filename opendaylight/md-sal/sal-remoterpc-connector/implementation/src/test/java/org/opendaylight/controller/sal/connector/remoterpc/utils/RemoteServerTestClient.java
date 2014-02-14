package org.opendaylight.controller.sal.connector.remoterpc.utils;

import org.opendaylight.controller.sal.connector.api.RpcRouter;
import org.opendaylight.controller.sal.connector.remoterpc.dto.Message;
import org.opendaylight.controller.sal.connector.remoterpc.util.XmlUtils;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.zeromq.ZMQ;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RemoteServerTestClient {



  public static void main(String args[]) throws Exception{
    String serverAddress = "tcp://10.195.128.108:5666";
    ZMQ.Context ctx = ZMQ.context(1);
    ExecutorService executor = Executors.newSingleThreadExecutor();
    RemoteServerTestClient client = new RemoteServerTestClient();
    executor.execute(
        MessagingUtil.sendAMessage(ctx, serverAddress, client.createPingMessage(serverAddress))
    );
    MessagingUtil.sendAMessage(ctx, serverAddress, client.createPingMessage(serverAddress));

    Thread.sleep(5000);
    MessagingUtil.closeZmqContext(ctx);
    executor.shutdown();
  }

  public Message createPingMessage(String serverAddress){
    Message ping = new Message.MessageBuilder()
        .type(Message.MessageType.PING)
        .sender("localhost:5444")
        .recipient(serverAddress)
        .build();

    return ping;
  }
  public Message createAddFlowMessage(String serverAddress ){

    RpcRouter.RouteIdentifier routeIdentifier = getAddFlowRpcIdentifier();

    Message addFlow = new Message.MessageBuilder()
        .type(Message.MessageType.REQUEST)
        .sender("localhost:5444")
        .recipient(serverAddress)
        .route(routeIdentifier)
        .payload(getAddFlowPayload(1,1))
        .build();

    return addFlow;
  }

  private RpcRouter.RouteIdentifier getAddFlowRpcIdentifier(){
    throw new UnsupportedOperationException();
  }

  private CompositeNode getAddFlowPayload(int flowId, int tableId){
    final String xml =
    "<flow xmlns=\"urn:opendaylight:flow:inventory\">"
    + "<priority>5</priority>"
    + "<flow-name>Foo</flow-name>"
    + "<match>"
    + "<ethernet-match>"
    + "<ethernet-type>"
    + "<type>2048</type>"
    + "</ethernet-type>"
    + "</ethernet-match>"
    + "<ipv4-destination>10.0.10.2/24</ipv4-destination>"
    + "</match>"
    + "<id>" + flowId + "</id>"
    + "<table_id>" + tableId + "</table_id>"
    + "<instructions>"
    + "<instruction>"
    + "<order>0</order>"
    + "<apply-actions>"
    + "<action>"
    + "<order>0</order>"
    + "<dec-nw-ttl/>"
    + "</action>"
    + "</apply-actions>"
    + "</instruction>"
    + "</instructions>"
    + "</flow>";

    return XmlUtils.xmlToCompositeNode(xml);
  }
}
