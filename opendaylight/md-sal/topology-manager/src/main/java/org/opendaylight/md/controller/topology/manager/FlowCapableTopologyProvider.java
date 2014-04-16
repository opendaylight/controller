/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.md.controller.topology.manager;

import org.opendaylight.controller.sal.binding.api.AbstractBindingAwareProvider;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.controller.sal.binding.api.data.DataProviderService;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.binding.NotificationListener;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FlowCapableTopologyProvider extends AbstractBindingAwareProvider implements AutoCloseable {
    private final static Logger LOG = LoggerFactory.getLogger(FlowCapableTopologyProvider.class);

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

    private final FlowCapableTopologyExporter exporter = new FlowCapableTopologyExporter();
    private Registration<NotificationListener> listenerRegistration;

    @Override
    public void close() {

        FlowCapableTopologyProvider.LOG.info("FlowCapableTopologyProvider stopped.");
        dataService = null;
        notificationService = null;
        if (this.listenerRegistration != null) {
            try {
                this.listenerRegistration.close();
            } catch (Exception e) {
                throw new IllegalStateException("Exception during close of listener registration.",e);
            }
        }
    }

    /**
     * Gets called on start of a bundle.
     *
     * @param session
     */
    @Override
    public void onSessionInitiated(final ProviderContext session) {
        dataService = session.getSALService(DataProviderService.class);
        notificationService = session.getSALService(NotificationProviderService.class);
        this.exporter.setDataService(dataService);
        this.exporter.start();
        this.listenerRegistration = notificationService.registerNotificationListener(this.exporter);
        ;
    }

    /**
     * Gets called during stop bundle
     *
     * @param context
     *            The execution context of the bundle being stopped.
     */
    @Override
    public void stopImpl(final BundleContext context) {
        this.close();
    }
}
