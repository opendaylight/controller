/**
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.md.controller.topology.lldp;

import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.controller.sal.binding.api.data.DataProviderService;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.binding.NotificationListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("all")
public class LLDPDiscoveryProvider implements AutoCloseable {
    private final static Logger LOG =  LoggerFactory.getLogger(LLDPDiscoveryProvider.class);
    private DataProviderService dataService;

    public DataProviderService getDataService() {
        return this.dataService;
    }

    public void setDataService(final DataProviderService dataService) {
        this.dataService = dataService;
    }

    private NotificationProviderService notificationService;

    public NotificationProviderService getNotificationService() {
        return this.notificationService;
    }

    public void setNotificationService(final NotificationProviderService notificationService) {
        this.notificationService = notificationService;
    }

    private final LLDPDiscoveryListener commiter = new LLDPDiscoveryListener(LLDPDiscoveryProvider.this);
    private Registration<NotificationListener> listenerRegistration;

    public void start() {
        NotificationProviderService newNotificationService = this.getNotificationService();
        Registration<NotificationListener> registerNotificationListener = newNotificationService.registerNotificationListener(this.commiter);
        this.listenerRegistration = registerNotificationListener;
        LLDPLinkAger instance = LLDPLinkAger.getInstance();
        instance.setManager(this);
        LOG.info("LLDPDiscoveryListener Started.");
    }

    public void close() {
        try {
            LOG.info("LLDPDiscoveryListener stopped.");
            if (this.listenerRegistration!=null) {
                this.listenerRegistration.close();
            }
            LLDPLinkAger.getInstance().close();
        } catch (Exception e) {
            throw new Error(e);
        }
    }
}
