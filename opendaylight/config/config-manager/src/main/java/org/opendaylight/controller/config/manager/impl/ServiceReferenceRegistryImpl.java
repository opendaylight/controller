/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.manager.impl;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.ObjectName;
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

public class ServiceReferenceRegistryImpl implements CloseableServiceReferenceReadableRegistry, SearchableServiceReferenceWritableRegistry {
    private static final Logger LOG = LoggerFactory.getLogger(ServiceReferenceRegistryImpl.class);

    private final Map<String, ModuleFactory> factories;
    private final Map<String, Set<String>> factoryNamesToQNames;
    // validator of incoming ObjectNames - throws InstanceNotFoundException if not found either in registry or transaction
    private final LookupRegistry lookupRegistry;
    private final ServiceReferenceRegistrator serviceReferenceRegistrator;
    // helper method for getting QName of SI from namespace + local name
    private final Map<String /* namespace */, Map<String /* local name */, ServiceInterfaceAnnotation>> namespacesToAnnotations;
    private final Map<String /* service qName */, ServiceInterfaceAnnotation> serviceQNamesToAnnotations;
    // all Service Interface qNames for sanity checking
    private final Set<String /* qName */> allQNames;
    Map<ModuleIdentifier, Map<ServiceInterfaceAnnotation, String /* service ref name */>> modulesToServiceRef = new HashMap<>();


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
            public Set<ObjectName> lookupConfigBeans(final String moduleName) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Set<ObjectName> lookupConfigBeans(final String moduleName, final String instanceName) {
                throw new UnsupportedOperationException();
            }

            @Override
            public ObjectName lookupConfigBean(final String moduleName, final String instanceName) throws InstanceNotFoundException {
                throw new UnsupportedOperationException();
            }

            @Override
            public void checkConfigBeanExists(final ObjectName objectName) throws InstanceNotFoundException {
                throw new InstanceNotFoundException("Cannot find " + objectName + " - Tried to use mocking registry");
            }

            @Override
            public Set<String> getAvailableModuleFactoryQNames() {
                throw new UnsupportedOperationException();
            }

            @Override
            public Set<ObjectName> lookupRuntimeBeans() {
                throw new UnsupportedOperationException();
            }

            @Override
            public Set<ObjectName> lookupRuntimeBeans(final String moduleName, final String instanceName) {
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
                    public ServiceReferenceJMXRegistration registerMBean(final ServiceReferenceMXBeanImpl object, final ObjectName on) throws InstanceAlreadyExistsException {
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
    public static SearchableServiceReferenceWritableRegistry createSRWritableRegistry(final ServiceReferenceReadableRegistry oldReadableRegistry,
                                                    final ConfigTransactionLookupRegistry txLookupRegistry,
                                                    final Map<String, Map.Entry<ModuleFactory, BundleContext>> currentlyRegisteredFactories) {

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
    public static CloseableServiceReferenceReadableRegistry createSRReadableRegistry(final ServiceReferenceWritableRegistry oldWritableRegistry,
                                                                            final LookupRegistry lookupRegistry, final BaseJMXRegistrator baseJMXRegistrator) {
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
    private static void copy(final ServiceReferenceRegistryImpl old, final ServiceReferenceRegistryImpl newRegistry, final String nullableDstTransactionName) {
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
                LOG.error("Cannot save service reference({}, {})", refNameEntry.getKey(), currentImplementation);
                throw new IllegalStateException("Possible code error", e);
            }
        }
    }

    private static Map<String, ModuleFactory> extractFactoriesMap(final Map<String, Map.Entry<ModuleFactory, BundleContext>> currentlyRegisteredFactories) {
        Map<String, ModuleFactory> result = new HashMap<>();
        for (Entry<String, Entry<ModuleFactory, BundleContext>> entry : currentlyRegisteredFactories.entrySet()) {
            result.put(entry.getKey(), entry.getValue().getKey());
        }
        return result;
    }

    private ServiceReferenceRegistryImpl(final Map<String, ModuleFactory> factories, final LookupRegistry lookupRegistry,
                                         final ServiceReferenceTransactionRegistratorFactory serviceReferenceRegistratorFactory,
                                         final boolean writable) {
        this.factories = factories;
        this.writable = writable;
        this.lookupRegistry = lookupRegistry;

        this.serviceReferenceRegistrator = serviceReferenceRegistratorFactory.create();

        Map<String, Set<String /* QName */>> modifiableFactoryNamesToQNames = new HashMap<>();
        Set<ServiceInterfaceAnnotation> allAnnotations = new HashSet<>();
        Set<String /* qName */> allQNameSet = new HashSet<>();


        for (Entry<String, ModuleFactory> entry : factories.entrySet()) {
            if (entry.getKey().equals(entry.getValue().getImplementationName()) == false) {
                LOG.error("Possible error in code: Mismatch between supplied and actual name of {}", entry);
                throw new IllegalArgumentException("Possible error in code: Mismatch between supplied and actual name of " + entry);
            }
            Set<ServiceInterfaceAnnotation> siAnnotations = InterfacesHelper.getServiceInterfaceAnnotations(entry.getValue());
            Set<String> qNames = InterfacesHelper.getQNames(siAnnotations);
            allAnnotations.addAll(siAnnotations);
            allQNameSet.addAll(qNames);
            modifiableFactoryNamesToQNames.put(entry.getKey(), qNames);
        }
        this.factoryNamesToQNames = ImmutableMap.copyOf(modifiableFactoryNamesToQNames);
        this.allQNames = ImmutableSet.copyOf(allQNameSet);
        // fill namespacesToAnnotations
        Map<String /* namespace */, Map<String /* localName */, ServiceInterfaceAnnotation>> modifiableNamespacesToAnnotations =
                new HashMap<>();
        Map<String /* service qName*/, ServiceInterfaceAnnotation> modifiableServiceQNamesToAnnotations = new HashMap<>();
        for (ServiceInterfaceAnnotation sia : allAnnotations) {
            Map<String, ServiceInterfaceAnnotation> ofNamespace = modifiableNamespacesToAnnotations.get(sia.namespace());
            if (ofNamespace == null) {
                ofNamespace = new HashMap<>();
                modifiableNamespacesToAnnotations.put(sia.namespace(), ofNamespace);
            }
            if (ofNamespace.containsKey(sia.localName())) {
                LOG.error("Cannot construct namespacesToAnnotations map, conflict between local names in {}, offending local name: {}, map so far {}",
                        sia.namespace(), sia.localName(), modifiableNamespacesToAnnotations);
                throw new IllegalArgumentException("Conflict between local names in " + sia.namespace() + " : " + sia.localName());
            }
            ofNamespace.put(sia.localName(), sia);
            modifiableServiceQNamesToAnnotations.put(sia.value(), sia);
        }
        this.namespacesToAnnotations = ImmutableMap.copyOf(modifiableNamespacesToAnnotations);
        this.serviceQNamesToAnnotations = ImmutableMap.copyOf(modifiableServiceQNamesToAnnotations);
        LOG.trace("factoryNamesToQNames:{}", this.factoryNamesToQNames);
    }

    @Override
    public Map<ServiceInterfaceAnnotation, String /* service ref name */> findServiceInterfaces(final ModuleIdentifier moduleIdentifier) {
        Map<ServiceInterfaceAnnotation, String /* service ref name */> result = modulesToServiceRef.get(moduleIdentifier);
        if (result == null) {
            return Collections.emptyMap();
        }
        return Collections.unmodifiableMap(result);
    }

    @Override
    public synchronized Set<String> lookupServiceInterfaceNames(final ObjectName objectName) throws InstanceNotFoundException {
        lookupRegistry.checkConfigBeanExists(objectName);

        String factoryName = ObjectNameUtil.getFactoryName(objectName);
        Set<String> serviceInterfaceAnnotations = factoryNamesToQNames.get(factoryName);
        if (serviceInterfaceAnnotations == null) {
            LOG.error("Possible error in code: cannot find factory annotations of '{}' extracted from ON {} in {}",
                    factoryName, objectName, factoryNamesToQNames);
            throw new IllegalArgumentException("Cannot find factory with name " + factoryName);
        }
        return serviceInterfaceAnnotations;
    }

    @Override
    public synchronized String getServiceInterfaceName(final String namespace, final String localName) {
        Map<String /* localName */, ServiceInterfaceAnnotation> ofNamespace = namespacesToAnnotations.get(namespace);
        if (ofNamespace == null) {
            LOG.error("Cannot find namespace {} in {}", namespace, namespacesToAnnotations);
            throw new IllegalArgumentException("Cannot find namespace " + namespace);
        }
        ServiceInterfaceAnnotation sia = ofNamespace.get(localName);
        if (sia == null) {
            LOG.error("Cannot find local name {} in namespace {}, found only {}", localName, namespace, ofNamespace);
            throw new IllegalArgumentException("Cannot find local name " + localName + " in namespace " + namespace);
        }
        return sia.value();
    }

    // reading:

    @Override
    public synchronized Map<String /* serviceInterfaceName */, Map<String/* refName */, ObjectName>> getServiceMapping() {
        Map<String /* serviceInterfaceName */, Map<String/* refName */, ObjectName>> result = new HashMap<>();
        for (Entry<ServiceReference, ModuleIdentifier> entry: refNames.entrySet()) {
            String qName = entry.getKey().getServiceInterfaceQName();
            Map<String /* refName */, ObjectName> innerMap = result.get(qName);
            if (innerMap == null) {
                innerMap = new HashMap<>();
                result.put(qName, innerMap);
            }
            innerMap.put(entry.getKey().getRefName(), getObjectName(entry.getValue()));
        }
        return result;
    }

    private ObjectName getObjectName(final ModuleIdentifier moduleIdentifier) {
        ObjectName on;
        try {
            on = lookupRegistry.lookupConfigBean(moduleIdentifier.getFactoryName(), moduleIdentifier.getInstanceName());
        } catch (InstanceNotFoundException e) {
            LOG.error("Cannot find instance {}", moduleIdentifier);
            throw new IllegalStateException("Cannot find instance " + moduleIdentifier, e);
        }
        return on;
    }

    @Override
    public synchronized ObjectName lookupConfigBeanByServiceInterfaceName(final String serviceInterfaceQName, final String refName) {
        ServiceReference serviceReference = new ServiceReference(serviceInterfaceQName, refName);
        ModuleIdentifier moduleIdentifier = refNames.get(serviceReference);
        if (moduleIdentifier == null) {
            LOG.error("Cannot find qname {} and refName {} in {}", serviceInterfaceQName, refName, refName);
            throw new IllegalArgumentException("Cannot find " + serviceReference);
        }
        return getObjectName(moduleIdentifier);
    }

    @Override
    public synchronized Map<String /* refName */, ObjectName> lookupServiceReferencesByServiceInterfaceName(final String serviceInterfaceQName) {
        Map<String, Map<String, ObjectName>> serviceMapping = getServiceMapping();
        Map<String, ObjectName> innerMap = serviceMapping.get(serviceInterfaceQName);
        if (innerMap == null) {
            LOG.error("Cannot find qname {} in {}", serviceInterfaceQName, refNames);
            throw new IllegalArgumentException("Cannot find " + serviceInterfaceQName);
        }
        return innerMap;
    }

    @Override
    public synchronized ObjectName getServiceReference(final String serviceInterfaceQName, final String refName) throws InstanceNotFoundException {
        ServiceReference serviceReference = new ServiceReference(serviceInterfaceQName, refName);
        if (mBeans.containsKey(serviceReference) == false) {
            throw new InstanceNotFoundException("Cannot find " + serviceReference);
        }
        return getServiceON(serviceReference);
    }

    @Override
    public synchronized void checkServiceReferenceExists(final ObjectName objectName) throws InstanceNotFoundException {
        String actualTransactionName = ObjectNameUtil.getTransactionName(objectName);
        String expectedTransactionName = serviceReferenceRegistrator.getNullableTransactionName();
        if (writable & actualTransactionName == null || (writable && actualTransactionName.equals(expectedTransactionName) == false)) {
            throw new IllegalArgumentException("Mismatched transaction name in " + objectName);
        }
        String serviceQName = ObjectNameUtil.getServiceQName(objectName);
        String referenceName = ObjectNameUtil.getReferenceName(objectName);
        ServiceReference serviceReference = new ServiceReference(serviceQName, referenceName);
        if (refNames.containsKey(serviceReference) == false) {
            LOG.warn("Cannot find {} in {}", serviceReference, refNames);
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
    public synchronized ObjectName saveServiceReference(final String serviceInterfaceName, final String refName, final ObjectName moduleON)  throws InstanceNotFoundException {
        assertWritable();
        ServiceReference serviceReference = new ServiceReference(serviceInterfaceName, refName);
        return saveServiceReference(serviceReference, moduleON);
    }

    private synchronized ObjectName saveServiceReference(final ServiceReference serviceReference, final ObjectName moduleON)
            throws InstanceNotFoundException{
        return saveServiceReference(serviceReference, moduleON, false);
    }

    private synchronized ObjectName saveServiceReference(final ServiceReference serviceReference, final ObjectName moduleON,
                                                         final boolean skipChecks) throws InstanceNotFoundException {

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
            LOG.error("Possible error in code: cannot find factoryName {} in {}, {}", moduleIdentifier.getFactoryName(),
                    factoryNamesToQNames, moduleIdentifier);
            throw new IllegalStateException("Possible error in code: cannot find annotations of existing factory " + moduleIdentifier.getFactoryName());
        }
        // supplied serviceInterfaceName must exist in this collection
        if (serviceInterfaceQNames.contains(serviceReference.getServiceInterfaceQName()) == false) {
            LOG.error("Cannot find qName {} with factory name {}, found {}", serviceReference.getServiceInterfaceQName(), moduleIdentifier.getFactoryName(), serviceInterfaceQNames);
            throw new IllegalArgumentException("Cannot find service interface " + serviceReference.getServiceInterfaceQName() + " within factory " + moduleIdentifier.getFactoryName());
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
        Map<ServiceInterfaceAnnotation, String /* service ref name */> refNamesToAnnotations = modulesToServiceRef.get(moduleIdentifier);
        if (refNamesToAnnotations == null){
            refNamesToAnnotations = new HashMap<>();
            modulesToServiceRef.put(moduleIdentifier, refNamesToAnnotations);
        }

        ServiceInterfaceAnnotation annotation = serviceQNamesToAnnotations.get(serviceReference.getServiceInterfaceQName());
        checkNotNull(annotation, "Possible error in code, cannot find annotation for " + serviceReference);
        refNamesToAnnotations.put(annotation, serviceReference.getRefName());
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
            public ServiceReferenceJMXRegistration setValue(final ServiceReferenceJMXRegistration value) {
                throw new UnsupportedOperationException();
            }
        };
    }

    private ObjectName getServiceON(final ServiceReference serviceReference) {
        if (writable) {
            return ObjectNameUtil.createTransactionServiceON(serviceReferenceRegistrator.getNullableTransactionName(),
                    serviceReference.getServiceInterfaceQName(), serviceReference.getRefName());
        } else {
            return ObjectNameUtil.createReadOnlyServiceON(serviceReference.getServiceInterfaceQName(), serviceReference.getRefName());
        }
    }

    @Override
    public synchronized void removeServiceReference(final String serviceInterfaceName, final String refName) throws InstanceNotFoundException{
        ServiceReference serviceReference = new ServiceReference(serviceInterfaceName, refName);
        removeServiceReference(serviceReference);
    }

    private synchronized void removeServiceReference(final ServiceReference serviceReference) throws InstanceNotFoundException {
        LOG.debug("Removing service reference {} from {}", serviceReference, this);
        assertWritable();
        // is the qName known?
        if (allQNames.contains(serviceReference.getServiceInterfaceQName()) == false) {
            LOG.error("Cannot find qname {} in {}", serviceReference.getServiceInterfaceQName(), allQNames);
            throw new IllegalArgumentException("Cannot find service interface " + serviceReference.getServiceInterfaceQName());
        }
        ModuleIdentifier removed = refNames.remove(serviceReference);
        if (removed == null){
            throw new InstanceNotFoundException("Cannot find " + serviceReference.getServiceInterfaceQName());
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
    public synchronized boolean removeServiceReferences(final ObjectName moduleObjectName) throws InstanceNotFoundException {
        lookupRegistry.checkConfigBeanExists(moduleObjectName);
        String factoryName = ObjectNameUtil.getFactoryName(moduleObjectName);
        // check that service interface name exist
        Set<String> serviceInterfaceQNames = factoryNamesToQNames.get(factoryName);
        return removeServiceReferences(moduleObjectName, serviceInterfaceQNames);
    }


    private boolean removeServiceReferences(final ObjectName moduleObjectName, final Set<String> qNames) throws InstanceNotFoundException {
        ObjectNameUtil.checkType(moduleObjectName, ObjectNameUtil.TYPE_MODULE);
        assertWritable();
        Set<ServiceReference> serviceReferencesLinkingTo = findServiceReferencesLinkingTo(moduleObjectName, qNames);
        for (ServiceReference sr : serviceReferencesLinkingTo) {
            removeServiceReference(sr);
        }
        return serviceReferencesLinkingTo.isEmpty() == false;
    }

    private Set<ServiceReference> findServiceReferencesLinkingTo(final ObjectName moduleObjectName, final Set<String> serviceInterfaceQNames) {
        String factoryName = ObjectNameUtil.getFactoryName(moduleObjectName);
        if (serviceInterfaceQNames == null) {
            LOG.warn("Possible error in code: cannot find factoryName {} in {}, object name {}", factoryName, factoryNamesToQNames, moduleObjectName);
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
