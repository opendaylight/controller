/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.connector.remoterpc

import org.slf4j.{LoggerFactory, Logger}
import scala.collection.JavaConverters._
import scala.Some
import org.opendaylight.controller.sal.connector.remoterpc.dto.{MessageWrapper, Message}

/**
 * Main server thread for sending requests. This does not maintain any state. If the
 * thread dies, it will be restarted
 */
class Sender extends Runnable {
  private val _logger: Logger = LoggerFactory.getLogger(Sender.this.getClass())

  override def run = {
    _logger.info("Sender starting...")
    val socketManager = new SocketManager()

    try {
      while (!Thread.currentThread().isInterrupted) {
        //read incoming messages from blocking queue
        val request: MessageWrapper = Client.requestQueue.poll()

        if (request != null) {
          if ((request.message != null) &&
            (request.message.getRecipient != null)) {

            val socket = socketManager.getManagedSocket(request.message.getRecipient)
            socket.send(request)
          } else {
            //invalid message. log and drop
            _logger.error("Invalid request [{}]", request)
          }
        }

        socketManager.getManagedSockets().asScala.map(s => s.process)

        // Poll all sockets for responses every 1 sec
        poll(socketManager)

        // If any sockets get a response, process it
        for (i <- 0 until socketManager.poller.getSize) {
          if (socketManager.poller.pollin(i)) {
            val socket = socketManager.getManagedSocketFor(socketManager.poller.getItem(i).getSocket)

            socket match {
              case None => //{
                _logger.error("Could not find a managed socket for zmq socket")
                throw new IllegalStateException("Could not find a managed socket for zmq socket")
              //}
              case Some(s) => {
                val response = s.receive()
                _logger.debug("Received rpc response [{}]", response.message)
                response.receiveSocket.send(Message.serialize(response.message))
              }
            }
          }
        }

      }
    } catch{
      case e:Exception => {
        _logger.debug("Sender stopping due to exception")
        e.printStackTrace()
      }
    } finally {
      socketManager.stop
    }
  }

  def poll(socketManager:SocketManager) = {
    try{
      socketManager.poller.poll(10)
    }catch{
      case t:Throwable => //ignore and continue
    }
  }
}


//    def newThread(r: Runnable): Thread = {
//      val t = new RequestHandler()
//      t.setUncaughtExceptionHandler(new RequestProcessorExceptionHandler)
//      t
//    }



/**
 * Restarts the request processing server in the event of unforeseen exceptions
 */
//private class RequestProcessorExceptionHandler extends UncaughtExceptionHandler {
//  def uncaughtException(t: Thread, e: Throwable) = {
//    _logger.error("Exception caught during request processing [{}]", e)
//    _logger.info("Restarting request processor server...")
//    RequestProcessor.start()
//  }

