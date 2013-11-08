/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.manager.impl.dependencyresolver;

import org.opendaylight.controller.config.api.DependencyResolver;
import org.opendaylight.controller.config.api.JmxAttribute;
import org.opendaylight.controller.config.api.JmxAttributeValidationException;
import org.opendaylight.controller.config.api.ModuleIdentifier;
import org.opendaylight.controller.config.api.annotations.AbstractServiceInterface;
import org.opendaylight.controller.config.api.jmx.ObjectNameUtil;
import org.opendaylight.controller.config.manager.impl.TransactionStatus;
import org.opendaylight.controller.config.spi.Module;
import org.opendaylight.controller.config.spi.ModuleFactory;

import javax.annotation.concurrent.GuardedBy;
import javax.management.ObjectName;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import static java.lang.String.format;

/**
 * Protect {@link org.opendaylight.controller.config.spi.Module#getInstance()}
 * by creating proxy that would throw exception if those methods are called
 * during validation. Tracks dependencies for ordering purposes.
 */
final class DependencyResolverImpl implements DependencyResolver,
       Comparable<DependencyResolverImpl> {
    private final ModulesHolder modulesHolder;
    private final ModuleIdentifier name;
    private final TransactionStatus transactionStatus;
    @GuardedBy("this")
    private final Set<ModuleIdentifier> dependencies = new HashSet<>();

    DependencyResolverImpl(ModuleIdentifier currentModule,
            TransactionStatus transactionStatus, ModulesHolder modulesHolder) {
        this.name = currentModule;
        this.transactionStatus = transactionStatus;
        this.modulesHolder = modulesHolder;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void validateDependency(
            Class<? extends AbstractServiceInterface> expectedServiceInterface,
            ObjectName dependentModuleReadOnlyON, JmxAttribute jmxAttribute) {

        transactionStatus.checkNotCommitted();
        if (expectedServiceInterface == null) {
            throw new NullPointerException(
                    "Parameter 'expectedServiceInterface' is null");
        }
        if (jmxAttribute == null)
            throw new NullPointerException("Parameter 'jmxAttribute' is null");

        JmxAttributeValidationException.checkNotNull(dependentModuleReadOnlyON,
                "is null, " + "expected dependency implementing "
                        + expectedServiceInterface, jmxAttribute);

        // check that objectName belongs to this transaction - this should be
        // stripped
        // in DynamicWritableWrapper
        boolean hasTransaction = ObjectNameUtil
                .getTransactionName(dependentModuleReadOnlyON) != null;
        JmxAttributeValidationException.checkCondition(
                hasTransaction == false,
                format("ObjectName should not contain "
                        + "transaction name. %s set to %s. ", jmxAttribute,
                        dependentModuleReadOnlyON), jmxAttribute);

        ModuleIdentifier moduleIdentifier = ObjectNameUtil.fromON(dependentModuleReadOnlyON, ObjectNameUtil
                .TYPE_MODULE);

        ModuleFactory foundFactory = modulesHolder.findModuleFactory(moduleIdentifier, jmxAttribute);

        boolean implementsSI = foundFactory
                .isModuleImplementingServiceInterface(expectedServiceInterface);
        if (implementsSI == false) {
            String message = format(
                    "Found module factory does not expose expected service interface. "
                            + "Module name is %s : %s, expected service interface %s, dependent module ON %s , "
                            + "attribute %s",
                    foundFactory.getImplementationName(), foundFactory,
                    expectedServiceInterface, dependentModuleReadOnlyON,
                    jmxAttribute);
            throw new JmxAttributeValidationException(message, jmxAttribute);
        }
        synchronized (this) {
            dependencies.add(moduleIdentifier);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> T resolveInstance(Class<T> expectedType, ObjectName dependentON,
            JmxAttribute jmxAttribute) {
        if (expectedType == null || dependentON == null || jmxAttribute == null) {
            throw new IllegalArgumentException(format(
                    "Null parameters not allowed, got {} {} {}", expectedType,
                    dependentON, jmxAttribute));
        }

        transactionStatus.checkCommitStarted();
        transactionStatus.checkNotCommitted();

        ModuleIdentifier dependentModuleIdentifier = ObjectNameUtil.fromON(
                dependentON, ObjectNameUtil.TYPE_MODULE);
        Module module = modulesHolder.findModule(dependentModuleIdentifier,
                jmxAttribute);
        synchronized (this) {
            dependencies.add(dependentModuleIdentifier);
        }
        AutoCloseable instance = module.getInstance();
        if (instance == null) {
            String message = format(
                    "Error while %s resolving instance %s. getInstance() returned null. "
                            + "Expected type %s , attribute %s", name,
                    dependentModuleIdentifier, expectedType, jmxAttribute);
            throw new JmxAttributeValidationException(message, jmxAttribute);
        }
        try {
            T result = expectedType.cast(instance);
            return result;
        } catch (ClassCastException e) {
            String message = format(
                    "Instance cannot be cast to expected type. Instance class is %s , "
                            + "expected type %s , attribute %s",
                    instance.getClass(), expectedType, jmxAttribute);
            throw new JmxAttributeValidationException(message, e, jmxAttribute);
        }
    }

    @Override
    public int compareTo(DependencyResolverImpl o) {
        transactionStatus.checkCommitted();
        return Integer.compare(getMaxDependencyDepth(),
                o.getMaxDependencyDepth());
    }

    private Integer maxDependencyDepth;

    int getMaxDependencyDepth() {
        if (maxDependencyDepth == null) {
            throw new IllegalStateException("Dependency depth was not computed");
        }
        return maxDependencyDepth;
    }

    public void countMaxDependencyDepth(DependencyResolverManager manager) {
        transactionStatus.checkCommitted();
        if (maxDependencyDepth == null) {
            maxDependencyDepth = getMaxDepth(this, manager,
                    new LinkedHashSet<ModuleIdentifier>());
        }
    }

    private static int getMaxDepth(DependencyResolverImpl impl,
            DependencyResolverManager manager,
            LinkedHashSet<ModuleIdentifier> chainForDetectingCycles) {
        int maxDepth = 0;
        LinkedHashSet<ModuleIdentifier> chainForDetectingCycles2 = new LinkedHashSet<>(
                chainForDetectingCycles);
        chainForDetectingCycles2.add(impl.getIdentifier());
        for (ModuleIdentifier dependencyName : impl.dependencies) {
            DependencyResolverImpl dependentDRI = manager
                    .getOrCreate(dependencyName);
            if (chainForDetectingCycles2.contains(dependencyName)) {
                throw new IllegalStateException(format(
                        "Cycle detected, {} contains {}",
                        chainForDetectingCycles2, dependencyName));
            }
            int subDepth;
            if (dependentDRI.maxDependencyDepth != null) {
                subDepth = dependentDRI.maxDependencyDepth;
            } else {
                subDepth = getMaxDepth(dependentDRI, manager,
                        chainForDetectingCycles2);
                dependentDRI.maxDependencyDepth = subDepth;
            }
            if (subDepth + 1 > maxDepth) {
                maxDepth = subDepth + 1;
            }
        }
        impl.maxDependencyDepth = maxDepth;
        return maxDepth;
    }

    @Override
    public ModuleIdentifier getIdentifier() {
        return name;
    }
}
