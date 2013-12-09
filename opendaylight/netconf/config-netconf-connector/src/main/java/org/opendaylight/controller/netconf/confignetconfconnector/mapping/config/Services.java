/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.confignetconfconnector.mapping.config;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import org.opendaylight.controller.config.api.ServiceReferenceReadableRegistry;
import org.opendaylight.controller.config.api.jmx.ObjectNameUtil;
import org.opendaylight.controller.netconf.confignetconfconnector.mapping.attributes.fromxml.ObjectNameAttributeReadingStrategy;
import org.opendaylight.controller.netconf.util.xml.XmlElement;
import org.opendaylight.controller.netconf.util.xml.XmlNetconfConstants;
import org.opendaylight.controller.netconf.util.xml.XmlUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.management.ObjectName;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Services {

    private static final Logger logger = LoggerFactory.getLogger(Services.class);

    private static final String PROVIDER_KEY = "provider";
    private static final String NAME_KEY = "name";
    public static final String TYPE_KEY = "type";
    public static final String SERVICE_KEY = "service";

    private long suffix = 1;

    private final Map<String /*Namespace*/, Map<String/* ServiceName */, Map<String/* refName */, ServiceInstance>>> namespaceToServiceNameToRefNameToInstance = Maps
            .newHashMap();
    private ServiceReferenceReadableRegistry configServiceRefRegistry;

    public Services(ServiceReferenceReadableRegistry configServiceRefRegistry) {
        this.configServiceRefRegistry = configServiceRefRegistry;
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

    public ServiceInstance getByServiceAndRefName(String namespace, String serviceName, String refName) {
        Map<String, Map<String, String>> serviceNameToRefNameToInstance = getMappedServices().get(namespace);

        Preconditions.checkArgument(serviceNameToRefNameToInstance != null, "No serviceInstances mapped to " + namespace);

        Map<String, String> refNameToInstance = serviceNameToRefNameToInstance.get(serviceName);
        Preconditions.checkArgument(refNameToInstance != null, "No serviceInstances mapped to " + serviceName + " , "
                + serviceNameToRefNameToInstance.keySet());

        ServiceInstance serviceInstance = ServiceInstance.fromString(refNameToInstance.get(refName));
        Preconditions.checkArgument(serviceInstance != null, "No serviceInstance mapped to " + refName
                + " under service name " + serviceName + " , " + refNameToInstance.keySet());
        return serviceInstance;
    }

    // TODO hide getMappedServices, call it explicitly in toXml

    public Map<String, Map<String, Map<String, String>>> getMappedServices() {
        Map<String, Map<String, Map<String, String>>> retVal = Maps.newHashMap();

        for (String namespace : namespaceToServiceNameToRefNameToInstance.keySet()) {

            Map<String, Map<String, ServiceInstance>> serviceNameToRefNameToInstance = namespaceToServiceNameToRefNameToInstance
                    .get(namespace);
            Map<String, Map<String, String>> innerRetVal = Maps.newHashMap();

            for (String serviceName : serviceNameToRefNameToInstance.keySet()) {

                Map<String, String> innerInnerRetVal = Maps.newHashMap();
                for (Entry<String, ServiceInstance> refNameToSi : serviceNameToRefNameToInstance.get(serviceName).entrySet()) {
                    innerInnerRetVal.put(refNameToSi.getKey(), refNameToSi.getValue().toString());
                }
                innerRetVal.put(serviceName, innerInnerRetVal);
            }
            retVal.put(namespace, innerRetVal);
        }

        Map<String, Map<String, ObjectName>> serviceMapping = configServiceRefRegistry.getServiceMapping();
        for (String serviceQName : serviceMapping.keySet())
            for (String refName : serviceMapping.get(serviceQName).keySet()) {

                ObjectName on = serviceMapping.get(serviceQName).get(refName);
                ServiceInstance si = ServiceInstance.fromObjectName(on);

                // FIXME use QName's new String constructor, after its implemented
                Pattern p = Pattern.compile("\\(([^\\(\\?]+)\\?[^\\?\\)]*\\)([^\\)]+)");
                Matcher matcher = p.matcher(serviceQName);
                Preconditions.checkArgument(matcher.matches());
                String namespace = matcher.group(1);
                String localName = matcher.group(2);

                Map<String, Map<String, String>> serviceToRefs = retVal.get(namespace);
                if(serviceToRefs==null) {
                    serviceToRefs = Maps.newHashMap();
                    retVal.put(namespace, serviceToRefs);
                }

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

    /**
     *
     */
    public Map<String, Map<String, Map<String, ServiceInstance>>> getNamespaceToServiceNameToRefNameToInstance() {
        return namespaceToServiceNameToRefNameToInstance;
    }

    // TODO hide resolveServices, call it explicitly in fromXml

    public static Services resolveServices(Map<String, Map<String, Map<String, String>>> mappedServices, ServiceReferenceReadableRegistry taClient) {
        Services tracker = new Services(taClient);

        for (Entry<String, Map<String, Map<String, String>>> namespaceEntry : mappedServices.entrySet()) {
            String namespace = namespaceEntry.getKey();

            for (Entry<String, Map<String, String>> serviceEntry : namespaceEntry.getValue().entrySet()) {

                String serviceName = serviceEntry.getKey();
                for (Entry<String, String> refEntry : serviceEntry.getValue().entrySet()) {

                    Map<String, Map<String, ServiceInstance>> namespaceToServices = tracker.namespaceToServiceNameToRefNameToInstance.get(namespace);
                    if (namespaceToServices == null) {
                        namespaceToServices = Maps.newHashMap();
                        tracker.namespaceToServiceNameToRefNameToInstance.put(namespace, namespaceToServices);
                    }

                    Map<String, ServiceInstance> refNameToInstance = namespaceToServices
                            .get(serviceName);
                    if (refNameToInstance == null) {
                        refNameToInstance = Maps.newHashMap();
                        namespaceToServices.put(serviceName, refNameToInstance);
                    }

                    String refName = refEntry.getKey();

                    ServiceInstance serviceInstance = ServiceInstance.fromString(refEntry.getValue());
                    refNameToInstance.put(refName, serviceInstance);

                }
            }
        }
        return tracker;
    }

    // TODO support edit strategies on services

    public static Map<String, Map<String, Map<String, String>>> fromXml(XmlElement xml) {
        Map<String, Map<String, Map<String, String>>> retVal = Maps.newHashMap();

        List<XmlElement> services = xml.getChildElements(SERVICE_KEY);
        xml.checkUnrecognisedElements(services);

        for (XmlElement service : services) {

            XmlElement typeElement = service.getOnlyChildElement(TYPE_KEY);
            Entry<String, String> prefixNamespace = typeElement.findNamespaceOfTextContent();

            Preconditions.checkState(prefixNamespace.getKey()!=null && prefixNamespace.getKey().equals("") == false, "Type attribute was not prefixed");

            Map<String, Map<String, String>> namespaceToServices = retVal.get(prefixNamespace.getValue());
            if(namespaceToServices == null) {
                namespaceToServices = Maps.newHashMap();
                retVal.put(prefixNamespace.getValue(), namespaceToServices);
            }

            String serviceName =  ObjectNameAttributeReadingStrategy.checkPrefixAndExtractServiceName(typeElement, prefixNamespace);

            Map<String, String> innerMap = Maps.newHashMap();
            namespaceToServices.put(serviceName, innerMap);

            List<XmlElement> instances = service.getChildElements(XmlNetconfConstants.INSTANCE_KEY);
            service.checkUnrecognisedElements(instances, typeElement);

            for (XmlElement instance : instances) {
                XmlElement nameElement = instance.getOnlyChildElement(NAME_KEY);
                String refName = nameElement.getTextContent();

                XmlElement providerElement = instance.getOnlyChildElement(PROVIDER_KEY);
                String providerName = providerElement.getTextContent();

                instance.checkUnrecognisedElements(nameElement, providerElement);

                innerMap.put(refName, providerName);
            }
        }

        return retVal;
    }

    private String findAvailableRefName(String refName, Set<String> refNamesAsSet) {
        String intitialRefName = refName;

        while (true) {
            refName = intitialRefName + "_" + suffix++;
            if (refNamesAsSet.contains(refName) == false)
                return refName;
        }
    }

    public Element toXml(Map<String, Map<String, Map<String, String>>> mappedServices, Document document) {
        Element root = document.createElement(XmlNetconfConstants.SERVICES_KEY);
        XmlUtil.addNamespaceAttr(root, XmlNetconfConstants.URN_OPENDAYLIGHT_PARAMS_XML_NS_YANG_CONTROLLER_CONFIG);

        for (String namespace : mappedServices.keySet()) {

            for (Entry<String, Map<String, String>> serviceEntry : mappedServices.get(namespace).entrySet()) {
                Element serviceElement = document.createElement(SERVICE_KEY);
                root.appendChild(serviceElement);

                Element typeElement = XmlUtil.createPrefixedTextElement(document, TYPE_KEY, XmlNetconfConstants.PREFIX,
                        serviceEntry.getKey());
                XmlUtil.addPrefixedNamespaceAttr(typeElement, XmlNetconfConstants.PREFIX, namespace);
                serviceElement.appendChild(typeElement);

                for (Entry<String, String> instanceEntry : serviceEntry.getValue().entrySet()) {
                    Element instanceElement = document.createElement(XmlNetconfConstants.INSTANCE_KEY);
                    serviceElement.appendChild(instanceElement);

                    Element nameElement = XmlUtil.createTextElement(document, NAME_KEY, instanceEntry.getKey());
                    instanceElement.appendChild(nameElement);

                    Element providerElement = XmlUtil.createTextElement(document, PROVIDER_KEY, instanceEntry.getValue());
                    instanceElement.appendChild(providerElement);
                }
            }

        }
        return root;
    }

    public String getRefName(String namespace, String serviceName, ObjectName on, Optional<String> expectedRefName) {
        Optional<String> refNameOptional = getRefNameOptional(namespace, serviceName, on, expectedRefName);
        Preconditions.checkState(refNameOptional.isPresent(), "No reference names mapped to %s, %s, %s", namespace,
                serviceName, on);
        return refNameOptional.get();
    }

    public Optional<String> getRefNameOptional(String namespace, String serviceName, ObjectName on,
            Optional<String> expectedRefName) {
        Map<String, Map<String, String>> services = getMappedServices().get(namespace);

        if(services == null) return Optional.absent();
        Map<String, String> refs = services.get(serviceName);

        if(refs == null) return Optional.absent();
        Multimap<ServiceInstance, String> reverted = revertMap(refs);

        ServiceInstance serviceInstance = ServiceInstance.fromObjectName(on);
        Collection<String> references = reverted.get(serviceInstance);

        if (expectedRefName.isPresent() && references.contains(expectedRefName.get())) {
            logger.debug("Returning expected ref name {} for {}", expectedRefName.get(), on);
            return expectedRefName;
        } else if (references.size() > 0) {
            String next = references.iterator().next();
            logger.debug("Returning random ref name {} for {}", next, on);
            return Optional.of(next);
        } else
            return Optional.absent();
    }

    private Multimap<ServiceInstance, String> revertMap(Map<String, String> refs) {
        Multimap<ServiceInstance, String> multimap = HashMultimap.create();

        for (Entry<String, String> e : refs.entrySet()) {
            multimap.put(ServiceInstance.fromString(e.getValue()), e.getKey());
        }

        return multimap;
    }

    public boolean hasRefName(String key, String value, ObjectName on) {
        return getRefNameOptional(key, value, on, Optional.<String>absent()).isPresent();
    }

    public static final class ServiceInstance {
        public ServiceInstance(String moduleName, String instanceName) {
            this.moduleName = moduleName;
            this.instanceName = instanceName;
        }

        public static ServiceInstance fromString(String instanceId) {
            instanceId = instanceId.trim();
            Matcher matcher = p.matcher(instanceId);
            if(matcher.matches() == false) {
                matcher = pDeprecated.matcher(instanceId);
            }

            Preconditions.checkArgument(matcher.matches(), "Unexpected format for provider, expected " + p.toString()
                    + " or " + pDeprecated.toString() + " but was " + instanceId);

            String factoryName = matcher.group(1);
            String instanceName = matcher.group(2);
            return new ServiceInstance(factoryName, instanceName);
        }

        private final String moduleName, instanceName;
        private String serviceName;

        public String getServiceName() {
            return serviceName;
        }

        public void setServiceName(String serviceName) {
            this.serviceName = serviceName;
        }

        public String getModuleName() {
            return moduleName;
        }

        public String getInstanceName() {
            return instanceName;
        }

        private static final String blueprint = "/"
                + XmlNetconfConstants.MODULES_KEY + "/" + XmlNetconfConstants.MODULE_KEY + "["
                + XmlNetconfConstants.TYPE_KEY + "='%s']["
                + XmlNetconfConstants.NAME_KEY + "='%s']";

        // TODO unify with xpath in RuntimeRpc

        // Previous version of xpath, needs to be supported for backwards compatibility (persisted configs by config-persister)
        private static final String blueprintRDeprecated = "/" + XmlNetconfConstants.CONFIG_KEY + "/"
                + XmlNetconfConstants.MODULES_KEY + "/" + XmlNetconfConstants.MODULE_KEY + "\\["
                + XmlNetconfConstants.NAME_KEY + "='%s'\\]/" + XmlNetconfConstants.INSTANCE_KEY + "\\["
                + XmlNetconfConstants.NAME_KEY + "='%s'\\]";

        private static final String blueprintR = "/"
                + XmlNetconfConstants.MODULES_KEY + "/" + XmlNetconfConstants.MODULE_KEY + "\\["
                + XmlNetconfConstants.TYPE_KEY + "='%s'\\]\\["
                + XmlNetconfConstants.NAME_KEY + "='%s'\\]";

        private static final Pattern pDeprecated = Pattern.compile(String.format(blueprintRDeprecated, "(.+)", "(.+)"));
        private static final Pattern p = Pattern.compile(String.format(blueprintR, "(.+)", "(.+)"));

        @Override
        public String toString() {
            return String.format(blueprint, moduleName, instanceName);
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((instanceName == null) ? 0 : instanceName.hashCode());
            result = prime * result + ((moduleName == null) ? 0 : moduleName.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            ServiceInstance other = (ServiceInstance) obj;
            if (instanceName == null) {
                if (other.instanceName != null)
                    return false;
            } else if (!instanceName.equals(other.instanceName))
                return false;
            if (moduleName == null) {
                if (other.moduleName != null)
                    return false;
            } else if (!moduleName.equals(other.moduleName))
                return false;
            return true;
        }

        public ObjectName getObjectName(String transactionName) {
            return ObjectNameUtil.createTransactionModuleON(transactionName, moduleName, instanceName);
        }

        public static ServiceInstance fromObjectName(ObjectName on) {
            return new ServiceInstance(ObjectNameUtil.getFactoryName(on), ObjectNameUtil.getInstanceName(on));
        }
    }

}
