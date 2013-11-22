/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.connector.remoterpc;

import org.opendaylight.controller.sal.connector.remoterpc.dto.MessageWrapper;
import org.opendaylight.controller.sal.core.api.RpcImplementation;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * An implementation of {@link RpcImplementation} that makes
 * remote RPC calls
 */
public class Client implements RpcImplementation {

  // I just input the minimal code so ClientTest wouldn't fail
  private static LinkedBlockingQueue<MessageWrapper> requestQueue = new LinkedBlockingQueue<MessageWrapper>(100);
  public static LinkedBlockingQueue<MessageWrapper> requestQueue() {
    return requestQueue;
  }

  public static void process(MessageWrapper msg) throws TimeoutException, InterruptedException {
    if (requestQueue.size() == 100) throw new TimeoutException();
    requestQueue.put(msg);
  }

  public static Client getInstance() {
    return new Client();
  }

  public void start() {

  }

  public void stop() {
  }
  
  @Override
  public Set<QName> getSupportedRpcs() {
    return Collections.emptySet();
  }

  @Override
  public RpcResult<CompositeNode> invokeRpc(QName rpc, CompositeNode input) {
    //ToDo: Fill in
    return null;
  }
}



/*  SCALA version

package org.opendaylight.controller.sal.connector.remoterpc

  import org.opendaylight.yangtools.yang.data.api.CompositeNode
  import org.opendaylight.yangtools.yang.common.{RpcError, RpcResult, QName}
  import org.opendaylight.controller.sal.core.api.RpcImplementation
  import java.util
  import java.util.{UUID, Collections}
  import org.zeromq.ZMQ
  import org.opendaylight.controller.sal.common.util.{RpcErrors, Rpcs}
  import org.slf4j.LoggerFactory
  import org.opendaylight.controller.sal.connector.remoterpc.dto.{MessageWrapper, RouteIdentifierImpl, Message}
  import Message.MessageType
  import java.util.concurrent._
  import java.lang.InterruptedException
8?

/**
 * An implementation of {@link RpcImplementation} that makes
 * remote RPC calls
*/
/*  object Client extends RpcImplementation{

private val _logger = LoggerFactory.getLogger(Client.getClass);

val requestQueue = new LinkedBlockingQueue[MessageWrapper](100)
  val pool: ExecutorService = Executors.newSingleThreadExecutor()
private val TIMEOUT = 5000 //in ms

  def getInstance = this

  def getSupportedRpcs: util.Set[QName] = {
  Collections.emptySet()
  }

  def invokeRpc(rpc: QName, input: CompositeNode): RpcResult[CompositeNode] = {

  val routeId = new RouteIdentifierImpl()
  routeId.setType(rpc)

  //lookup address for the rpc request
  val routingTable = Server.getInstance().getRoutingTable()
  require( routingTable != null, "Routing table not found. Exiting" )

  val addresses:util.Set[String] = routingTable.getRoutes(routeId.toString)
  require(addresses != null, "Address not found for rpc " + rpc);
require(addresses.size() == 1) //its a global service.

  val address = addresses.iterator().next()
  require(address != null, "Address is null")

  //create in-process "pair" socket and pass it to sender thread
  //Sender replies on this when result is available
  val inProcAddress = "inproc://" + UUID.randomUUID()
  val receiver = Context.zmqContext.socket(ZMQ.PAIR)
  receiver.bind(inProcAddress);

val sender = Context.zmqContext.socket(ZMQ.PAIR)
  sender.connect(inProcAddress)

  val requestMessage = new Message.MessageBuilder()
  .`type`(MessageType.REQUEST)
  //.sender("tcp://localhost:8081")
  .recipient(address)
  .route(routeId)
  .payload(input)
  .build()

  _logger.debug("Queuing up request and expecting response on [{}]", inProcAddress)

  val messageWrapper = new MessageWrapper(requestMessage, sender)
  val errors = new util.ArrayList[RpcError]

  try {
  process(messageWrapper)
  val response = parseMessage(receiver)

  return Rpcs.getRpcResult(
  true, response.getPayload.asInstanceOf[CompositeNode], Collections.emptySet())

  } catch {
  case e: Exception => {
  errors.add(RpcErrors.getRpcError(null,null,null,null,e.getMessage,null,e.getCause))
  return Rpcs.getRpcResult(false, null, errors)
  }
  } finally {
  receiver.close();
sender.close();
}

  }
*/
/**
 * Block on socket for reply
 * @param receiver
 * @return
 */
/*private def parseMessage(receiver:ZMQ.Socket): Message = {
  val bytes = receiver.recv()
  return  Message.deserialize(bytes).asInstanceOf[Message]
  }

  def start() = {
  pool.execute(new Sender)
  }

  def process(msg: MessageWrapper) = {
  _logger.debug("Processing message [{}]", msg)
  val success = requestQueue.offer(msg, TIMEOUT, TimeUnit.MILLISECONDS)

  if (!success) throw new TimeoutException("Queue is full");

}

  def stop() = {
  pool.shutdown() //intiate shutdown
  _logger.debug("Client stopping...")
  //    Context.zmqContext.term();
  //    _logger.debug("ZMQ context terminated")

  try {

  if (!pool.awaitTermination(10, TimeUnit.SECONDS)) {
  pool.shutdownNow();
if (!pool.awaitTermination(10, TimeUnit.SECONDS))
  _logger.error("Client thread pool did not shut down");
}
  } catch {
  case ie:InterruptedException =>
  // (Re-)Cancel if current thread also interrupted
  pool.shutdownNow();
// Preserve interrupt status
Thread.currentThread().interrupt();
}
  _logger.debug("Client stopped")
  }
  }
 */
