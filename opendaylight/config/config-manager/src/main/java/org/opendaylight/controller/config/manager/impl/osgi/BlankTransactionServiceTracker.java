/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.manager.impl.osgi;

import org.opendaylight.controller.config.api.jmx.CommitStatus;
import org.opendaylight.controller.config.manager.impl.ConfigRegistryImpl;
import org.opendaylight.controller.config.spi.ModuleFactory;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.ObjectName;

/**
 * Every time factory is added or removed, blank transaction is triggered to handle
 * {@link org.opendaylight.controller.config.spi.ModuleFactory#getDefaultModules(org.opendaylight.controller.config.api.DependencyResolverFactory)}
 * functionality.
 */
public class BlankTransactionServiceTracker implements ServiceTrackerCustomizer<ModuleFactory, Object> {
    private static final Logger logger = LoggerFactory.getLogger(BlankTransactionServiceTracker.class);

    private final ConfigRegistryImpl configRegistry;

    public BlankTransactionServiceTracker(ConfigRegistryImpl configRegistry) {
        this.configRegistry = configRegistry;
    }

    @Override
    public Object addingService(ServiceReference<ModuleFactory> moduleFactoryServiceReference) {
        blankTransaction();
        return null;
    }

    private synchronized void blankTransaction() {
        // create transaction
        ObjectName tx = configRegistry.beginConfig();
        CommitStatus commitStatus = configRegistry.commitConfig(tx);
        logger.debug("Committed blank transaction with status {}", commitStatus);
    }

    @Override
    public void modifiedService(ServiceReference<ModuleFactory> moduleFactoryServiceReference, Object o) {
        blankTransaction();
    }

    @Override
    public void removedService(ServiceReference<ModuleFactory> moduleFactoryServiceReference, Object o) {
        blankTransaction();
    }
}
