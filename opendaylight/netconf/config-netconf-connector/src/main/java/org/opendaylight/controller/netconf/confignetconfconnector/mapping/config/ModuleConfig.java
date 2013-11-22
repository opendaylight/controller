/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.confignetconfconnector.mapping.config;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.opendaylight.controller.config.api.jmx.ObjectNameUtil;
import org.opendaylight.controller.netconf.confignetconfconnector.operations.editconfig.EditStrategyType;
import org.opendaylight.controller.netconf.util.xml.XmlElement;
import org.opendaylight.controller.netconf.util.xml.XmlNetconfConstants;
import org.opendaylight.controller.netconf.util.xml.XmlUtil;
import org.opendaylight.yangtools.yang.common.QName;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.management.ObjectName;
import java.util.Collection;

public class ModuleConfig {

    private final String moduleName;
    private final InstanceConfig instanceConfig;
    private final Multimap<String, String> providedServices;

    public ModuleConfig(String moduleName, InstanceConfig mbeanMapping, Collection<QName> providedServices) {
        this.moduleName = moduleName;
        this.instanceConfig = mbeanMapping;
        this.providedServices = mapServices(providedServices);
    }

    private Multimap<String, String> mapServices(Collection<QName> providedServices) {
        Multimap<String, String> mapped = HashMultimap.create();

        for (QName providedService : providedServices) {
            String key = providedService.getNamespace().toString();
            mapped.put(key, providedService.getLocalName());
        }

        return  mapped;
    }

    public InstanceConfig getMbeanMapping() {
        return instanceConfig;
    }

    public Multimap<String, String> getProvidedServices() {
        return providedServices;
    }

    public Element toXml(ObjectName instanceON, Services depTracker, Document document, String namespace) {
        Element root = document.createElement(XmlNetconfConstants.MODULE_KEY);
        // Xml.addNamespaceAttr(document, root, namespace);

        final String prefix = getPrefix(namespace);
        Element typeElement = XmlUtil.createPrefixedTextElement(document, XmlNetconfConstants.TYPE_KEY, prefix,
                moduleName);
        XmlUtil.addPrefixedNamespaceAttr(typeElement, prefix, namespace);
        // Xml.addNamespaceAttr(document, typeElement,
        // XMLUtil.URN_OPENDAYLIGHT_PARAMS_XML_NS_YANG_CONTROLLER_CONFIG);
        root.appendChild(typeElement);

        Element nameElement = XmlUtil.createTextElement(document, XmlNetconfConstants.NAME_KEY,
                ObjectNameUtil.getInstanceName(instanceON));
        // Xml.addNamespaceAttr(document, nameElement,
        // XMLUtil.URN_OPENDAYLIGHT_PARAMS_XML_NS_YANG_CONTROLLER_CONFIG);
        root.appendChild(nameElement);

        root = instanceConfig.toXml(instanceON, depTracker, namespace, document, root);

        return root;
    }

    private String getPrefix(String namespace) {
        // if(namespace.contains(":")==false)
        return XmlNetconfConstants.PREFIX;
        // return namespace.substring(namespace.lastIndexOf(':') + 1,
        // namespace.length());

    }

    public ModuleElementResolved fromXml(XmlElement moduleElement, Services depTracker, String instanceName,
            String moduleNamespace, EditStrategyType defaultStrategy) {

        InstanceConfigElementResolved ice = instanceConfig.fromXml(moduleElement, depTracker, moduleNamespace, defaultStrategy);
        return new ModuleElementResolved(instanceName, ice);
    }

}
