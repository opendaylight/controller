/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.connector.remoterpc.utils;

import junit.framework.Assert;
import org.opendaylight.controller.sal.connector.remoterpc.dto.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.ZMQ;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

public class MessagingUtil {

  private static final Logger _logger = LoggerFactory.getLogger(MessagingUtil.class);

  public static Runnable startReplyServer(final ZMQ.Context context,
                                          final String serverAddress,
                                          final int numRequests /*number of requests after which server shuts down*/) {
    return new Runnable() {

      @Override
      public void run() {
        final ZMQ.Socket socket = context.socket(ZMQ.REP);
        try {
          int returnCode = socket.bind("tcp://" + serverAddress);
          Assert.assertNotSame(-1, returnCode);
          _logger.info(" Starting reply server[{}] for test...", serverAddress);

          //for (int i=0;i<numRequests;i++) {
          while (!Thread.currentThread().isInterrupted()) {
            byte[] bytes = socket.recv();
            _logger.debug(" Got request ");
            socket.send(bytes);
            _logger.debug(" Sent response ");
          }
        } catch (Exception x) {
          StringWriter w = new StringWriter();
          PrintWriter p = new PrintWriter(w);
          x.printStackTrace(p);
          _logger.debug(w.toString());
        } finally {
          socket.close();
          _logger.info("Shutting down reply server");
        }
      }
    };
  }

  public static Runnable createRouterDealerBridge(final ZMQ.Context context, final String dealerAddress, final int routerPort) {
    return new Runnable() {
      @Override
      public void run() {
        ZMQ.Socket router = null;
        ZMQ.Socket dealer = null;
        try {
          router = context.socket(ZMQ.ROUTER);
          dealer = context.socket(ZMQ.DEALER);
          router.bind("tcp://*:" + routerPort);
          dealer.bind(dealerAddress);
          ZMQ.proxy(router, dealer, null);
        } catch (Exception e) {/*Ignore*/} finally {
          if (router != null) router.close();
          if (dealer != null) dealer.close();
        }
      }
    };
  }

  public static Runnable sendAnEmptyMessage(final ZMQ.Context context, final String serverAddress)
          throws IOException, ClassNotFoundException, InterruptedException {

    return new Runnable() {
      @Override
      public void run() {
        final ZMQ.Socket socket = context.socket(ZMQ.REQ);
        try {

          socket.connect(serverAddress);
          System.out.println(Thread.currentThread().getName() + " Sending message");
          try {
            socket.send(Message.serialize(new Message()));
          } catch (IOException e) {
            e.printStackTrace();
          }
          byte[] bytes = socket.recv();
          Message response = null;
          try {
            response = (Message) Message.deserialize(bytes);
          } catch (IOException e) {
            e.printStackTrace();
          } catch (ClassNotFoundException e) {
            e.printStackTrace();
          }
          System.out.println(Thread.currentThread().getName() + " Got response " + response);
        } catch (Exception x) {
          x.printStackTrace();
        } finally {
          socket.close();
        }
      }
    };
  }

  public static Message createEmptyMessage() {
    return new Message();
  }

  /**
   * Closes ZMQ Context. It tries to gracefully terminate the context. If
   * termination takes more than a second, its forcefully shutdown.
   */
  public static void closeZmqContext(final ZMQ.Context context) {
    if (context == null) return;

    ExecutorService exec = Executors.newSingleThreadExecutor();
    FutureTask zmqTermination = new FutureTask(new Runnable() {

      @Override
      public void run() {
        try {
          if (context != null)
            context.term();
            _logger.debug("ZMQ Context terminated gracefully!");
        } catch (Exception e) {/*Ignore and continue shutdown*/}
      }
    }, null);

    exec.execute(zmqTermination);

    try {
      zmqTermination.get(1L, TimeUnit.SECONDS);
    } catch (Exception e) {
      _logger.debug("ZMQ Context terminated forcefully!");
    }

    exec.shutdownNow();
  }
}
