/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.confignetconfconnector.osgi;

import org.opendaylight.controller.config.util.ConfigRegistryJMXClient;
import org.opendaylight.controller.config.yang.store.api.YangStoreException;
import org.opendaylight.controller.config.yang.store.api.YangStoreService;
import org.opendaylight.controller.netconf.mapping.api.NetconfOperationServiceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.MBeanServer;
import java.lang.management.ManagementFactory;

public class NetconfOperationServiceFactoryImpl implements NetconfOperationServiceFactory {

    public static final int ATTEMPT_TIMEOUT_MS = 1000;

    private final YangStoreService yangStoreService;
    private final ConfigRegistryJMXClient jmxClient;

    private static final Logger logger = LoggerFactory.getLogger(NetconfOperationServiceFactoryImpl.class);

    public NetconfOperationServiceFactoryImpl(YangStoreService yangStoreService) {
        this(yangStoreService, ManagementFactory.getPlatformMBeanServer());
    }

    public NetconfOperationServiceFactoryImpl(YangStoreService yangStoreService, MBeanServer mBeanServer) {
        this.yangStoreService = yangStoreService;

        // Config registry might not be present yet, but will be eventually
        while(true) {

            final ConfigRegistryJMXClient configRegistryJMXClient;
            try {
                configRegistryJMXClient = new ConfigRegistryJMXClient(mBeanServer);
            } catch (IllegalStateException e) {
                logger.debug("Jmx client could not be created, reattempting");
                try {
                    Thread.sleep(ATTEMPT_TIMEOUT_MS);
                } catch (InterruptedException e1) {
                    throw new RuntimeException(e1);
                }
                continue;
            }

            jmxClient = configRegistryJMXClient;
            break;
        }
    }

    @Override
    public NetconfOperationServiceImpl createService(long netconfSessionId, String netconfSessionIdForReporting) {
        try {
            return new NetconfOperationServiceImpl(yangStoreService, jmxClient, netconfSessionIdForReporting);
        } catch (YangStoreException e) {
            throw new IllegalStateException(e);
        }
    }
}
