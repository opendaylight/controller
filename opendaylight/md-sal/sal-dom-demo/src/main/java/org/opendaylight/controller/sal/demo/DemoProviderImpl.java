/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.demo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.opendaylight.controller.sal.core.api.Broker.ProviderSession;
import org.opendaylight.controller.sal.core.api.notify.NotificationProviderService;
import org.opendaylight.controller.yang.data.api.Node;
import org.opendaylight.controller.yang.data.util.Nodes;


public class DemoProviderImpl implements
        org.opendaylight.controller.sal.core.api.Provider {

    private ProviderSession session;
    private NotificationProviderService notifier;

    @Override
    public void onSessionInitiated(ProviderSession session) {
        this.session = session;
        notifier = session.getService(NotificationProviderService.class);
    }

    @Override
    public Collection<ProviderFunctionality> getProviderFunctionality() {
        return Collections.emptySet();
    }

    public void sendAlertNotification(String content) {
        List<Node<?>> nodes = new ArrayList<Node<?>>();
        nodes.add(DemoUtils.contentNode(content));

        if (notifier == null) {
            System.out.println("Provider: Error: Session not available");
            System.out
                    .println("                 Notification Service not available");
            return;
        }
        notifier.sendNotification(Nodes.containerNode(
                DemoUtils.alertNotification, nodes));
    }

    public void sendChangeNotification(String content) {
        List<Node<?>> nodes = new ArrayList<Node<?>>();
        nodes.add(DemoUtils.contentNode(content));

        if (notifier == null) {
            System.out.println("Provider: Error: Session not available");
            System.out
                    .println("                 Notification Service not available");
            return;
        }
        notifier.sendNotification(Nodes.containerNode(
                DemoUtils.changeNotification, nodes));
    }

    public void closeSession() {
        session.close();
    }
}
