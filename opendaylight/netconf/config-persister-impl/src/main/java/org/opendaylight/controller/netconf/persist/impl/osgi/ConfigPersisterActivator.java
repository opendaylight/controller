/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.persist.impl.osgi;

import org.opendaylight.controller.config.persist.api.storage.StorageAdapter.PropertiesProvider;
import org.opendaylight.controller.netconf.persist.impl.ConfigPersisterNotificationHandler;
import org.opendaylight.controller.netconf.persist.impl.PersisterImpl;
import org.opendaylight.controller.netconf.util.osgi.NetconfConfigUtil;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.MBeanServer;
import java.lang.management.ManagementFactory;
import java.net.InetSocketAddress;

public class ConfigPersisterActivator implements BundleActivator {

    private static final Logger logger = LoggerFactory.getLogger(ConfigPersisterActivator.class);

    private final static MBeanServer platformMBeanServer = ManagementFactory.getPlatformMBeanServer();

    private ConfigPersisterNotificationHandler configPersisterNotificationHandler;

    private Thread initializationThread;

    private static final String NETCONF_CONFIG_PERSISTER_PREFIX = "netconf.config.persister.";
    public static final String STORAGE_ADAPTER_CLASS_PROP_SUFFIX =  "storageAdapterClass";

    @Override
    public void start(final BundleContext context) throws Exception {
        logger.debug("ConfigPersister starting");

        PropertiesProvider propertiesProvider = new PropertiesProvider() {
            @Override
            public String getProperty(String key) {
                return context.getProperty(getFullKeyForReporting(key));
            }

            @Override
            public String getFullKeyForReporting(String key) {
                return NETCONF_CONFIG_PERSISTER_PREFIX + key;
            }
        };

        PersisterImpl persister = PersisterImpl.createFromProperties(propertiesProvider);

        InetSocketAddress address = NetconfConfigUtil.extractTCPNetconfAddress(context,
                "Netconf is not configured, persister is not operational");
        configPersisterNotificationHandler = new ConfigPersisterNotificationHandler(persister, address,
                platformMBeanServer);

        // offload initialization to another thread in order to stop blocking activator
        Runnable initializationRunnable = new Runnable() {
            @Override
            public void run() {
                try {
                    configPersisterNotificationHandler.init();
                } catch (InterruptedException e) {
                    logger.info("Interrupted while waiting for netconf connection");
                }
            }
        };
        initializationThread = new Thread(initializationRunnable, "ConfigPersister-registrator");
        initializationThread.start();
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        initializationThread.interrupt();
        configPersisterNotificationHandler.close();
    }
}
