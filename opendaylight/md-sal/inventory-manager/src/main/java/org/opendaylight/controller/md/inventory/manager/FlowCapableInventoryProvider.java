/**
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.inventory.manager;

import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.controller.sal.binding.api.data.DataModificationTransaction;
import org.opendaylight.controller.sal.binding.api.data.DataProviderService;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.binding.NotificationListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FlowCapableInventoryProvider implements AutoCloseable {

    private final static Logger LOG = LoggerFactory.getLogger(FlowCapableInventoryProvider.class);

    private DataProviderService dataService;
    private NotificationProviderService notificationService;
    private Registration<NotificationListener> listenerRegistration;
    private final NodeChangeCommiter changeCommiter = new NodeChangeCommiter(FlowCapableInventoryProvider.this);

    public void start() {
        this.listenerRegistration = this.notificationService.registerNotificationListener(this.changeCommiter);
        LOG.info("Flow Capable Inventory Provider started.");
    }

    protected DataModificationTransaction startChange() {
        DataProviderService _dataService = this.dataService;
        return _dataService.beginTransaction();
    }

    @Override
    public void close() {
        try {
            LOG.info("Flow Capable Inventory Provider stopped.");
            if (this.listenerRegistration != null) {
                this.listenerRegistration.close();
            }
        } catch (Exception e) {
            String errMsg = "Error by stop Flow Capable Inventory Provider.";
            LOG.error(errMsg, e);
            throw new RuntimeException(errMsg, e);
        }
    }

    public DataProviderService getDataService() {
        return this.dataService;
    }

    public void setDataService(final DataProviderService dataService) {
        this.dataService = dataService;
    }

    public NotificationProviderService getNotificationService() {
        return this.notificationService;
    }

    public void setNotificationService(
            final NotificationProviderService notificationService) {
        this.notificationService = notificationService;
    }
}
