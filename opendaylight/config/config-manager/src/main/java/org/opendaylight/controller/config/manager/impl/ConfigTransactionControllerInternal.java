/*
 * Copyright (c) 2013, 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.manager.impl;

import java.util.Collection;
import java.util.List;
import javax.management.ObjectName;
import org.opendaylight.controller.config.api.ModuleIdentifier;
import org.opendaylight.controller.config.api.ValidationException;
import org.opendaylight.controller.config.spi.ModuleFactory;
import org.osgi.framework.BundleContext;

/**
 * Defines contract between {@link ConfigTransactionControllerImpl} (producer)
 * and {@link ConfigRegistryImpl} (consumer).
 */
interface ConfigTransactionControllerInternal extends ConfigTransactionControllerImplMXBean, AutoCloseable {

    /**
     * 1, Copy already committed modules to current transaction. 2, Diff: compute
     * added and removed factories from last run, then create new modules using
     * {@link org.opendaylight.controller.config.spi
     * .ModuleFactory#getDefaultModules(org.opendaylight.controller
     * .config .api.DependencyResolverFactory, BundleContext)}
     * and remove modules belonging to removed factories.
     *
     * @param entries
     *            entries
     * @param lastListOfFactories
     *            list of factories
     */
    void copyExistingModulesAndProcessFactoryDiff(Collection<ModuleInternalInfo> entries,
            List<ModuleFactory> lastListOfFactories);

    /**
     * Call {@link org.opendaylight.controller.config.spi.Module#validate()} on all
     * beans in transaction. Lock transaction after successful validation. This
     * method can be called multiple times if validation fails, but cannot be called
     * after it did not throw exception.
     *
     * @return CommitInfo commit info
     * @throws {@linkRuntimeException}
     *             if validation fails. It is safe to run this method in future
     */
    CommitInfo validateBeforeCommitAndLockTransaction() throws ValidationException;

    /**
     * Call {@link org.opendaylight.controller.config.spi.Module#getInstance()} on
     * all beans in transaction. This method can be only called once.
     *
     * @return ordered list of module identifiers that respects dependency order.
     * @throws {@link
     *             RuntimeException} commit fails, indicates bug in config bean
     */
    List<ModuleIdentifier> secondPhaseCommit();

    /**
     * Gets the objectName of this transaction controller.
     *
     * @return ObjectName
     */
    ObjectName getControllerObjectName();

    /**
     * Check if the transaction was committed or aborted.
     *
     * @return result of transaction
     */
    boolean isClosed();

    List<ModuleFactory> getCurrentlyRegisteredFactories();

    BundleContext getModuleFactoryBundleContext(String factoryName);

    SearchableServiceReferenceWritableRegistry getWritableRegistry();

    TransactionIdentifier getTransactionIdentifier();

    @Override
    void close();
}
