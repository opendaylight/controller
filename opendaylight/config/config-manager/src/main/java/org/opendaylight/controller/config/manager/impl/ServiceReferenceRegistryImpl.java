/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.manager.impl;

import org.opendaylight.controller.config.api.LookupRegistry;
import org.opendaylight.controller.config.api.ModuleIdentifier;
import org.opendaylight.controller.config.api.ServiceReferenceReadableRegistry;
import org.opendaylight.controller.config.api.ServiceReferenceWritableRegistry;
import org.opendaylight.controller.config.api.annotations.ServiceInterfaceAnnotation;
import org.opendaylight.controller.config.api.jmx.ObjectNameUtil;
import org.opendaylight.controller.config.manager.impl.jmx.BaseJMXRegistrator;
import org.opendaylight.controller.config.manager.impl.jmx.ServiceReference;
import org.opendaylight.controller.config.manager.impl.jmx.ServiceReferenceMXBeanImpl;
import org.opendaylight.controller.config.manager.impl.jmx.ServiceReferenceRegistrator;
import org.opendaylight.controller.config.manager.impl.jmx.ServiceReferenceRegistrator.ServiceReferenceJMXRegistration;
import org.opendaylight.controller.config.manager.impl.jmx.ServiceReferenceRegistrator.ServiceReferenceTransactionRegistratorFactory;
import org.opendaylight.controller.config.manager.impl.jmx.ServiceReferenceRegistrator.ServiceReferenceTransactionRegistratorFactoryImpl;
import org.opendaylight.controller.config.manager.impl.util.InterfacesHelper;
import org.opendaylight.controller.config.spi.ModuleFactory;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.ObjectName;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class ServiceReferenceRegistryImpl implements CloseableServiceReferenceReadableRegistry, ServiceReferenceWritableRegistry {
    private static final Logger logger = LoggerFactory.getLogger(ServiceReferenceRegistryImpl.class);

    private final Map<String, ModuleFactory> factories;
    private final Map<String, Set<String>> factoryNamesToQNames;
    // validator of incoming ObjectNames - throws InstanceNotFoundException if not found either in registry or transaction
    private final LookupRegistry lookupRegistry;
    private final ServiceReferenceRegistrator serviceReferenceRegistrator;
    // helper method for getting QName of SI from namespace + local name
    private final Map<String /* namespace */, Map<String /* local name */, ServiceInterfaceAnnotation>> namespacesToAnnotations;
    // all Service Interface qNames for sanity checking
    private final Set<String /* qName */> allQNames;

    // actual reference database
    private final Map<ServiceReference, ModuleIdentifier> refNames = new HashMap<>();
    private final boolean writable;
    private final Map<ServiceReference, Entry<ServiceReferenceMXBeanImpl, ServiceReferenceJMXRegistration>> mBeans = new HashMap<>();

    /**
     * Static constructor for config registry. Since only transaction can write to this registry, it will
     * return blank state.
     */
    public static CloseableServiceReferenceReadableRegistry createInitialSRLookupRegistry() {
        // since this is initial state, just throw exception:
        LookupRegistry lookupRegistry = new LookupRegistry() {
            @Override
            public Set<ObjectName> lookupConfigBeans() {
                throw new UnsupportedOperationException();
            }

            @Override
            public Set<ObjectName> lookupConfigBeans(String moduleName) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Set<ObjectName> lookupConfigBeans(String moduleName, String instanceName) {
                throw new UnsupportedOperationException();
            }

            @Override
            public ObjectName lookupConfigBean(String moduleName, String instanceName) throws InstanceNotFoundException {
                throw new UnsupportedOperationException();
            }

            @Override
            public void checkConfigBeanExists(ObjectName objectName) throws InstanceNotFoundException {
                throw new InstanceNotFoundException("Cannot find " + objectName + " - Tried to use mocking registry");
            }

            @Override
            public Set<String> getAvailableModuleFactoryQNames() {
                throw new UnsupportedOperationException();
            }

            @Override
            public String toString() {
                return "initial";
            }
        };
        ServiceReferenceTransactionRegistratorFactory serviceReferenceRegistratorFactory = new ServiceReferenceTransactionRegistratorFactory(){
            @Override
            public ServiceReferenceRegistrator create() {
                return new ServiceReferenceRegistrator() {
                    @Override
                    public String getNullableTransactionName() {
                        throw new UnsupportedOperationException();
                    }

                    @Override
                    public ServiceReferenceJMXRegistration registerMBean(ServiceReferenceMXBeanImpl object, ObjectName on) throws InstanceAlreadyExistsException {
                        throw new UnsupportedOperationException();
                    }

                    @Override
                    public void close() {

                    }
                };
            }
        };
        return new ServiceReferenceRegistryImpl(Collections.<String, ModuleFactory>emptyMap(), lookupRegistry,
                serviceReferenceRegistratorFactory, false);
    }

    /**
     * Static constructor for transaction controller. Take current state as seen by config registry, allow writing new data.
     */
    public static ServiceReferenceWritableRegistry createSRWritableRegistry(ServiceReferenceReadableRegistry oldReadableRegistry,
                                                    ConfigTransactionLookupRegistry txLookupRegistry,
                                                    Map<String, Map.Entry<ModuleFactory, BundleContext>> currentlyRegisteredFactories) {

        if (txLookupRegistry == null) {
            throw new IllegalArgumentException("txLookupRegistry is null");
        }
        ServiceReferenceRegistryImpl old = (ServiceReferenceRegistryImpl) oldReadableRegistry;
        Map<String, ModuleFactory> factories = extractFactoriesMap(currentlyRegisteredFactories);
        ServiceReferenceTransactionRegistratorFactory serviceReferenceRegistratorFactory = new ServiceReferenceTransactionRegistratorFactoryImpl(
                txLookupRegistry.getTxModuleJMXRegistrator(), txLookupRegistry.getTxModuleJMXRegistrator().getTransactionName());
        ServiceReferenceRegistryImpl newRegistry = new ServiceReferenceRegistryImpl(factories, txLookupRegistry,
                serviceReferenceRegistratorFactory, true);
        copy(old, newRegistry, txLookupRegistry.getTransactionIdentifier().getName());
        return newRegistry;
    }

    /**
     * Copy back state to config registry after commit.
     */
    public static CloseableServiceReferenceReadableRegistry createSRReadableRegistry(ServiceReferenceWritableRegistry oldWritableRegistry,
                                                                            LookupRegistry lookupRegistry, BaseJMXRegistrator baseJMXRegistrator) {
        ServiceReferenceRegistryImpl old = (ServiceReferenceRegistryImpl) oldWritableRegistry;

        // even if factories do change, nothing in the mapping can change between transactions
        ServiceReferenceTransactionRegistratorFactory serviceReferenceRegistratorFactory = new ServiceReferenceTransactionRegistratorFactoryImpl(baseJMXRegistrator);
        ServiceReferenceRegistryImpl newRegistry = new ServiceReferenceRegistryImpl(old.factories, lookupRegistry,
                serviceReferenceRegistratorFactory, false);
        copy(old, newRegistry, null);
        return newRegistry;
    }

    /**
     * Fill refNames and mBeans maps from old instance
     */
    private static void copy(ServiceReferenceRegistryImpl old, ServiceReferenceRegistryImpl newRegistry, String nullableDstTransactionName) {
        for (Entry<ServiceReference, Entry<ServiceReferenceMXBeanImpl, ServiceReferenceJMXRegistration>> refNameEntry : old.mBeans.entrySet()) {
            ObjectName currentImplementation;
            ObjectName currentImplementationSrc = refNameEntry.getValue().getKey().getCurrentImplementation();
            if (nullableDstTransactionName != null) {
                currentImplementation = ObjectNameUtil.withTransactionName(currentImplementationSrc, nullableDstTransactionName);
            } else {
                currentImplementation = ObjectNameUtil.withoutTransactionName(currentImplementationSrc);
            }
            try {
                boolean skipChecks = true;
                newRegistry.saveServiceReference(refNameEntry.getKey(), currentImplementation, skipChecks);
            } catch (InstanceNotFoundException e) {
                logger.error("Cannot save service reference({}, {})", refNameEntry.getKey(), currentImplementation);
                throw new IllegalStateException("Possible code error", e);
            }
        }
    }

    private static Map<String, ModuleFactory> extractFactoriesMap(Map<String, Map.Entry<ModuleFactory, BundleContext>> currentlyRegisteredFactories) {
        Map<String, ModuleFactory> result = new HashMap<>();
        for (Entry<String, Entry<ModuleFactory, BundleContext>> entry : currentlyRegisteredFactories.entrySet()) {
            result.put(entry.getKey(), entry.getValue().getKey());
        }
        return result;
    }

    private ServiceReferenceRegistryImpl(Map<String, ModuleFactory> factories, LookupRegistry lookupRegistry,
                                         ServiceReferenceTransactionRegistratorFactory serviceReferenceRegistratorFactory,
                                         boolean writable) {
        this.factories = factories;
        this.writable = writable;
        this.lookupRegistry = lookupRegistry;

        this.serviceReferenceRegistrator = serviceReferenceRegistratorFactory.create();

        Map<String, Set<String /* QName */>> modifiableFactoryNamesToQNames = new HashMap<>();
        Set<ServiceInterfaceAnnotation> allAnnotations = new HashSet<>();
        Set<String /* qName */> allQNames = new HashSet<>();
        for (Entry<String, ModuleFactory> entry : factories.entrySet()) {
            if (entry.getKey().equals(entry.getValue().getImplementationName()) == false) {
                logger.error("Possible error in code: Mismatch between supplied and actual name of {}", entry);
                throw new IllegalArgumentException("Possible error in code: Mismatch between supplied and actual name of " + entry);
            }
            Set<ServiceInterfaceAnnotation> siAnnotations = InterfacesHelper.getServiceInterfaceAnnotations(entry.getValue());
            Set<String> qNames = new HashSet<>();
            for (ServiceInterfaceAnnotation sia: siAnnotations) {
                qNames.add(sia.value());
            }
            allAnnotations.addAll(siAnnotations);
            allQNames.addAll(qNames);
            modifiableFactoryNamesToQNames.put(entry.getKey(), Collections.unmodifiableSet(qNames));
        }
        this.factoryNamesToQNames = Collections.unmodifiableMap(modifiableFactoryNamesToQNames);
        this.allQNames = Collections.unmodifiableSet(allQNames);
        // fill namespacesToAnnotations
        Map<String /* namespace */, Map<String /* localName */, ServiceInterfaceAnnotation>> modifiableNamespacesToAnnotations =
                new HashMap<>();
        for (ServiceInterfaceAnnotation sia : allAnnotations) {
            Map<String, ServiceInterfaceAnnotation> ofNamespace = modifiableNamespacesToAnnotations.get(sia.namespace());
            if (ofNamespace == null) {
                ofNamespace = new HashMap<>();
                modifiableNamespacesToAnnotations.put(sia.namespace(), ofNamespace);
            }
            if (ofNamespace.containsKey(sia.localName())) {
                logger.error("Cannot construct namespacesToAnnotations map, conflict between local names in {}, offending local name: {}, map so far {}",
                        sia.namespace(), sia.localName(), modifiableNamespacesToAnnotations);
                throw new IllegalArgumentException("Conflict between local names in " + sia.namespace() + " : " + sia.localName());
            }
            ofNamespace.put(sia.localName(), sia);
        }
        this.namespacesToAnnotations = Collections.unmodifiableMap(modifiableNamespacesToAnnotations);
        // copy refNames
        logger.trace("factoryNamesToQNames:{}", this.factoryNamesToQNames);
    }


    @Override
    public synchronized Set<String> lookupServiceInterfaceNames(ObjectName objectName) throws InstanceNotFoundException {
        lookupRegistry.checkConfigBeanExists(objectName);

        String factoryName = ObjectNameUtil.getFactoryName(objectName);
        Set<String> serviceInterfaceAnnotations = factoryNamesToQNames.get(factoryName);
        if (serviceInterfaceAnnotations == null) {
            logger.error("Possible error in code: cannot find factory annotations of '{}' extracted from ON {} in {}",
                    factoryName, objectName, factoryNamesToQNames);
            throw new IllegalArgumentException("Cannot find factory with name " + factoryName);
        }
        return serviceInterfaceAnnotations;
    }

    @Override
    public synchronized String getServiceInterfaceName(String namespace, String localName) {
        Map<String /* localName */, ServiceInterfaceAnnotation> ofNamespace = namespacesToAnnotations.get(namespace);
        if (ofNamespace == null) {
            logger.error("Cannot find namespace {} in {}", namespace, namespacesToAnnotations);
            throw new IllegalArgumentException("Cannot find namespace " + namespace);
        }
        ServiceInterfaceAnnotation sia = ofNamespace.get(localName);
        if (sia == null) {
            logger.error("Cannot find local name {} in namespace {}, found only {}", localName, namespace, ofNamespace);
            throw new IllegalArgumentException("Cannot find local name " + localName + " in namespace " + namespace);
        }
        return sia.value();
    }

    // reading:

    @Override
    public synchronized Map<String /* serviceInterfaceName */, Map<String/* refName */, ObjectName>> getServiceMapping() {
        Map<String /* serviceInterfaceName */, Map<String/* refName */, ObjectName>> result = new HashMap<>();
        for (Entry<ServiceReference, ModuleIdentifier> entry: refNames.entrySet()) {
            String qName = entry.getKey().getServiceInterfaceName();
            Map<String /* refName */, ObjectName> innerMap = result.get(qName);
            if (innerMap == null) {
                innerMap = new HashMap<>();
                result.put(qName, innerMap);
            }
            innerMap.put(entry.getKey().getRefName(), getObjectName(entry.getValue()));
        }
        return result;
    }

    private ObjectName getObjectName(ModuleIdentifier moduleIdentifier) {
        ObjectName on;
        try {
            on = lookupRegistry.lookupConfigBean(moduleIdentifier.getFactoryName(), moduleIdentifier.getInstanceName());
        } catch (InstanceNotFoundException e) {
            logger.error("Cannot find instance {}", moduleIdentifier);
            throw new IllegalStateException("Cannot find instance " + moduleIdentifier, e);
        }
        return on;
    }

    @Override
    public synchronized ObjectName lookupConfigBeanByServiceInterfaceName(String serviceInterfaceQName, String refName) {
        ServiceReference serviceReference = new ServiceReference(serviceInterfaceQName, refName);
        ModuleIdentifier moduleIdentifier = refNames.get(serviceReference);
        if (moduleIdentifier == null) {
            logger.error("Cannot find qname {} and refName {} in {}", serviceInterfaceQName, refName, refName);
            throw new IllegalArgumentException("Cannot find " + serviceReference);
        }
        return getObjectName(moduleIdentifier);
    }

    @Override
    public synchronized Map<String /* refName */, ObjectName> lookupServiceReferencesByServiceInterfaceName(String serviceInterfaceQName) {
        Map<String, Map<String, ObjectName>> serviceMapping = getServiceMapping();
        Map<String, ObjectName> innerMap = serviceMapping.get(serviceInterfaceQName);
        if (innerMap == null) {
            logger.error("Cannot find qname {} in {}", serviceInterfaceQName, refNames);
            throw new IllegalArgumentException("Cannot find " + serviceInterfaceQName);
        }
        return innerMap;
    }

    @Override
    public synchronized ObjectName getServiceReference(String serviceInterfaceQName, String refName) throws InstanceNotFoundException {
        ServiceReference serviceReference = new ServiceReference(serviceInterfaceQName, refName);
        if (mBeans.containsKey(serviceReference) == false) {
            throw new InstanceNotFoundException("Cannot find " + serviceReference);
        }
        return getServiceON(serviceReference);
    }

    @Override
    public synchronized void checkServiceReferenceExists(ObjectName objectName) throws InstanceNotFoundException {
        String actualTransactionName = ObjectNameUtil.getTransactionName(objectName);
        String expectedTransactionName = serviceReferenceRegistrator.getNullableTransactionName();
        if (writable & actualTransactionName == null || (writable && actualTransactionName.equals(expectedTransactionName) == false)) {
            throw new IllegalArgumentException("Mismatched transaction name in " + objectName);
        }
        String serviceQName = ObjectNameUtil.getServiceQName(objectName);
        String referenceName = ObjectNameUtil.getReferenceName(objectName);
        ServiceReference serviceReference = new ServiceReference(serviceQName, referenceName);
        if (refNames.containsKey(serviceReference) == false) {
            logger.warn("Cannot find {} in {}", serviceReference, refNames);
            throw new InstanceNotFoundException("Service reference not found:" + objectName);
        }
    }

    // writing:

    private void assertWritable() {
        if (writable == false) {
            throw new IllegalStateException("Cannot write to readable registry");
        }
    }

    @Override
    public synchronized ObjectName saveServiceReference(String serviceInterfaceName, String refName, ObjectName moduleON)  throws InstanceNotFoundException {
        assertWritable();
        ServiceReference serviceReference = new ServiceReference(serviceInterfaceName, refName);
        return saveServiceReference(serviceReference, moduleON);
    }

    private synchronized ObjectName saveServiceReference(ServiceReference serviceReference, ObjectName moduleON)
            throws InstanceNotFoundException{
        return saveServiceReference(serviceReference, moduleON, false);
    }

    private synchronized ObjectName saveServiceReference(ServiceReference serviceReference, ObjectName moduleON,
                                                         boolean skipChecks) throws InstanceNotFoundException {

        // make sure it is found
        if (skipChecks == false) {
            lookupRegistry.checkConfigBeanExists(moduleON);
        }
        String factoryName = ObjectNameUtil.getFactoryName(moduleON);
        String instanceName = ObjectNameUtil.getInstanceName(moduleON);
        ModuleIdentifier moduleIdentifier = new ModuleIdentifier(factoryName, instanceName);

        // check that service interface name exist
        Set<String> serviceInterfaceQNames = factoryNamesToQNames.get(moduleIdentifier.getFactoryName());
        if (serviceInterfaceQNames == null) {
            logger.error("Possible error in code: cannot find factoryName {} in {}, {}", moduleIdentifier.getFactoryName(),
                    factoryNamesToQNames, moduleIdentifier);
            throw new IllegalStateException("Possible error in code: cannot find annotations of existing factory " + moduleIdentifier.getFactoryName());
        }
        // supplied serviceInterfaceName must exist in this collection
        if (serviceInterfaceQNames.contains(serviceReference.getServiceInterfaceName()) == false) {
            logger.error("Cannot find qName {} with factory name {}, found {}", serviceReference.getServiceInterfaceName(), moduleIdentifier.getFactoryName(), serviceInterfaceQNames);
            throw new IllegalArgumentException("Cannot find service interface " + serviceReference.getServiceInterfaceName() + " within factory " + moduleIdentifier.getFactoryName());
        }


        // create service reference object name, put to mBeans
        ObjectName result = getServiceON(serviceReference);
        Entry<ServiceReferenceMXBeanImpl, ServiceReferenceJMXRegistration> mxBeanEntry = mBeans.get(serviceReference);
        if (mxBeanEntry == null) {
            // create dummy mx bean
            ServiceReferenceMXBeanImpl dummyMXBean = new ServiceReferenceMXBeanImpl(moduleON);
            ServiceReferenceJMXRegistration dummyMXBeanRegistration;
            try {
                dummyMXBeanRegistration = serviceReferenceRegistrator.registerMBean(dummyMXBean, result);
            } catch (InstanceAlreadyExistsException e) {
                throw new IllegalStateException("Possible error in code. Cannot register " + result, e);
            }
            mBeans.put(serviceReference, createMXBeanEntry(dummyMXBean, dummyMXBeanRegistration));
        } else {
            // update
            mxBeanEntry.getKey().setCurrentImplementation(moduleON);
        }
        // save to refNames
        refNames.put(serviceReference, moduleIdentifier);
        return result;
    }

    private Entry<ServiceReferenceMXBeanImpl, ServiceReferenceJMXRegistration> createMXBeanEntry(
            final ServiceReferenceMXBeanImpl mxBean, final ServiceReferenceJMXRegistration registration) {
        return new Entry<ServiceReferenceMXBeanImpl, ServiceReferenceJMXRegistration>() {
            @Override
            public ServiceReferenceMXBeanImpl getKey() {
                return mxBean;
            }

            @Override
            public ServiceReferenceJMXRegistration getValue() {
                return registration;
            }

            @Override
            public ServiceReferenceJMXRegistration setValue(ServiceReferenceJMXRegistration value) {
                throw new UnsupportedOperationException();
            }
        };
    }

    private ObjectName getServiceON(ServiceReference serviceReference) {
        if (writable) {
            return ObjectNameUtil.createTransactionServiceON(serviceReferenceRegistrator.getNullableTransactionName(),
                    serviceReference.getServiceInterfaceName(), serviceReference.getRefName());
        } else {
            return ObjectNameUtil.createReadOnlyServiceON(serviceReference.getServiceInterfaceName(), serviceReference.getRefName());
        }
    }

    @Override
    public synchronized void removeServiceReference(String serviceInterfaceName, String refName) throws InstanceNotFoundException{
        ServiceReference serviceReference = new ServiceReference(serviceInterfaceName, refName);
        removeServiceReference(serviceReference);
    }

    private synchronized void removeServiceReference(ServiceReference serviceReference) throws InstanceNotFoundException {
        logger.debug("Removing service reference {} from {}", serviceReference, this);
        assertWritable();
        // is the qName known?
        if (allQNames.contains(serviceReference.getServiceInterfaceName()) == false) {
            logger.error("Cannot find qname {} in {}", serviceReference.getServiceInterfaceName(), allQNames);
            throw new IllegalArgumentException("Cannot find service interface " + serviceReference.getServiceInterfaceName());
        }
        ModuleIdentifier removed = refNames.remove(serviceReference);
        if (removed == null){
            throw new InstanceNotFoundException("Cannot find " + serviceReference.getServiceInterfaceName());
        }
        Entry<ServiceReferenceMXBeanImpl, ServiceReferenceJMXRegistration> entry = mBeans.remove(serviceReference);
        if (entry == null) {
            throw new IllegalStateException("Possible code error: cannot remove from mBeans: " + serviceReference);
        }
        entry.getValue().close();
    }

    @Override
    public synchronized void removeAllServiceReferences() {
        assertWritable();
        for (ServiceReference serviceReference: mBeans.keySet()) {
            try {
                removeServiceReference(serviceReference);
            } catch (InstanceNotFoundException e) {
                throw new IllegalStateException("Possible error in code", e);
            }
        }
    }

    @Override
    public synchronized boolean removeServiceReferences(ObjectName moduleObjectName) throws InstanceNotFoundException {
        assertWritable();
        Set<ServiceReference> serviceReferencesLinkingTo = findServiceReferencesLinkingTo(moduleObjectName);
        for (ServiceReference sr : serviceReferencesLinkingTo) {
            removeServiceReference(sr);
        }
        return serviceReferencesLinkingTo.isEmpty() == false;
    }

    private synchronized Set<ServiceReference> findServiceReferencesLinkingTo(ObjectName moduleObjectName)  throws InstanceNotFoundException {
        lookupRegistry.checkConfigBeanExists(moduleObjectName);
        String factoryName = ObjectNameUtil.getFactoryName(moduleObjectName);
        // check that service interface name exist
        Set<String> serviceInterfaceQNames = factoryNamesToQNames.get(factoryName);
        if (serviceInterfaceQNames == null) {
            logger.error("Possible error in code: cannot find factoryName {} in {}, object name {}", factoryName, factoryNamesToQNames, moduleObjectName);
            throw new IllegalStateException("Possible error in code: cannot find annotations of existing factory " + factoryName);
        }
        String instanceName = ObjectNameUtil.getInstanceName(moduleObjectName);
        ModuleIdentifier moduleIdentifier = new ModuleIdentifier(factoryName, instanceName);
        Set<ServiceReference> result = new HashSet<>();
        for (Entry<ServiceReference, ModuleIdentifier> entry : refNames.entrySet()) {
            if (entry.getValue().equals(moduleIdentifier)) {
                result.add(entry.getKey());
            }
        }
        return result;
    }


    @Override
    public String toString() {
        return "ServiceReferenceRegistryImpl{" +
                "lookupRegistry=" + lookupRegistry +
                "refNames=" + refNames +
                ", factoryNamesToQNames=" + factoryNamesToQNames +
                '}';
    }

    @Override
    public void close() {
        serviceReferenceRegistrator.close();
    }
}
