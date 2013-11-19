/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.connector.remoterpc.dto;

import org.opendaylight.controller.sal.connector.api.RpcRouter;

import java.io.*;

public class Message implements Serializable {

 public static enum MessageType {
    ANNOUNCE((byte) 0),  //TODO: Remove announce, add rpc registration and deregistration
    HEARTBEAT((byte) 1),
    REQUEST((byte) 2),
    RESPONSE((byte) 3),
    ERROR((byte)4);

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
  private String recipient;
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

  public String getRecipient() {
    return recipient;
  }

  public void setRecipient(String recipient) {
    this.recipient = recipient;
  }
  @Override
  public String toString() {
    return "Message{" +
        "type=" + type +
        ", sender='" + sender + '\'' +
        ", recipient='" + recipient + '\'' +
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

  /**
   * Builds a {@link Message} object
   */
  public static class MessageBuilder{

    private Message message;

    public MessageBuilder(){
      message = new Message();
    }


    public MessageBuilder type(MessageType type){
      message.setType(type);
      return this;
    }

    public MessageBuilder sender(String sender){
      message.setSender(sender);
      return this;
    }

    public MessageBuilder recipient(String recipient){
      message.setRecipient(recipient);
      return this;
    }

    public MessageBuilder route(RpcRouter.RouteIdentifier route){
      message.setRoute(route);
      return this;
    }

    public MessageBuilder payload(Object obj){
      message.setPayload(obj);
      return this;
    }

    public Message build(){
      return message;
    }
  }
}

