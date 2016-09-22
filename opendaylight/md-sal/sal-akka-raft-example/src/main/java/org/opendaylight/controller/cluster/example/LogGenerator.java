/*
 * Copyright (c) 2014, 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.example;

import akka.actor.ActorRef;
import org.opendaylight.controller.cluster.example.messages.KeyValue;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Created by kramesha on 7/16/14.
 */
public class LogGenerator {
    private Map<ActorRef, LoggingThread> clientToLoggingThread = new HashMap<>();

    public void startLoggingForClient(ActorRef client) {
        LoggingThread lt = new LoggingThread(client);
        clientToLoggingThread.put(client, lt);
        Thread t = new Thread(lt);
        t.start();
    }

    public void stopLoggingForClient(ActorRef client) {
        clientToLoggingThread.get(client).stopLogging();
        clientToLoggingThread.remove(client);
    }

    public class LoggingThread implements Runnable {

        private ActorRef clientActor;
        private volatile boolean stopLogging = false;

        public LoggingThread(ActorRef clientActor) {
            this.clientActor = clientActor;
        }

        public void run() {
            Random r = new Random();
            while (true) {
                if (stopLogging) {
                    System.out.println("Logging stopped for client:" + clientActor.path());
                    break;
                }
                String key = clientActor.path().name();
                int random = r.nextInt(100);
                clientActor.tell(new KeyValue(key+"-key-" + random, "value-" + random), null);
                try {
                    Thread.sleep((random%10) * 1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        public void stopLogging() {
            stopLogging = true;
        }

        public void startLogging() {
            stopLogging = false;
        }


    }


}
