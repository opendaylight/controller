/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.connector.remoterpc;

import org.opendaylight.controller.sal.connector.remoterpc.dto.Message;
import org.opendaylight.controller.sal.connector.remoterpc.dto.MessageWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.ZMQ;

import java.io.IOException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * A class encapsulating {@link ZMQ.Socket} of type {@link ZMQ.REQ}.
 * It adds following capabilities:
 * <li> Retry logic - Tries 3 times before giving up
 * <li> Request times out after {@link TIMEOUT} property
 * <li> The limitation of {@link ZMQ.REQ}/{@link ZMQ.REP} pair is that no 2 requests can be sent before
 * the response for the 1st request is received. To overcome that, this socket queues all messages until
 * the previous request has been responded.
 */
public class RpcSocket {

  // Constants
  public static final int TIMEOUT = 2000;
  public static final int QUEUE_SIZE = 10;
  public static final int NUM_RETRIES = 3;
  private static final Logger log = LoggerFactory.getLogger(RpcSocket.class);

  private ZMQ.Socket socket;
  private ZMQ.Poller poller;
  private String address;
  private SocketState state;
  private long sendTime;
  private int retriesLeft;
  private LinkedBlockingQueue<MessageWrapper> inQueue;


  public RpcSocket(String address, ZMQ.Poller poller) {
    this.socket = null;
    this.state = new IdleSocketState();
    this.sendTime = -1;
    this.retriesLeft = NUM_RETRIES;
    this.inQueue = new LinkedBlockingQueue<MessageWrapper>(QUEUE_SIZE);
    this.address = address;
    this.poller = poller;
    createSocket();
  }

  public ZMQ.Socket getSocket() {
    return socket;
  }

  public String getAddress() {
    return address;
  }

  public int getRetriesLeft() {
    return retriesLeft;
  }

  public void setRetriesLeft(int retriesLeft) {
    this.retriesLeft = retriesLeft;
  }

  public SocketState getState() {
    return state;
  }

  public void setState(SocketState state) {
    this.state = state;
  }

  public int getQueueSize() {
    return inQueue.size();
  }

  public MessageWrapper removeCurrentRequest() {
    return inQueue.poll();
  }

  public boolean hasTimedOut() {
    return (System.currentTimeMillis() - sendTime > RpcSocket.TIMEOUT);
  }

  public void send(MessageWrapper request) throws TimeoutException {
    try {
      boolean success = inQueue.offer(request, TIMEOUT, TimeUnit.MILLISECONDS);    
      if (!success) {
        throw new TimeoutException("send :: Queue is full");
      }
      process();
    }
    catch (InterruptedException e) {
      log.error("send : Thread interrupted while attempting to add request to inQueue", e);
    }
  }
  
  public MessageWrapper receive() {
    Message response = parseMessage();
    MessageWrapper messageWrapper = inQueue.poll(); //remove the message from queue
    MessageWrapper responseMessageWrapper = new MessageWrapper(response, messageWrapper.getReceiveSocket());

    state = new IdleSocketState();
    retriesLeft = NUM_RETRIES;
    return responseMessageWrapper;
  }
  
  public void process() {
    if (getQueueSize() > 0) //process if there's message in the queue
      state.process(this);
  }

  // Called by IdleSocketState & BusySocketState
  public void sendMessage() {
    //Get the message from queue without removing it. For retries
    MessageWrapper messageWrapper = inQueue.peek();
    if (messageWrapper != null) {
      Message message = messageWrapper.getMessage();
      try {
        socket.send(Message.serialize(message));
      }
      catch (IOException e) {
        log.debug("Message send failed [{}]", message);
        log.debug("Exception [{}]", e);
      }
      sendTime = System.currentTimeMillis();
    }
  }
  
  public Message parseMessage() {
    Message parsedMessage = null;
    byte[] bytes = socket.recv();
    log.debug("Received bytes:[{}]", bytes.length);
    try {
      parsedMessage = (Message)Message.deserialize(bytes);
    }
    catch (IOException e) {
      log.debug("parseMessage : Deserializing received bytes failed", e);
    }
    catch (ClassNotFoundException e) {
      log.debug("parseMessage : Deserializing received bytes failed", e);
    }
    return parsedMessage;
  }

  public void recycleSocket() {
    close();
  }

  public void close() {
    socket.setLinger(10);
    socket.close();
  }

  private void createSocket() {
    socket = Context.getInstance().getZmqContext().socket(ZMQ.REQ);
    socket.connect(address);
    poller.register(socket, ZMQ.Poller.POLLIN);
    state = new IdleSocketState();
  }


  /**
   * Represents the state of a {@link org.opendaylight.controller.sal.connector.remoterpc.RpcSocket}
   */
  public static interface SocketState {

    /* The processing actions to be performed in this state
     */
    public void process(RpcSocket socket);
  }

  /**
   * Represents the idle state of a {@link org.opendaylight.controller.sal.connector.remoterpc.RpcSocket}
   */
  public static class IdleSocketState implements SocketState {

    @Override
    public void process(RpcSocket socket) {
      socket.sendMessage();
      socket.setState(new BusySocketState());
      socket.setRetriesLeft(socket.getRetriesLeft()-1);
    }
  }

  /**
   * Represents the busy state of a {@link org.opendaylight.controller.sal.connector.remoterpc.RpcSocket}
   */
  public static class BusySocketState implements SocketState {

    private static Logger log = LoggerFactory.getLogger(BusySocketState.class);

    @Override
    public void process(RpcSocket socket) {
      if (socket.hasTimedOut()) {
        if (socket.getRetriesLeft() > 0) {
          log.debug("process : Request timed out, retrying now...");
          socket.sendMessage();
          socket.setRetriesLeft(socket.getRetriesLeft() - 1);
        }
        else {
          // No more retries for current request, so stop processing the current request
          MessageWrapper message = socket.removeCurrentRequest();
          if (message != null) {
            log.error("Unable to process rpc request [{}]", message);
            socket.setState(new IdleSocketState());
            socket.setRetriesLeft(NUM_RETRIES);
          }
        }
      }
      // Else no timeout, so allow processing to continue
    }
  }
}
