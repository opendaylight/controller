/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.md.controller.topology.lldp;

import java.util.Date;
import java.util.Map;
import java.util.Timer;
import java.util.Map.Entry;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

import org.opendaylight.md.controller.topology.lldp.utils.LLDPDiscoveryUtils;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.topology.discovery.rev130819.LinkDiscovered;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.topology.discovery.rev130819.LinkRemovedBuilder;


public class LLDPLinkAger {
    private static final LLDPLinkAger instance = new LLDPLinkAger();
    private Map<LinkDiscovered,Date> linkToDate = new ConcurrentHashMap<LinkDiscovered,Date>();
    private LLDPDiscoveryProvider manager;
    private Timer timer = new Timer();

    public LLDPDiscoveryProvider getManager() {
        return manager;
    }
    public void setManager(LLDPDiscoveryProvider manager) {
        this.manager = manager;
    }
    private LLDPLinkAger() {
        timer.schedule(new LLDPAgingTask(), 0,LLDPDiscoveryUtils.LLDP_INTERVAL);
    }
    public static LLDPLinkAger getInstance() {
        return instance;
    }
    
    public void put(LinkDiscovered link) {
        Date expires = new Date();
        expires.setTime(expires.getTime() + LLDPDiscoveryUtils.LLDP_EXPIRATION_TIME);
        linkToDate.put(link, expires);
    }
    
    public void close() {
        timer.cancel();
    }
    
    private class LLDPAgingTask extends TimerTask {

        @Override
        public void run() {
            for (Entry<LinkDiscovered,Date> entry : linkToDate.entrySet()) {
                LinkDiscovered link = entry.getKey();
                Date expires = entry.getValue();
                Date now = new Date();
                if(now.after(expires)) {
                    if(getInstance().getManager() != null) {
                        LinkRemovedBuilder lrb = new LinkRemovedBuilder(link);
                        getInstance().getManager().getNotificationService().publish(lrb.build());
                        linkToDate.remove(link);
                    }
                }
            }
            
        }
        
    }
}

