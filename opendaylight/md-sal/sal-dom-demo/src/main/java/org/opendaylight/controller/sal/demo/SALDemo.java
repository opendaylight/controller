/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.demo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.opendaylight.controller.sal.core.impl.BrokerImpl;
import org.opendaylight.controller.sal.core.impl.NotificationModule;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SALDemo {
    protected static final Logger logger = LoggerFactory
        .getLogger(SALDemo.class);

    static BrokerImpl broker;
    static DemoProviderImpl provider;
    static DemoConsumerImpl consumer1;
    static DemoConsumerImpl consumer2;

    public static void main(String[] args) {

        initialize();
        initializeProvider();
        displayHelp();

        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        String s;
        try {
            while (true) {

                logger.info("\nEnter your choice (0 - list): ");
                s = in.readLine();
                int choice = Integer.parseInt(s.trim());
                try {
                    switch (choice) {
                    case 0:
                        displayHelp();
                        break;
                    case 1:
                        registerProvider();
                        break;
                    case 2:
                        registerConsumer1();
                        break;
                    case 3:
                        registerConsumer2();
                        break;
                    case 4:
                        sendAlert(in);
                        break;
                    case 5:
                        sendChange(in);
                        break;
                    case 6:
                        unregisterConsumer1();
                        break;
                    case 7:
                        unregisterConsumer2();
                        break;
                    case 8:
                        unregisterProvider();
                        break;
                    case 9:
                        return;
                    default:
                    	logger.info("Please enter valid input.");
                        break;
                    }
                } catch (Exception e) {
                    logger.info("Operation failed. Reason exception raised: ",
                                    e.getClass().getSimpleName());
                    logger.info("   Message: ", e.getMessage());
                }

            }
        } catch (IOException e) {

            logger.error("",e);
        }
    }

    private static void registerConsumer1() {
        broker.registerConsumer(consumer1);
    }

    private static void registerConsumer2() {
        broker.registerConsumer(consumer2);
    }

    private static void sendAlert(BufferedReader in) throws IOException {
    	logger.info("Please enter notification content:");
        String content = in.readLine();
        provider.sendAlertNotification(content);
    }

    private static void sendChange(BufferedReader in) throws IOException {
    	logger.info("Please enter notification content:");
        String content = in.readLine();
        provider.sendChangeNotification(content);
    }

    private static void unregisterConsumer1() {
        consumer1.closeSession();
    }

    private static void unregisterConsumer2() {
        consumer2.closeSession();
    }

    private static void unregisterProvider() {
        provider.closeSession();
    }

    private static void displayHelp() {
    	logger.info("Usage: ");
    	logger.info("  0) Display Help");
    	logger.info("  1) Register Provider");
    	logger.info("  2) Register Consumer 1 (listening on alert)");
    	logger.info("  3) Register Consumer 2 (listening on alert,change)");
    	logger.info("  4) Send Alert Notification");
    	logger.info("  5) Send Change Notification");
    	logger.info("  6) Unregister Consumer 1");
    	logger.info("  7) Unregister Consumer 2");
    	logger.info("  8) Unregister Provider");
    	logger.info("  9) Exit");

    }

    private static void initializeProvider() {
        provider = new DemoProviderImpl();
    }

    private static void initialize() {
    	logger.info("Initializing broker");
        broker = new BrokerImpl();
        NotificationModule notifyModule = new NotificationModule();
        broker.addModule(notifyModule);

        consumer1 = new DemoConsumerImpl("Consumer 1");
        consumer2 = new DemoConsumerImpl("Consumer 2");
        consumer2.setChangeAware(true);
    }

    private static void registerProvider() {
        broker.registerProvider(provider);
    }
}
