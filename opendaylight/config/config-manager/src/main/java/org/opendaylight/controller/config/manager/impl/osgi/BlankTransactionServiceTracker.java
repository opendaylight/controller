/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.manager.impl.osgi;

import org.opendaylight.controller.config.api.ConflictingVersionException;
import org.opendaylight.controller.config.api.ValidationException;
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
 * {@link org.opendaylight.controller.config.spi.ModuleFactory#getDefaultModules(org.opendaylight.controller.config.api.DependencyResolverFactory, org.osgi.framework.BundleContext)}
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

    synchronized void blankTransaction() {
        // race condition check: config-persister might push new configuration while server is starting up.
        ConflictingVersionException lastException = null;
        int maxAttempts = 10;
        for (int i = 0; i < maxAttempts; i++) {
            try {
                // create transaction
                boolean blankTransaction = true;
                ObjectName tx = configRegistry.beginConfig(blankTransaction);
                CommitStatus commitStatus = configRegistry.commitConfig(tx);
                logger.debug("Committed blank transaction with status {}", commitStatus);
                return;
            } catch (ConflictingVersionException e) {
                lastException = e;
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException interruptedException) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(interruptedException);
                }
            } catch (ValidationException e) {
                logger.error("Validation exception while running blank transaction indicates programming error", e);
                throw new RuntimeException("Validation exception while running blank transaction indicates programming error", e);
            }
        }
        throw new RuntimeException("Maximal number of attempts reached and still cannot get optimistic lock from " +
                "config manager",lastException);
    }

    @Override
    public void modifiedService(ServiceReference <ModuleFactory> moduleFactoryServiceReference, Object o) {
        blankTransaction();
    }

    @Override
    public void removedService(ServiceReference<ModuleFactory> moduleFactoryServiceReference, Object o) {
        blankTransaction();
    }
}
