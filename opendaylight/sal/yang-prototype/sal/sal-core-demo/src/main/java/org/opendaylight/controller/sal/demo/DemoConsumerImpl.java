/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.demo;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.opendaylight.controller.sal.core.api.Consumer;
import org.opendaylight.controller.sal.core.api.Broker.ConsumerSession;
import org.opendaylight.controller.sal.core.api.notify.NotificationListener;
import org.opendaylight.controller.sal.core.api.notify.NotificationService;
import org.opendaylight.controller.yang.common.QName;
import org.opendaylight.controller.yang.data.api.CompositeNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class DemoConsumerImpl implements Consumer {

    private ConsumerSession session;
    private NotificationService notificationService;
    private final String name;
    private static Logger log = LoggerFactory.getLogger("AlertLogger");

    private boolean changeAware;

    public DemoConsumerImpl(String name) {
        this.name = name;
    }

    private NotificationListener alertLogger = new NotificationListener() {

        @Override
        public void onNotification(CompositeNode notification) {
            System.out.println(name
                    + ": Received alert: "
                    + notification.getFirstSimpleByName(
                            DemoUtils.contentNodeName).getValue());
            log.info("AlertLogger: Received notification: " + notification);
        }

        @Override
        public Set<QName> getSupportedNotifications() {
            Set<QName> supported = new HashSet<QName>();
            supported.add(DemoUtils.alertNotification);
            return supported;
        }
    };

    private NotificationListener changeLogger = new NotificationListener() {

        @Override
        public void onNotification(CompositeNode notification) {
            System.out.println(name
                    + ": Received change: "
                    + notification.getFirstSimpleByName(
                            DemoUtils.contentNodeName).getValue());
            log.info("ChangeLogger: Received notification: " + notification);
        }

        @Override
        public Set<QName> getSupportedNotifications() {
            Set<QName> supported = new HashSet<QName>();
            supported.add(DemoUtils.alertNotification);
            return supported;
        }
    };

    @Override
    public void onSessionInitiated(ConsumerSession session) {
        this.session = session;
        this.notificationService = session
                .getService(NotificationService.class);
        notificationService.addNotificationListener(
                DemoUtils.alertNotification, alertLogger);
        if (isChangeAware()) {
            notificationService.addNotificationListener(
                    DemoUtils.changeNotification, changeLogger);
        }
    }

    @Override
    public Collection<ConsumerFunctionality> getConsumerFunctionality() {
        Set<ConsumerFunctionality> func = new HashSet<ConsumerFunctionality>();
        func.add(alertLogger);
        return func;
    }

    public void closeSession() {
        session.close();
    }

    public boolean isChangeAware() {
        return changeAware;
    }

    public void setChangeAware(boolean changeAware) {
        this.changeAware = changeAware;
    }

}
