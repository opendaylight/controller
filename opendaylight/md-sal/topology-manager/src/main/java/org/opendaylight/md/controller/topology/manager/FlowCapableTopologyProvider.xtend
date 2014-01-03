/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.md.controller.topology.manager

import org.opendaylight.controller.sal.binding.api.NotificationProviderService
import org.opendaylight.controller.sal.binding.api.data.DataProviderService
import org.opendaylight.yangtools.concepts.Registration
import org.opendaylight.yangtools.yang.binding.NotificationListener
import org.slf4j.LoggerFactory
import org.opendaylight.controller.sal.binding.api.AbstractBindingAwareProvider
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext

class FlowCapableTopologyProvider extends AbstractBindingAwareProvider implements AutoCloseable {



    static val LOG = LoggerFactory.getLogger(FlowCapableTopologyProvider);

    @Property
    DataProviderService dataService;        

    @Property
    NotificationProviderService notificationService;

    val FlowCapableTopologyExporter exporter = new FlowCapableTopologyExporter();

    Registration<NotificationListener> listenerRegistration
    
    override close() {
       LOG.info("FlowCapableTopologyProvider stopped.");
        listenerRegistration?.close();
    }
    
    override onSessionInitiated(ProviderContext session) {
        dataService = session.getSALService(DataProviderService)
        notificationService = session.getSALService(NotificationProviderService)
        exporter.setDataService(dataService);
        exporter.start();
        listenerRegistration = notificationService.registerNotificationListener(exporter);
    }
    
}


