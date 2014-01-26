/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.md.controller.topology.lldp;

import org.opendaylight.md.controller.topology.lldp.utils.LLDPDiscoveryUtils;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.topology.discovery.rev130819.LinkDiscovered;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.topology.discovery.rev130819.LinkDiscoveredBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketProcessingListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketReceived;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class LLDPDiscoveryListener implements PacketProcessingListener {
    static Logger LOG = LoggerFactory.getLogger(LLDPDiscoveryListener.class);

    private LLDPDiscoveryProvider manager;

    LLDPDiscoveryListener(LLDPDiscoveryProvider manager) {
        this.manager = manager;
    }
    
    public void onPacketReceived(PacketReceived lldp) {
        NodeConnectorRef src = LLDPDiscoveryUtils.lldpToNodeConnectorRef(lldp.getPayload());
        if(src != null) {
            LinkDiscoveredBuilder ldb = new LinkDiscoveredBuilder();
            ldb.setDestination(lldp.getIngress());
            ldb.setSource(new NodeConnectorRef(src));
            LinkDiscovered ld = ldb.build();
            
            manager.getNotificationService().publish(ld);
            LLDPLinkAger.getInstance().put(ld);
        }
    }
    
}
