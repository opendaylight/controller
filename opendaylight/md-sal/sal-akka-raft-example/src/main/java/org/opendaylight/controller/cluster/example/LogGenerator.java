/*
 * Copyright (c) 2014, 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.example;

import akka.actor.ActorRef;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import org.opendaylight.controller.cluster.example.messages.KeyValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by kramesha on 7/16/14.
 */
public class LogGenerator {
    private static final Logger LOG = LoggerFactory.getLogger(LogGenerator.class);

    private final Map<ActorRef, LoggingThread> clientToLoggingThread = new HashMap<>();

    public void startLoggingForClient(final ActorRef client) {
        LoggingThread lt = new LoggingThread(client);
        clientToLoggingThread.put(client, lt);
        new Thread(lt).start();
    }

    public void stopLoggingForClient(final ActorRef client) {
        clientToLoggingThread.get(client).stopLogging();
        clientToLoggingThread.remove(client);
    }

    public static class LoggingThread implements Runnable {

        private final ActorRef clientActor;
        private volatile boolean stopLogging = false;

        public LoggingThread(final ActorRef clientActor) {
            this.clientActor = clientActor;
        }

        @Override
        public void run() {
            Random random = new Random();
            while (true) {
                if (stopLogging) {
                    LOG.info("Logging stopped for client: {}", clientActor.path());
                    break;
                }
                String key = clientActor.path().name();
                int randomInt = random.nextInt(100);
                clientActor.tell(new KeyValue(key + "-key-" + randomInt, "value-" + randomInt), null);

                try {
                    Thread.sleep(randomInt % 10 * 1000L);
                } catch (InterruptedException e) {
                    LOG.info("Interrupted while sleeping", e);
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
