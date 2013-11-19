/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.connector.remoterpc

import java.util.concurrent.{TimeoutException, TimeUnit, ConcurrentHashMap, LinkedBlockingQueue}
import org.zeromq.ZMQ
import org.slf4j.{LoggerFactory, Logger}
import scala.collection.convert.decorateAsScala._
import java.net.UnknownHostException
import org.opendaylight.controller.sal.connector.remoterpc.dto.{MessageWrapper, Message}

/**
 * Manages creation of {@link RpcSocket} and their registration with {@link ZMQ.Poller}
 */

class SocketManager {

  val _logger = LoggerFactory.getLogger(SocketManager.this.getClass)

  /*
   * RpcSockets mapped by network address its connected to
   */
  private val managedSockets = new ConcurrentHashMap[String, RpcSocket]()

  private val _poller = new ZMQ.Poller(2); //randomly selected size. Poller grows automatically

  /**
   * Returns a {@link RpcSocket} for the given address
   * @param address network address with port eg: 10.199.199.20:5554
   * @return
   */
  def getManagedSocket(address:String):RpcSocket = {
    //Precondition
    require(address.matches("(tcp://)(.*)(:)(\\d*)"),
            "Address must of format 'tcp://<ip address>:<port>' but is " + address)

    if (!managedSockets.containsKey(address)) {
      _logger.debug("{} Creating new socket for {}", Thread.currentThread().getName())

      try{
        val socket = new RpcSocket(address, _poller)
        managedSockets.put(address, socket)
      }catch{
        case e:UnknownHostException => throw new IllegalArgumentException(e)
      }
    }

    managedSockets.get(address)
  }

  /**
   * Returns a {@link RpcSocket} for the given {@link ZMQ.Socket}
   * @param socket
   * @return
   */
  def getManagedSocketFor(socket:ZMQ.Socket):Option[RpcSocket] =
    getManagedSockets().asScala.find(s => socket == s.socket)

  /**
   * Return a collection of all managed sockets
   * @return
   */
  def getManagedSockets() = {
    managedSockets.values()
  }

  /**
   * Returns the {@link ZMQ.Poller}
   * @return
   */
  def poller = _poller

  /**
   * This should be called when stopping the server to close all the sockets
   * @return
   */
  def stop = {
    _logger.debug("{} stopping SocketManager", Thread.currentThread().getName)
    managedSockets.values().asScala.map(socket => socket.close )
    managedSockets.clear();
    _logger.debug("SocketManager stopped")
  }
}

/**
 * A class encapsulating {@link ZMQ.Socket} of type {@link ZMQ.REQ}.
 * It adds following capabilities:
 * <li> Retry logic - Tries 3 times before giving up
 * <li> Request times out after {@link TIMEOUT} property
 * <li> The limitation of {@link ZMQ.REQ}/{@link ZMQ.REP} pair is that no 2 requests can be sent before
 * the response for the 1st request is received. To overcome that, this socket queues all messages until
 * the previous request has been responded.
 *
 * @param address
 */
class RpcSocket(val address: String, val poller: ZMQ.Poller) {

  private val _logger: Logger = LoggerFactory.getLogger(RpcSocket.this.getClass())

  private val IDLE: Byte = 0
  private val BUSY: Byte = 1
  private val QUEUE_SIZE = 10  //randomly selected size
  private val NUM_RETRIES = 3
  private val TIMEOUT = 2000 //in ms
  private val inQueue = new LinkedBlockingQueue[MessageWrapper](QUEUE_SIZE)

  private var state: Byte = IDLE
  private var sendTime = System.currentTimeMillis()
  private var retriesLeft = NUM_RETRIES
  private var _socket:ZMQ.Socket = null
  createSocket()

  def socket = _socket

  def queueSize = inQueue.size()

  def send(request: MessageWrapper) = {
    val success = inQueue.offer(request, TIMEOUT, TimeUnit.MILLISECONDS)

    if (!success) throw new TimeoutException("Queue is full");

    process()
  }


  def receive(): MessageWrapper = {
    //receive from socket, parse message and return response
    val response = parseMessage()
    val messageWrapper = inQueue.poll()//remove the message from queue
    val responseMessageWrapper = new MessageWrapper(response, messageWrapper.receiveSocket)

    state = IDLE
    retriesLeft = NUM_RETRIES
    responseMessageWrapper
  }

  def process() = {

    if (isIdle()) {
      sendMessage()
      state = BUSY
      retriesLeft = retriesLeft - 1
    } else if (hasTimedOut()) {
      if (retriesLeft > 0) {
        _logger.debug("Retrying...")
        //recycleSocket()
        sendMessage()
        retriesLeft = retriesLeft - 1
      } else {
        val message = inQueue.poll();
        if (message != null) {
          //no more retries left, give up
          _logger.error("Unable to process rpc request [{}]", message)
          state = IDLE
          retriesLeft = NUM_RETRIES
        } // else no messages to process
      }
    }
  }

  private def isIdle(): Boolean = state match {
    case 0 => true
    case 1 => false
  }

  private def hasTimedOut(): Boolean = {
    if (System.currentTimeMillis() - sendTime > TIMEOUT) true
    else false
  }

  private def sendMessage() = {
    val messageWrapper = inQueue.peek();
     //get the message from queue without removing it. For retries
    if (messageWrapper != null) {
      val message = messageWrapper.message
      try{
      _socket.send(Message.serialize(message))
      }catch {
        case t:Throwable => _logger.debug("Message send failed [{}]", message)
      }
      sendTime = System.currentTimeMillis()
    }
  }

  private def parseMessage(): Message = {
    val bytes = _socket.recv()
    _logger.debug("Received bytes:[{}]", bytes.length)
    return  Message.deserialize(bytes).asInstanceOf[Message]
  }

  private def recycleSocket() = {
    close()
    createSocket()
  }

  def close() = {
    _socket.setLinger(10) //10ms to drain message queue
    _socket.close()

  }

  private def createSocket() = {
    _socket = Context.zmqContext.socket(ZMQ.REQ)
    _socket.connect(address)
    poller.register(_socket, ZMQ.Poller.POLLIN)
    state = IDLE
  }
}
