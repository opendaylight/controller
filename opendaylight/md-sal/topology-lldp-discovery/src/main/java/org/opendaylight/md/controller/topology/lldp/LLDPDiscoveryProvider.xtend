/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.md.controller.topology.lldp

import org.opendaylight.controller.sal.binding.api.NotificationProviderService
import org.opendaylight.controller.sal.binding.api.data.DataProviderService
import org.opendaylight.yangtools.concepts.Registration
import org.opendaylight.yangtools.yang.binding.NotificationListener
import org.slf4j.LoggerFactory

class LLDPDiscoveryProvider implements AutoCloseable {


    static val LOG = LoggerFactory.getLogger(LLDPDiscoveryProvider);

    @Property
    DataProviderService dataService;        

    @Property
    NotificationProviderService notificationService;

    val LLDPDiscoveryListener commiter = new LLDPDiscoveryListener(this);

    Registration<NotificationListener> listenerRegistration

    def void start() {
        listenerRegistration = notificationService.registerNotificationListener(commiter);
        LLDPLinkAger.instance.manager = this;
        LOG.info("LLDPDiscoveryListener Started.");
        
    }   
    
    override close() {
       LOG.info("LLDPDiscoveryListener stopped.");
        listenerRegistration?.close();
        LLDPLinkAger.instance.close();
    }
    
}


