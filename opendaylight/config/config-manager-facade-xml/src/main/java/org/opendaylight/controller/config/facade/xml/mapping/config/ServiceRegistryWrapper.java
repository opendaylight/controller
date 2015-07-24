/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.facade.xml.mapping.config;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import java.util.Map;
import javax.management.InstanceNotFoundException;
import javax.management.ObjectName;
import org.opendaylight.controller.config.api.ServiceReferenceReadableRegistry;
import org.opendaylight.controller.config.api.jmx.ObjectNameUtil;
import org.opendaylight.yangtools.yang.common.QName;

public class ServiceRegistryWrapper {

    private final ServiceReferenceReadableRegistry configServiceRefRegistry;

    public ServiceRegistryWrapper(ServiceReferenceReadableRegistry configServiceRefRegistry) {
        this.configServiceRefRegistry = configServiceRefRegistry;
    }

    public ObjectName getByServiceAndRefName(String namespace, String serviceType, String refName) {
        Map<String, Map<String, Map<String, String>>> mappedServices = getMappedServices();
        Map<String, Map<String, String>> serviceNameToRefNameToInstance = mappedServices.get(namespace);

        Preconditions.checkArgument(serviceNameToRefNameToInstance != null,
                "No service mapped to %s:%s:%s. Wrong namespace, available namespaces: %s",
                namespace, serviceType, refName, mappedServices.keySet());

        Map<String, String> refNameToInstance = serviceNameToRefNameToInstance.get(serviceType);
        Preconditions.checkArgument(refNameToInstance != null,
                "No service mapped to %s:%s:%s. Wrong service type, available service types: %s"
                , namespace, serviceType, refName, serviceNameToRefNameToInstance.keySet());

        String instanceId = refNameToInstance.get(refName);
        Preconditions.checkArgument(instanceId != null,
                "No service mapped to %s:%s:%s. Wrong ref name, available ref names: %s"
                ,namespace, serviceType, refName, refNameToInstance.keySet());

        Services.ServiceInstance serviceInstance = Services.ServiceInstance.fromString(instanceId);
        Preconditions.checkArgument(serviceInstance != null,
                "No service mapped to %s:%s:%s. Wrong ref name, available ref names: %s"
                ,namespace, serviceType, refName, refNameToInstance.keySet());

        String qNameOfService = configServiceRefRegistry.getServiceInterfaceName(namespace, serviceType);
        try {
            /*
             Remove transaction name as this is redundant - will be stripped in DynamicWritableWrapper,
             and makes it hard to compare with service references got from MXBean attributes
            */
            return ObjectNameUtil.withoutTransactionName(
                    configServiceRefRegistry.getServiceReference(qNameOfService, refName));
        } catch (InstanceNotFoundException e) {
            throw new IllegalArgumentException("No serviceInstance mapped to " + refName
                    + " under service name " + serviceType + " , " + refNameToInstance.keySet(), e);

        }
    }

    public Map<String, Map<String, Map<String, String>>> getMappedServices() {
        Map<String, Map<String, Map<String, String>>> retVal = Maps.newHashMap();

        Map<String, Map<String, ObjectName>> serviceMapping = configServiceRefRegistry.getServiceMapping();
        for (Map.Entry<String, Map<String, ObjectName>> qNameToRefNameEntry : serviceMapping.entrySet()){
            for (String refName : qNameToRefNameEntry.getValue().keySet()) {

                ObjectName on = qNameToRefNameEntry.getValue().get(refName);
                Services.ServiceInstance si = Services.ServiceInstance.fromObjectName(on);

                QName qname = QName.create(qNameToRefNameEntry.getKey());
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

                Preconditions.checkState(!refsToSis.containsKey(refName),
                        "Duplicate reference name %s for service %s:%s, now for instance %s", refName, namespace,
                        localName, on);
                refsToSis.put(refName, si.toString());
            }
        }

        return retVal;
    }
}
