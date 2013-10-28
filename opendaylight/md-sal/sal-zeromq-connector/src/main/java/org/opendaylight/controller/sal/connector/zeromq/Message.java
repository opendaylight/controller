package org.opendaylight.controller.sal.connector.zeromq;


import org.codehaus.jackson.map.ObjectMapper;
import org.opendaylight.controller.sal.connector.api.RpcRouter;

import java.io.*;
import java.util.Arrays;

public class Message implements Serializable {

 public enum MessageType {
    ANNOUNCE((byte) 0),
    HEARTBEAT((byte) 1),
    REQUEST((byte) 2),
    RESPONSE((byte) 3);

    private final byte type;

    MessageType(byte type) {
      this.type = type;
    }

    public byte getType(){
      return this.type;
    }
  }

  private MessageType type;
  private String sender;
  private RpcRouter.RouteIdentifier route;
  private Object payload;

  public MessageType getType() {
    return type;
  }

  public void setType(MessageType type) {
    this.type = type;
  }

  public String getSender() {
    return sender;
  }

  public void setSender(String sender) {
    this.sender = sender;
  }

  public RpcRouter.RouteIdentifier getRoute() {
    return route;
  }

  public void setRoute(RpcRouter.RouteIdentifier route) {
    this.route = route;
  }

  public Object getPayload() {
    return payload;
  }

  public void setPayload(Object payload) {
    this.payload = payload;
  }

  @Override
  public String toString() {
    return "Message{" +
        "type=" + type +
        ", sender='" + sender + '\'' +
        ", route=" + route +
        ", payload=" + payload +
        '}';
  }

  /**
   * Converts any {@link Serializable} object to byte[]
   *
   * @param obj
   * @return
   * @throws IOException
   */
  public static byte[] serialize(Object obj) throws IOException {
    ByteArrayOutputStream b = new ByteArrayOutputStream();
    ObjectOutputStream o = new ObjectOutputStream(b);
    o.writeObject(obj);
    return b.toByteArray();
  }

  /**
   * Converts byte[] to a java object
   *
   * @param bytes
   * @return
   * @throws IOException
   * @throws ClassNotFoundException
   */
  public static Object deserialize(byte[] bytes) throws IOException, ClassNotFoundException {
    ByteArrayInputStream b = new ByteArrayInputStream(bytes);
    ObjectInputStream o = new ObjectInputStream(b);
    return o.readObject();
  }

  public static byte[] toJsonBytes(Message m){
    ObjectMapper o = new ObjectMapper();
    try {
      System.out.println(o.writeValueAsString(m));
      return o.writeValueAsBytes(m);
    } catch (IOException e) {
      e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
    }
    return null;
  }

  public static Message fromJsonBytes(byte [] bytes){

    ObjectMapper o = new ObjectMapper();
    Message m = null;
    try {
      m = o.readValue(bytes, Message.class);
    } catch (IOException e) {
      e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
    }
    return m;
  }

  public static class Response extends Message implements RpcRouter.RpcReply {
    private ResponseCode code; // response code

    public static enum ResponseCode {
      SUCCESS(200), BADREQUEST(400), TIMEOUT(408), GONE(410), SERVERERROR(500), SERVICEUNAVAILABLE(503);

      private int code;

      ResponseCode(int code) {
        this.code = code;
      }
    }

    public ResponseCode getCode() {
      return code;
    }

    public void setCode(ResponseCode code) {
      this.code = code;
    }
  }

}

