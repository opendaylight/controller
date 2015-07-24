/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.facade.xml.mapping.config;


import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.management.ObjectName;
import org.opendaylight.controller.config.api.jmx.ObjectNameUtil;
import org.opendaylight.controller.config.facade.xml.mapping.attributes.fromxml.ObjectNameAttributeReadingStrategy;
import org.opendaylight.controller.config.util.xml.DocumentedException;
import org.opendaylight.controller.config.util.xml.XmlElement;
import org.opendaylight.controller.config.util.xml.XmlMappingConstants;
import org.opendaylight.controller.config.util.xml.XmlUtil;
import org.opendaylight.yangtools.yang.data.api.ModifyAction;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public final class Services {

    private static final String EMPTY_PROVIDER = "";
    private static final String PROVIDER_KEY = "provider";
    private static final String NAME_KEY = "name";
    public static final String TYPE_KEY = "type";
    public static final String SERVICE_KEY = "service";

    private final Map<String /*Namespace*/, Map<String/* ServiceName */, Map<String/* refName */, ServiceInstance>>> namespaceToServiceNameToRefNameToInstance = Maps
            .newHashMap();

    /**
     *
     */
    public Map<String, Map<String, Map<String, ServiceInstance>>> getNamespaceToServiceNameToRefNameToInstance() {
        return namespaceToServiceNameToRefNameToInstance;
    }

    private static Services resolveServices(Map<String, Map<String, Map<String, String>>> mappedServices) {
        Services tracker = new Services();

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
                    //we want to compare reference not value of the provider
                    refNameToInstance.put(refName, refEntry.getValue() == EMPTY_PROVIDER
                            //provider name cannot be EMPTY_PROVIDER instance unless we are executing delete
                            ? ServiceInstance.EMPTY_SERVICE_INSTANCE
                            : ServiceInstance.fromString(refEntry.getValue()));

                }
            }
        }
        return tracker;
    }

    // TODO support edit strategies on services

    public static Services fromXml(XmlElement xml) throws DocumentedException {
        Map<String, Map<String, Map<String, String>>> retVal = Maps.newHashMap();

        List<XmlElement> services = xml.getChildElements(SERVICE_KEY);
        xml.checkUnrecognisedElements(services);

        for (XmlElement service : services) {

            XmlElement typeElement = service.getOnlyChildElement(TYPE_KEY);
            Entry<String, String> prefixNamespace = typeElement.findNamespaceOfTextContent();

            Preconditions.checkState(prefixNamespace.getKey()!=null && !prefixNamespace.getKey().equals(""), "Type attribute was not prefixed");

            Map<String, Map<String, String>> namespaceToServices = retVal.get(prefixNamespace.getValue());
            if(namespaceToServices == null) {
                namespaceToServices = Maps.newHashMap();
                retVal.put(prefixNamespace.getValue(), namespaceToServices);
            }

            String serviceName =  ObjectNameAttributeReadingStrategy
                .checkPrefixAndExtractServiceName(typeElement, prefixNamespace);

            Map<String, String> innerMap = namespaceToServices.get(serviceName);
            if (innerMap == null) {
                innerMap = Maps.newHashMap();
                namespaceToServices.put(serviceName, innerMap);
            }

            List<XmlElement> instances = service.getChildElements(XmlMappingConstants.INSTANCE_KEY);
            service.checkUnrecognisedElements(instances, typeElement);

            for (XmlElement instance : instances) {
                XmlElement nameElement = instance.getOnlyChildElement(NAME_KEY);
                String refName = nameElement.getTextContent();

                if (!ModifyAction.DELETE.toString().toLowerCase().equals(
                        instance.getAttribute(
                                XmlMappingConstants.OPERATION_ATTR_KEY,
                                XmlMappingConstants.URN_IETF_PARAMS_XML_NS_NETCONF_BASE_1_0)))
                {
                    XmlElement providerElement = instance.getOnlyChildElement(PROVIDER_KEY);
                    String providerName = providerElement.getTextContent();

                    instance.checkUnrecognisedElements(nameElement, providerElement);

                    innerMap.put(refName, providerName);
                } else {
                    //since this is a delete we dont have a provider name - we want empty service instance
                    innerMap.put(refName, EMPTY_PROVIDER);
                }
            }
        }

        return resolveServices(retVal);
    }

    public static Element toXml(ServiceRegistryWrapper serviceRegistryWrapper, Document document) {
        Element root = XmlUtil.createElement(document, XmlMappingConstants.SERVICES_KEY, Optional.of(XmlMappingConstants.URN_OPENDAYLIGHT_PARAMS_XML_NS_YANG_CONTROLLER_CONFIG));

        Map<String, Map<String, Map<String, String>>> mappedServices = serviceRegistryWrapper.getMappedServices();
        for (Entry<String, Map<String, Map<String, String>>> namespaceToRefEntry : mappedServices.entrySet()) {

            for (Entry<String, Map<String, String>> serviceEntry : namespaceToRefEntry.getValue().entrySet()) {
                // service belongs to config.yang namespace
                Element serviceElement = XmlUtil.createElement(document, SERVICE_KEY, Optional.<String>absent());
                root.appendChild(serviceElement);

                // type belongs to config.yang namespace
                String serviceType = serviceEntry.getKey();
                Element typeElement = XmlUtil.createTextElementWithNamespacedContent(document, XmlMappingConstants.TYPE_KEY,
                        XmlMappingConstants.PREFIX, namespaceToRefEntry.getKey(), serviceType);

                serviceElement.appendChild(typeElement);

                for (Entry<String, String> instanceEntry : serviceEntry.getValue().entrySet()) {
                    Element instanceElement = XmlUtil.createElement(document, XmlMappingConstants.INSTANCE_KEY, Optional.<String>absent());
                    serviceElement.appendChild(instanceElement);

                    Element nameElement = XmlUtil.createTextElement(document, NAME_KEY, instanceEntry.getKey(), Optional.<String>absent());
                    instanceElement.appendChild(nameElement);

                    Element providerElement = XmlUtil.createTextElement(document, PROVIDER_KEY, instanceEntry.getValue(), Optional.<String>absent());
                    instanceElement.appendChild(providerElement);
                }
            }

        }
        return root;
    }

    public static final class ServiceInstance {
        public static final ServiceInstance EMPTY_SERVICE_INSTANCE = new ServiceInstance("", "");

        public ServiceInstance(String moduleName, String instanceName) {
            this.moduleName = moduleName;
            this.instanceName = instanceName;
        }

        public static ServiceInstance fromString(String instanceId) {
            instanceId = instanceId.trim();
            Matcher matcher = p.matcher(instanceId);
            if(!matcher.matches()) {
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
                + XmlMappingConstants.MODULES_KEY + "/" + XmlMappingConstants.MODULE_KEY + "["
                + XmlMappingConstants.TYPE_KEY + "='%s']["
                + XmlMappingConstants.NAME_KEY + "='%s']";

        // TODO unify with xpath in RuntimeRpc

        // Previous version of xpath, needs to be supported for backwards compatibility (persisted configs by config-persister)
        private static final String blueprintRDeprecated = "/" + XmlMappingConstants.CONFIG_KEY + "/"
                + XmlMappingConstants.MODULES_KEY + "/" + XmlMappingConstants.MODULE_KEY + "\\["
                + XmlMappingConstants.NAME_KEY + "='%s'\\]/" + XmlMappingConstants.INSTANCE_KEY + "\\["
                + XmlMappingConstants.NAME_KEY + "='%s'\\]";

        private static final String blueprintR = "/"
                + XmlMappingConstants.MODULES_KEY + "/" + XmlMappingConstants.MODULE_KEY + "\\["
                + XmlMappingConstants.TYPE_KEY + "='%s'\\]\\["
                + XmlMappingConstants.NAME_KEY + "='%s'\\]";

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
            if (this == obj){
                return true;
            }
            if (obj == null){
                return false;
            }
            if (getClass() != obj.getClass()){
                return false;
            }
            ServiceInstance other = (ServiceInstance) obj;
            if (instanceName == null) {
                if (other.instanceName != null){
                    return false;
                }
            } else if (!instanceName.equals(other.instanceName)){
                return false;
            }
            if (moduleName == null) {
                if (other.moduleName != null){
                    return false;
                }
            } else if (!moduleName.equals(other.moduleName)){
                return false;
            }
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
