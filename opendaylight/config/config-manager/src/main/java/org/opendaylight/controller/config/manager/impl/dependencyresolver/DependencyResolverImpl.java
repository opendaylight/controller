/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.manager.impl.dependencyresolver;

import static java.lang.String.format;

import com.google.common.base.Preconditions;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import javax.annotation.concurrent.GuardedBy;
import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.JMX;
import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import org.opendaylight.controller.config.api.DependencyResolver;
import org.opendaylight.controller.config.api.IdentityAttributeRef;
import org.opendaylight.controller.config.api.JmxAttribute;
import org.opendaylight.controller.config.api.JmxAttributeValidationException;
import org.opendaylight.controller.config.api.ModuleIdentifier;
import org.opendaylight.controller.config.api.ServiceReferenceReadableRegistry;
import org.opendaylight.controller.config.api.annotations.AbstractServiceInterface;
import org.opendaylight.controller.config.api.jmx.ObjectNameUtil;
import org.opendaylight.controller.config.manager.impl.TransactionStatus;
import org.opendaylight.controller.config.spi.Module;
import org.opendaylight.controller.config.spi.ModuleFactory;
import org.opendaylight.yangtools.yang.binding.BaseIdentity;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.impl.codec.CodecRegistry;
import org.opendaylight.yangtools.yang.data.impl.codec.IdentityCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Protect {@link org.opendaylight.controller.config.spi.Module#getInstance()}
 * by creating proxy that would throw exception if those methods are called
 * during validation. Tracks dependencies for ordering purposes.
 */
final class DependencyResolverImpl implements DependencyResolver,
        Comparable<DependencyResolverImpl> {
    private static final Logger LOG = LoggerFactory.getLogger(DependencyResolverImpl.class);

    private final ModulesHolder modulesHolder;
    private final ModuleIdentifier name;
    private final TransactionStatus transactionStatus;
    @GuardedBy("this")
    private final Set<ModuleIdentifier> dependencies = new HashSet<>();
    private final ServiceReferenceReadableRegistry readableRegistry;
    private final CodecRegistry codecRegistry;
    private final String transactionName;
    private final MBeanServer mBeanServer;

    DependencyResolverImpl(ModuleIdentifier currentModule,
                           TransactionStatus transactionStatus, ModulesHolder modulesHolder,
                           ServiceReferenceReadableRegistry readableRegistry, CodecRegistry codecRegistry,
                           String transactionName, MBeanServer mBeanServer) {
        this.codecRegistry = codecRegistry;
        this.name = currentModule;
        this.transactionStatus = transactionStatus;
        this.modulesHolder = modulesHolder;
        this.readableRegistry = readableRegistry;
        this.transactionName = transactionName;
        this.mBeanServer = mBeanServer;
    }

    /**
     * {@inheritDoc}
     */
    //TODO: check for cycles
    @Override
    public void validateDependency(
            Class<? extends AbstractServiceInterface> expectedServiceInterface,
            ObjectName dependentReadOnlyON, JmxAttribute jmxAttribute) {

        transactionStatus.checkNotCommitted();
        if (expectedServiceInterface == null) {
            throw new NullPointerException(
                    "Parameter 'expectedServiceInterface' is null");
        }
        if (jmxAttribute == null) {
            throw new NullPointerException("Parameter 'jmxAttribute' is null");
        }

        JmxAttributeValidationException.checkNotNull(dependentReadOnlyON,
                "is null, expected dependency implementing "
                        + expectedServiceInterface, jmxAttribute
        );


        // check that objectName belongs to this transaction - this should be
        // stripped
        // in DynamicWritableWrapper
        boolean hasTransaction = ObjectNameUtil
                .getTransactionName(dependentReadOnlyON) != null;
        JmxAttributeValidationException.checkCondition(
                hasTransaction == false,
                format("ObjectName should not contain "
                                + "transaction name. %s set to %s. ", jmxAttribute,
                        dependentReadOnlyON
            ), jmxAttribute
        );

        ObjectName newDependentReadOnlyON = translateServiceRefIfPossible(dependentReadOnlyON);

        ModuleIdentifier moduleIdentifier = ObjectNameUtil.fromON(newDependentReadOnlyON, ObjectNameUtil
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
                    expectedServiceInterface, newDependentReadOnlyON,
                    jmxAttribute
            );
            throw new JmxAttributeValidationException(message, jmxAttribute);
        }
        synchronized (this) {
            dependencies.add(moduleIdentifier);
        }
    }

    // translate from serviceref to module ON
    private ObjectName translateServiceRefIfPossible(ObjectName dependentReadOnlyON) {
        ObjectName translatedDependentReadOnlyON = dependentReadOnlyON;
        if (ObjectNameUtil.isServiceReference(translatedDependentReadOnlyON)) {
            String serviceQName = ObjectNameUtil.getServiceQName(translatedDependentReadOnlyON);
            String refName = ObjectNameUtil.getReferenceName(translatedDependentReadOnlyON);
            translatedDependentReadOnlyON = ObjectNameUtil.withoutTransactionName( // strip again of transaction name
                    readableRegistry.lookupConfigBeanByServiceInterfaceName(serviceQName, refName));
        }
        return translatedDependentReadOnlyON;
    }

    /**
     * {@inheritDoc}
     */
    //TODO: check for cycles
    @Override
    public <T> T resolveInstance(Class<T> expectedType, ObjectName dependentReadOnlyON,
                                 JmxAttribute jmxAttribute) {
        Module module = resolveModuleInstance(dependentReadOnlyON, jmxAttribute);

        synchronized (this) {
            dependencies.add(module.getIdentifier());
        }
        AutoCloseable instance = module.getInstance();
        if (instance == null) {
            String message = format(
                    "Error while %s resolving instance %s. getInstance() returned null. "
                            + "Expected type %s , attribute %s", name,
                    module.getIdentifier(), expectedType, jmxAttribute
            );
            throw new JmxAttributeValidationException(message, jmxAttribute);
        }
        try {
            return expectedType.cast(instance);
        } catch (ClassCastException e) {
            String message = format(
                    "Instance cannot be cast to expected type. Instance class is %s , "
                            + "expected type %s , attribute %s",
                    instance.getClass(), expectedType, jmxAttribute
            );
            throw new JmxAttributeValidationException(message, e, jmxAttribute);
        }
    }

    private Module resolveModuleInstance(ObjectName dependentReadOnlyON,
                                 JmxAttribute jmxAttribute) {
        Preconditions.checkArgument(dependentReadOnlyON != null ,"dependentReadOnlyON");
        Preconditions.checkArgument(jmxAttribute != null, "jmxAttribute");
        ObjectName translatedDependentReadOnlyON = translateServiceRefIfPossible(dependentReadOnlyON);
        transactionStatus.checkCommitStarted();
        transactionStatus.checkNotCommitted();

        ModuleIdentifier dependentModuleIdentifier = ObjectNameUtil.fromON(
                translatedDependentReadOnlyON, ObjectNameUtil.TYPE_MODULE);

        return Preconditions.checkNotNull(modulesHolder.findModule(dependentModuleIdentifier, jmxAttribute));
    }

    @Override
    public boolean canReuseDependency(ObjectName objectName, JmxAttribute jmxAttribute) {
        Preconditions.checkNotNull(objectName);
        Preconditions.checkNotNull(jmxAttribute);

        Module currentModule = resolveModuleInstance(objectName, jmxAttribute);
        ModuleIdentifier identifier = currentModule.getIdentifier();
        ModuleInternalTransactionalInfo moduleInternalTransactionalInfo = modulesHolder.findModuleInternalTransactionalInfo(identifier);

        if(moduleInternalTransactionalInfo.hasOldModule()) {
            Module oldModule = moduleInternalTransactionalInfo.getOldInternalInfo().getReadableModule().getModule();
            return currentModule.canReuse(oldModule);
        }
        return false;
    }

    @Override
    public <T extends BaseIdentity> Class<? extends T> resolveIdentity(IdentityAttributeRef identityRef, Class<T> expectedBaseClass) {
        final QName qName = QName.create(identityRef.getqNameOfIdentity());
        IdentityCodec<?> identityCodec = codecRegistry.getIdentityCodec();
        Class<? extends BaseIdentity> deserialized = identityCodec.deserialize(qName);
        if (deserialized == null) {
            throw new IllegalStateException("Unable to retrieve identity class for " + qName + ", null response from "
                    + codecRegistry);
        }
        if (expectedBaseClass.isAssignableFrom(deserialized)) {
            return (Class<T>) deserialized;
        } else {
            LOG.error("Cannot resolve class of identity {} : deserialized class {} is not a subclass of {}.",
                    identityRef, deserialized, expectedBaseClass);
            throw new IllegalArgumentException("Deserialized identity " + deserialized + " cannot be cast to " + expectedBaseClass);
        }
    }

    @Override
    public <T extends BaseIdentity> void validateIdentity(IdentityAttributeRef identityRef, Class<T> expectedBaseClass, JmxAttribute jmxAttribute) {
        try {
            resolveIdentity(identityRef, expectedBaseClass);
        } catch (Exception e) {
            throw JmxAttributeValidationException.wrap(e, jmxAttribute);
        }
    }

    @Override
    public int compareTo(DependencyResolverImpl o) {
        transactionStatus.checkCommitStarted();
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

    void countMaxDependencyDepth(DependencyResolverManager manager) {
        // We can calculate the dependency after second phase commit was started
        // Second phase commit starts after validation and validation adds the dependencies into the dependency resolver, which are necessary for the calculation
        // FIXME generated code for abstract module declares validate method as non-final
        // Overriding the validate would cause recreate every time instead of reuse + also possibly wrong close order if there is another module depending
        transactionStatus.checkCommitStarted();
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
                        "Cycle detected, %s contains %s",
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

    @Override
    public Object getAttribute(ObjectName name, String attribute)
            throws MBeanException, AttributeNotFoundException, InstanceNotFoundException, ReflectionException {
        ObjectName newName = translateServiceRefIfPossible(name);
        // add transaction name
        newName = ObjectNameUtil.withTransactionName(newName, transactionName);
        return mBeanServer.getAttribute(newName, attribute);
    }

    @Override
    public <T> T newMXBeanProxy(ObjectName name, Class<T> interfaceClass) {
        ObjectName newName = translateServiceRefIfPossible(name);
        // add transaction name
        newName = ObjectNameUtil.withTransactionName(newName, transactionName);
        return JMX.newMXBeanProxy(mBeanServer, newName, interfaceClass);
    }
}
