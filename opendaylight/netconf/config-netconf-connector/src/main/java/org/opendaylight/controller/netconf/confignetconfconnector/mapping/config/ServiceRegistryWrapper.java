/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.confignetconfconnector.mapping.config;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.opendaylight.controller.config.api.ServiceReferenceReadableRegistry;
import org.opendaylight.yangtools.yang.common.QName;

import javax.management.InstanceNotFoundException;
import javax.management.ObjectName;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

public class ServiceRegistryWrapper {

    private ServiceReferenceReadableRegistry configServiceRefRegistry;

    private long suffix = 1;

    public ServiceRegistryWrapper(ServiceReferenceReadableRegistry configServiceRefRegistry) {
        this.configServiceRefRegistry = configServiceRefRegistry;
    }


    public boolean hasRefName(String namespace, String serviceName, ObjectName on) {
        String qname = configServiceRefRegistry.getServiceInterfaceName(namespace, serviceName);
        Map<String, ObjectName> forQName = configServiceRefRegistry.getServiceMapping().get(qname);
        if(forQName==null) return false;
        return forQName.values().contains(on);
    }

    public ObjectName getByServiceAndRefName(String namespace, String serviceName, String refName) {
        Map<String, Map<String, String>> serviceNameToRefNameToInstance = getMappedServices().get(namespace);

        Preconditions.checkArgument(serviceNameToRefNameToInstance != null, "No serviceInstances mapped to " + namespace);

        Map<String, String> refNameToInstance = serviceNameToRefNameToInstance.get(serviceName);
        Preconditions.checkArgument(refNameToInstance != null, "No serviceInstances mapped to " + serviceName + " , "
                + serviceNameToRefNameToInstance.keySet());

        String instanceId = refNameToInstance.get(refName);
        Preconditions.checkArgument(instanceId != null, "No serviceInstances mapped to " + serviceName + ":"
                + refName + ", " + serviceNameToRefNameToInstance.keySet());

        Services.ServiceInstance serviceInstance = Services.ServiceInstance.fromString(instanceId);
        Preconditions.checkArgument(serviceInstance != null, "No serviceInstance mapped to " + refName
                + " under service name " + serviceName + " , " + refNameToInstance.keySet());

        String qNameOfService = configServiceRefRegistry.getServiceInterfaceName(namespace, serviceName);
        try {
            return configServiceRefRegistry.getServiceReference(qNameOfService, refName);
        } catch (InstanceNotFoundException e) {
            throw new IllegalArgumentException("No serviceInstance mapped to " + refName
                    + " under service name " + serviceName + " , " + refNameToInstance.keySet(), e);

        }
    }

    public Map<String, Map<String, Map<String, String>>> getMappedServices() {
        Map<String, Map<String, Map<String, String>>> retVal = Maps.newHashMap();

        Map<String, Map<String, ObjectName>> serviceMapping = configServiceRefRegistry.getServiceMapping();
        for (String serviceQName : serviceMapping.keySet())
            for (String refName : serviceMapping.get(serviceQName).keySet()) {

                ObjectName on = serviceMapping.get(serviceQName).get(refName);
                Services.ServiceInstance si = Services.ServiceInstance.fromObjectName(on);

                QName qname = QName.create(serviceQName);
                String namespace = qname.getNamespace().toString();
                Map<String, Map<String, String>> serviceToRefs = retVal.get(namespace);
                if(serviceToRefs==null) {
                    serviceToRefs = Maps.newHashMap();
                    retVal.put(namespace, serviceToRefs);
                }

                String localName = qname.getLocalName();
                Map<String, String> refsToSis = serviceToRefs.get(localName);
                if(refsToSis==null) {
                    refsToSis = Maps.newHashMap();
                    serviceToRefs.put(localName, refsToSis);
                }

                Preconditions.checkState(refsToSis.containsKey(refName) == false,
                        "Duplicate reference name %s for service %s:%s, now for instance %s", refName, namespace,
                        localName, on);
                refsToSis.put(refName, si.toString());
            }

        return retVal;
    }

    @VisibleForTesting
    public String getNewDefaultRefName(String namespace, String serviceName, String moduleName, String instanceName) {
        String refName;
        refName = "ref_" + instanceName;

        Map<String, Map<String, String>> serviceNameToRefNameToInstance = getMappedServices().get(namespace);

        Map<String, String> refNameToInstance;
        if(serviceNameToRefNameToInstance == null || serviceNameToRefNameToInstance.containsKey(serviceName) == false) {
            refNameToInstance = Collections.emptyMap();
        } else
            refNameToInstance = serviceNameToRefNameToInstance.get(serviceName);

        final Set<String> refNamesAsSet = toSet(refNameToInstance.keySet());
        if (refNamesAsSet.contains(refName)) {
            refName = findAvailableRefName(refName, refNamesAsSet);
        }

        return refName;
    }


    private Set<String> toSet(Collection<String> values) {
        Set<String> refNamesAsSet = Sets.newHashSet();

        for (String refName : values) {
            boolean resultAdd = refNamesAsSet.add(refName);
            Preconditions.checkState(resultAdd,
                    "Error occurred building services element, reference name {} was present twice", refName);
        }

        return refNamesAsSet;
    }

    private String findAvailableRefName(String refName, Set<String> refNamesAsSet) {
        String intitialRefName = refName;

        while (true) {
            refName = intitialRefName + "_" + suffix++;
            if (refNamesAsSet.contains(refName) == false)
                return refName;
        }
    }
}
