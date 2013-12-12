/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.connector.remoterpc.utils;

import org.opendaylight.controller.sal.connector.remoterpc.dto.Message;
import org.zeromq.ZMQ;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

public class MessagingUtil {

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

  /**
   * Closes ZMQ Context. It tries to gracefully terminate the context. If
   * termination takes more than a second, its forcefully shutdown.
   */
  public static void closeZmqContext(final ZMQ.Context context) {
    ExecutorService exec = Executors.newSingleThreadExecutor();
    FutureTask zmqTermination = new FutureTask(new Runnable() {

      @Override
      public void run() {
        try {
          if (context != null)
            context.term();
        } catch (Exception e) {/*Ignore and continue shutdown*/}
      }
    }, null);

    exec.execute(zmqTermination);

    try {
      zmqTermination.get(1L, TimeUnit.SECONDS);
    } catch (Exception e) {/*ignore and continue with shutdown*/}

    exec.shutdownNow();
  }
}
