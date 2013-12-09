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
import org.opendaylight.controller.config.manager.impl.util.InterfacesHelper;
import org.opendaylight.controller.config.spi.ModuleFactory;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.InstanceNotFoundException;
import javax.management.ObjectName;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class ServiceReferenceRegistryImpl implements ServiceReferenceReadableRegistry, ServiceReferenceWritableRegistry {
    private static final Logger logger = LoggerFactory.getLogger(ServiceReferenceRegistryImpl.class);

    private final Map<String, ModuleFactory> factories;
    private final Map<String, Set<String>> factoryNamesToQNames;
    // validator of incoming ObjectNames - throws InstanceNotFoundException if not found either in registry or transaction
    private final LookupRegistry lookupRegistry;
    // helper method for getting QName of SI from namespace + local name
    private final Map<String /* namespace */, Map<String /* local name */, ServiceInterfaceAnnotation>> namespacesToAnnotations;
    // all Service Interface qNames for sanity checking
    private final Set<String /* qName */> allQNames;

    // actual reference database
    private final Map<String /* qName */, Map<String /* refName */, ModuleIdentifier>> refNames;

    /**
     * Static constructor for config registry. Since only transaction can write to this registry, it will
     * return blank state.
     */
    public static ServiceReferenceReadableRegistry createInitialSRLookupRegistry() {
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
                throw new InstanceNotFoundException("Cannot find " + objectName);
            }
        };
        return new ServiceReferenceRegistryImpl(Collections.<String, ModuleFactory>emptyMap(), lookupRegistry,
                Collections.<String /* qName */, Map<String /* refName */, ModuleIdentifier>>emptyMap());
    }

    /**
     * Static constructor for transaction controller. Take current state as seen by config registry, allow writing new data.
     */
    public static ServiceReferenceWritableRegistry createSRWritableRegistry(ServiceReferenceReadableRegistry oldReadableRegistry,
            LookupRegistry lookupRegistry, Map<String, Map.Entry<ModuleFactory, BundleContext>> currentlyRegisteredFactories) {

        ServiceReferenceRegistryImpl old = (ServiceReferenceRegistryImpl) oldReadableRegistry;
        Map<String, ModuleFactory> factories = extractFactoriesMap(currentlyRegisteredFactories);
        return new ServiceReferenceRegistryImpl(factories, lookupRegistry, old.refNames);
    }

    /**
     * Copy back state to config registry after commit.
     */
    public static ServiceReferenceReadableRegistry createSRReadableRegistry(ServiceReferenceWritableRegistry oldWritableRegistry, LookupRegistry lookupRegistry) {
        ServiceReferenceRegistryImpl old = (ServiceReferenceRegistryImpl) oldWritableRegistry;
        // even if factories do change, nothing in the mapping can change between transactions
        return new ServiceReferenceRegistryImpl(old.factories, lookupRegistry, old.refNames);
    }

    private static Map<String, ModuleFactory> extractFactoriesMap(Map<String, Map.Entry<ModuleFactory, BundleContext>> currentlyRegisteredFactories) {
        Map<String, ModuleFactory> result = new HashMap<>();
        for (Entry<String, Entry<ModuleFactory, BundleContext>> entry : currentlyRegisteredFactories.entrySet()) {
            result.put(entry.getKey(), entry.getValue().getKey());
        }
        return result;
    }

    private ServiceReferenceRegistryImpl(Map<String, ModuleFactory> factories, LookupRegistry lookupRegistry,
                                         Map<String /* qName */, Map<String /* refName */, ModuleIdentifier>> refNamesToCopy) {
        this.factories = factories;
        this.lookupRegistry = lookupRegistry;
        Map<String, Set<String /* QName */>> factoryNamesToQNames = new HashMap<>();
        Set<ServiceInterfaceAnnotation> allAnnotations = new HashSet<>();
        Set<String /* qName */> allQNames = new HashSet<>();
        for (Entry<String, ModuleFactory> entry : factories.entrySet()) {
            if (entry.getKey().equals(entry.getValue().getImplementationName()) == false) {
                throw new IllegalArgumentException("Mismatch between supplied and actual name of " + entry);
            }
            Set<ServiceInterfaceAnnotation> siAnnotations = InterfacesHelper.getServiceInterfaceAnnotations(entry.getValue());
            Set<String> qNames = new HashSet<>();
            for (ServiceInterfaceAnnotation sia: siAnnotations) {
                qNames.add(sia.value());
            }
            allAnnotations.addAll(siAnnotations);
            allQNames.addAll(qNames);
            factoryNamesToQNames.put(entry.getKey(), Collections.unmodifiableSet(qNames));
        }
        this.factoryNamesToQNames = Collections.unmodifiableMap(factoryNamesToQNames);
        this.allQNames = Collections.unmodifiableSet(allQNames);
        // fill namespacesToAnnotations
        Map<String /* namespace */, Map<String /* localName */, ServiceInterfaceAnnotation>> namespacesToAnnotations =
                new HashMap<>();
        for (ServiceInterfaceAnnotation sia : allAnnotations) {
            Map<String, ServiceInterfaceAnnotation> ofNamespace = namespacesToAnnotations.get(sia.namespace());
            if (ofNamespace == null) {
                ofNamespace = new HashMap<>();
                namespacesToAnnotations.put(sia.namespace(), ofNamespace);
            }
            if (ofNamespace.containsKey(sia.localName())) {
                throw new IllegalArgumentException("Conflict between local names in " + sia.namespace() + " : " + sia.localName());
            }
            ofNamespace.put(sia.localName(), sia);
        }
        this.namespacesToAnnotations = Collections.unmodifiableMap(namespacesToAnnotations);
        // copy refNames
        Map<String /* qName */, Map<String /* refName */, ModuleIdentifier>> deepCopy = new HashMap<>();
        for (Entry<String, Map<String, ModuleIdentifier>> outerROEntry: refNamesToCopy.entrySet()) {
            Map<String /* refName */, ModuleIdentifier> innerWritableMap = new HashMap<>();
            deepCopy.put(outerROEntry.getKey(), innerWritableMap);
            for (Entry<String, ModuleIdentifier> innerROEntry:  outerROEntry.getValue().entrySet()) {
                innerWritableMap.put(innerROEntry.getKey(), innerROEntry.getValue());
            }
        }
        this.refNames = Collections.unmodifiableMap(deepCopy);
    }


    @Override
    public Set<String> lookupServiceInterfaceNames(ObjectName objectName) throws InstanceNotFoundException {
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
    public String getServiceInterfaceName(String namespace, String localName) {
        Map<String /* localName */, ServiceInterfaceAnnotation> ofNamespace = namespacesToAnnotations.get(namespace);
        if (ofNamespace == null) {
            throw new IllegalArgumentException("Cannot find namespace " + namespace);
        }
        ServiceInterfaceAnnotation sia = ofNamespace.get(localName);
        if (sia == null) {
            throw new IllegalArgumentException("Cannot find local name " + localName + " in namespace " + namespace);
        }
        return sia.value();
    }



    // reading:

    @Override
    public Map<String /* serviceInterfaceName */, Map<String/* refName */, ObjectName>> getServiceMapping() {
        Map<String /* serviceInterfaceName */, Map<String/* refName */, ObjectName>> result = new HashMap<>();
        for (Entry<String /* qName */, Map<String, ModuleIdentifier>> outerEntry: refNames.entrySet()) {
            String qName = outerEntry.getKey();
            Map<String /* refName */, ObjectName> innerMap = new HashMap<>();
            result.put(qName, innerMap);
            for (Entry<String /* refName */, ModuleIdentifier> innerEntry: outerEntry.getValue().entrySet()) {
                ModuleIdentifier moduleIdentifier = innerEntry.getValue();
                ObjectName on;
                on = getObjectName(moduleIdentifier);
                innerMap.put(innerEntry.getKey(), on);
            }
        }
        return result;
    }

    private ObjectName getObjectName(ModuleIdentifier moduleIdentifier) {
        ObjectName on;
        try {
            on = lookupRegistry.lookupConfigBean(moduleIdentifier.getFactoryName(), moduleIdentifier.getInstanceName());
        } catch (InstanceNotFoundException e) {
            throw new IllegalStateException("Cannot find instance " + moduleIdentifier, e);
        }
        return on;
    }

    @Override
    public ObjectName lookupConfigBeanByServiceInterfaceName(String serviceInterfaceName, String refName) {
        Map<String, ModuleIdentifier> innerMap = refNames.get(serviceInterfaceName);
        if (innerMap == null) {
            throw new IllegalArgumentException("Cannot find " + serviceInterfaceName);
        }
        ModuleIdentifier moduleIdentifier = innerMap.get(refName);
        if (moduleIdentifier == null) {
            throw new IllegalArgumentException("Cannot find module based on service reference " + refName);
        }
        return getObjectName(moduleIdentifier);
    }

    @Override
    public  Map<String /* refName */, ObjectName> lookupServiceReferencesByServiceInterfaceName(String serviceInterfaceName) {
        Map<String, ModuleIdentifier> innerMap = refNames.get(serviceInterfaceName);
        if (innerMap == null) {
            throw new IllegalArgumentException("Cannot find " + serviceInterfaceName);
        }
        Map<String /* refName */, ObjectName> result = new HashMap<>();
        for (Entry<String/* refName */, ModuleIdentifier> entry: innerMap.entrySet()) {
            ObjectName on = getObjectName(entry.getValue());
            result.put(entry.getKey(), on);
        }
        return result;
    }

    // writing:

    @Override
    public void saveServiceReference(String serviceInterfaceName, String refName, ObjectName objectName)  throws InstanceNotFoundException {
        // make sure it is found
        lookupRegistry.checkConfigBeanExists(objectName);
        String factoryName = ObjectNameUtil.getFactoryName(objectName);
        // check that service interface name exist
        Set<String> serviceInterfaceQNames = factoryNamesToQNames.get(factoryName);
        if (serviceInterfaceQNames == null) {
            throw new IllegalStateException("Possible error in code: cannot find annotations of existing factory " + factoryName);
        }
        // supplied serviceInterfaceName must exist in this collection
        if (serviceInterfaceQNames.contains(serviceInterfaceName) == false) {
            throw new IllegalArgumentException("Cannot find service interface " + serviceInterfaceName + " within factory " + factoryName );
        }
        String instanceName = ObjectNameUtil.getInstanceName(objectName);
        ModuleIdentifier moduleIdentifier = new ModuleIdentifier(factoryName, instanceName);
        Map<String /* refName */, ModuleIdentifier> ofQName = refNames.get(serviceInterfaceName);
        // might be null
        if (ofQName == null) {
            ofQName = new HashMap<>();
            refNames.put(serviceInterfaceName, ofQName);
        }
        ofQName.put(refName, moduleIdentifier);
    }

    @Override
    public boolean removeServiceReference(String serviceInterfaceName, String refName) {
        // is the qname known?
        if (allQNames.contains(serviceInterfaceName) == false) {
            throw new IllegalArgumentException("Cannot find service interface " + serviceInterfaceName);
        }
        Map<String, ModuleIdentifier> ofQName = refNames.get(serviceInterfaceName);
        if (ofQName == null) {
            return false;
        }
        return ofQName.remove(refName) != null;
    }

    @Override
    public void removeAllServiceReferences() {
        refNames.clear();
    }
}
