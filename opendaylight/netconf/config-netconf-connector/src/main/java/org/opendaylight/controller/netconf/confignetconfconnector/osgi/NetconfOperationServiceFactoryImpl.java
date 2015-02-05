/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.confignetconfconnector.osgi;

import java.lang.management.ManagementFactory;
import javax.management.MBeanServer;
import org.opendaylight.controller.config.util.ConfigRegistryJMXClient;
import org.opendaylight.controller.netconf.mapping.api.NetconfOperationServiceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NetconfOperationServiceFactoryImpl implements NetconfOperationServiceFactory {

    public static final int ATTEMPT_TIMEOUT_MS = 1000;
    private static final int SILENT_ATTEMPTS = 30;

    private final YangStoreService yangStoreService;
    private final ConfigRegistryJMXClient jmxClient;

    private static final Logger LOG = LoggerFactory.getLogger(NetconfOperationServiceFactoryImpl.class);

    public NetconfOperationServiceFactoryImpl(YangStoreService yangStoreService) {
        this(yangStoreService, ManagementFactory.getPlatformMBeanServer());
    }

    public NetconfOperationServiceFactoryImpl(YangStoreService yangStoreService, MBeanServer mBeanServer) {
        this.yangStoreService = yangStoreService;

        ConfigRegistryJMXClient configRegistryJMXClient;
        int i = 0;
        // Config registry might not be present yet, but will be eventually
        while(true) {

            try {
                configRegistryJMXClient = new ConfigRegistryJMXClient(mBeanServer);
                break;
            } catch (IllegalStateException e) {
                ++i;
                if (i > SILENT_ATTEMPTS) {
                    LOG.info("JMX client not created after {} attempts, still trying", i, e);
                } else {
                    LOG.debug("JMX client could not be created, reattempting, try {}", i, e);
                }
                try {
                    Thread.sleep(ATTEMPT_TIMEOUT_MS);
                } catch (InterruptedException e1) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Interrupted while reattempting connection", e1);
                }
            }
        }

        jmxClient = configRegistryJMXClient;
        if (i > SILENT_ATTEMPTS) {
            LOG.info("Created JMX client after {} attempts", i);
        } else {
            LOG.debug("Created JMX client after {} attempts", i);
        }
    }

    @Override
    public NetconfOperationServiceImpl createService(String netconfSessionIdForReporting) {
        return new NetconfOperationServiceImpl(yangStoreService, jmxClient, netconfSessionIdForReporting);
    }
}
