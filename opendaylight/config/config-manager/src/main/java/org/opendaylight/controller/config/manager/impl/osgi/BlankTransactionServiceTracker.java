/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.manager.impl.osgi;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.management.ObjectName;
import org.opendaylight.controller.config.api.ConflictingVersionException;
import org.opendaylight.controller.config.api.ValidationException;
import org.opendaylight.controller.config.api.jmx.CommitStatus;
import org.opendaylight.controller.config.manager.impl.ConfigRegistryImpl;
import org.opendaylight.controller.config.spi.ModuleFactory;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Every time factory is added or removed, blank transaction is triggered to handle
 * {@link org.opendaylight.controller.config.spi.ModuleFactory#getDefaultModules(org.opendaylight.controller.config.api.DependencyResolverFactory, org.osgi.framework.BundleContext)}
 * functionality.
 */
public class BlankTransactionServiceTracker implements ServiceTrackerCustomizer<ModuleFactory, Object> {
    private static final Logger LOG = LoggerFactory.getLogger(BlankTransactionServiceTracker.class);

    public static final int DEFAULT_MAX_ATTEMPTS = 10;

    private final BlankTransaction blankTransaction;
    private final ExecutorService txExecutor;
    private final int maxAttempts;

    public BlankTransactionServiceTracker(final ConfigRegistryImpl configRegistry) {
        this(new BlankTransaction() {
            @Override
            public CommitStatus hit()
                    throws ValidationException, ConflictingVersionException {
                ObjectName tx = configRegistry.beginConfig(true);
                return configRegistry.commitConfig(tx);
            }
        });
    }

    public BlankTransactionServiceTracker(final BlankTransaction blankTransaction) {
        this(blankTransaction, DEFAULT_MAX_ATTEMPTS, Executors.newSingleThreadExecutor(new ThreadFactoryBuilder()
                .setNameFormat("config-blank-txn-%d").build()));
    }

    @VisibleForTesting
    BlankTransactionServiceTracker(final BlankTransaction blankTx, final int maxAttempts,
            final ExecutorService txExecutor) {
        this.blankTransaction = blankTx;
        this.maxAttempts = maxAttempts;
        this.txExecutor = txExecutor;
    }

    @Override
    public Object addingService(ServiceReference<ModuleFactory> moduleFactoryServiceReference) {
        blankTransactionAsync();
        return null;
    }

    private void blankTransactionAsync() {
        txExecutor.execute(() -> { blankTransactionSync(); });
    }

    void blankTransactionSync() {
        // race condition check: config-persister might push new configuration while server is starting up.
        ConflictingVersionException lastException = null;
        for (int i = 0; i < maxAttempts; i++) {
            try {
                // create transaction
                CommitStatus commitStatus = blankTransaction.hit();
                LOG.debug("Committed blank transaction with status {}", commitStatus);
                return;
            } catch (ConflictingVersionException e) {
                lastException = e;
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException interruptedException) {
                    Thread.currentThread().interrupt();
                    LOG.debug("blankTransactionSync was interrupted");
                    return;
                }
            } catch (ValidationException e) {
                LOG.error("Validation exception while running blank transaction indicates programming error", e);
            }
        }

        LOG.error("Maximal number of attempts reached and still cannot get optimistic lock from config manager",
                lastException);
    }

    @Override
    public void modifiedService(ServiceReference <ModuleFactory> moduleFactoryServiceReference, Object o) {
        blankTransactionAsync();
    }

    @Override
    public void removedService(ServiceReference<ModuleFactory> moduleFactoryServiceReference, Object o) {
        blankTransactionAsync();
    }

    @VisibleForTesting
    interface BlankTransaction {
        CommitStatus hit() throws ValidationException, ConflictingVersionException;
    }
}
