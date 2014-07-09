/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.confignetconfconnector.mapping.config;

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


    public ObjectName getByServiceAndRefName(String namespace, String serviceName, String refName) {
        Map<String, Map<String, String>> serviceNameToRefNameToInstance = getMappedServices().get(namespace);

        Preconditions.checkNotNull(serviceNameToRefNameToInstance, "No serviceInstances mapped to " + namespace);

        Map<String, String> refNameToInstance = serviceNameToRefNameToInstance.get(serviceName);
        Preconditions.checkNotNull(refNameToInstance, "No serviceInstances mapped to " + serviceName + " , "
                + serviceNameToRefNameToInstance.keySet());

        String instanceId = refNameToInstance.get(refName);
        Preconditions.checkArgument(instanceId != null, "No serviceInstances mapped to " + serviceName + ":"
                + refName + ", " + serviceNameToRefNameToInstance.keySet());

        Services.ServiceInstance serviceInstance = Services.ServiceInstance.fromString(instanceId);
        Preconditions.checkArgument(serviceInstance != null, "No serviceInstance mapped to " + refName
                + " under service name " + serviceName + " , " + refNameToInstance.keySet());

        String qNameOfService = configServiceRefRegistry.getServiceInterfaceName(namespace, serviceName);
        try {
            /*
             Remove transaction name as this is redundant - will be stripped in DynamicWritableWrapper,
             and makes it hard to compare with service references got from MXBean attributes
            */
            return ObjectNameUtil.withoutTransactionName(
                    configServiceRefRegistry.getServiceReference(qNameOfService, refName));
        } catch (InstanceNotFoundException e) {
            throw new IllegalArgumentException("No serviceInstance mapped to " + refName
                    + " under service name " + serviceName + " , " + refNameToInstance.keySet(), e);

        }
    }

    public Map<String, Map<String, Map<String, String>>> getMappedServices() {
        Map<String, Map<String, Map<String, String>>> retVal = Maps.newHashMap();

        Map<String, Map<String, ObjectName>> serviceMapping = configServiceRefRegistry.getServiceMapping();
        for (String serviceQName : serviceMapping.keySet()){
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

                Preconditions.checkState(!refsToSis.containsKey(refName),
                        "Duplicate reference name %s for service %s:%s, now for instance %s", refName, namespace,
                        localName, on);
                refsToSis.put(refName, si.toString());
            }
        }

        return retVal;
    }
}
