/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.impl.osgi;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.opendaylight.controller.netconf.mapping.api.NetconfOperationService;
import org.opendaylight.controller.netconf.mapping.api.NetconfOperationServiceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NetconfOperationServiceSnapshot implements AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(NetconfOperationServiceSnapshot.class);

    private final Set<NetconfOperationService> services;
    private final String netconfSessionIdForReporting;

    public NetconfOperationServiceSnapshot(Set<NetconfOperationServiceFactory> factories, long sessionId) {
        Set<NetconfOperationService> services = new HashSet<>();
        netconfSessionIdForReporting = getNetconfSessionIdForReporting(sessionId);
        for (NetconfOperationServiceFactory factory : factories) {
            services.add(factory.createService(sessionId, netconfSessionIdForReporting));
        }
        this.services = Collections.unmodifiableSet(services);
    }

    private static String getNetconfSessionIdForReporting(long sessionId) {
        return "netconf session id " + sessionId;
    }

    public String getNetconfSessionIdForReporting() {
        return netconfSessionIdForReporting;
    }

    public Set<NetconfOperationService> getServices() {
        return services;
    }

    @Override
    public void close() {
        RuntimeException firstException = null;
        for (NetconfOperationService service : services) {
            try {
                service.close();
            } catch (RuntimeException e) {
                logger.warn("Got exception while closing {}", service, e);
                if (firstException == null) {
                    firstException = e;
                } else {
                    firstException.addSuppressed(e);
                }
            }
        }
        if (firstException != null) {
            throw firstException;
        }
    }

    @Override
    public String toString() {
        return "NetconfOperationServiceSnapshot{" + netconfSessionIdForReporting + '}';
    }
}
