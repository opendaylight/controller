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


public class SALDemo {

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

                System.out.print("\nEnter your choice (0 - list): ");
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
                        System.out.println("Please enter valid input.");
                        break;
                    }
                } catch (Exception e) {
                    System.out
                            .println("Operation failed. Reason exception raised: "
                                    + e.getClass().getSimpleName());
                    System.out.println("   Message: " + e.getMessage());
                }

            }
        } catch (IOException e) {

            e.printStackTrace();
        }
    }

    private static void registerConsumer1() {
        broker.registerConsumer(consumer1);
    }

    private static void registerConsumer2() {
        broker.registerConsumer(consumer2);
    }

    private static void sendAlert(BufferedReader in) throws IOException {
        System.out.print("Please enter notification content:");
        String content = in.readLine();
        provider.sendAlertNotification(content);
    }

    private static void sendChange(BufferedReader in) throws IOException {
        System.out.print("Please enter notification content:");
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
        System.out.println("Usage: ");
        System.out.println("  0) Display Help");
        System.out.println("  1) Register Provider");
        System.out.println("  2) Register Consumer 1 (listening on alert)");
        System.out
                .println("  3) Register Consumer 2 (listening on alert,change)");
        System.out.println("  4) Send Alert Notification");
        System.out.println("  5) Send Change Notification");
        System.out.println("  6) Unregister Consumer 1");
        System.out.println("  7) Unregister Consumer 2");
        System.out.println("  8) Unregister Provider");
        System.out.println("  9) Exit");

    }

    private static void initializeProvider() {
        provider = new DemoProviderImpl();
    }

    private static void initialize() {
        System.out.println("Initializing broker");
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
