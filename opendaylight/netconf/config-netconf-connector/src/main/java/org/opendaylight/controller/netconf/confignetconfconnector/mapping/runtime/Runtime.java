/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.confignetconfconnector.mapping.runtime;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import org.opendaylight.controller.config.api.jmx.ObjectNameUtil;
import org.opendaylight.controller.netconf.util.xml.XmlNetconfConstants;
import org.opendaylight.controller.netconf.util.xml.XmlUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.management.ObjectName;
import java.util.Map;
import java.util.Set;

public class Runtime {

    private final Map<String, Map<String, ModuleRuntime>> moduleRuntimes;

    public Runtime(Map<String, Map<String, ModuleRuntime>> moduleRuntimes) {
        this.moduleRuntimes = moduleRuntimes;
    }

    private Map<String, Multimap<String, ObjectName>> mapInstancesToModules(Set<ObjectName> instancesToMap) {
        Map<String, Multimap<String, ObjectName>> retVal = Maps.newHashMap();

        for (ObjectName objectName : instancesToMap) {
            String moduleName = ObjectNameUtil.getFactoryName(objectName);

            Multimap<String, ObjectName> multimap = retVal.get(moduleName);
            if (multimap == null) {
                multimap = HashMultimap.create();
                retVal.put(moduleName, multimap);
            }

            String instanceName = ObjectNameUtil.getInstanceName(objectName);

            multimap.put(instanceName, objectName);
        }

        return retVal;
    }

    public Element toXml(Set<ObjectName> instancesToMap, Document document) {
        Element root = document.createElement(XmlNetconfConstants.DATA_KEY);

        Element modulesElement = document.createElement(XmlNetconfConstants.MODULES_KEY);
        XmlUtil.addNamespaceAttr(modulesElement,
                XmlNetconfConstants.URN_OPENDAYLIGHT_PARAMS_XML_NS_YANG_CONTROLLER_CONFIG);
        root.appendChild(modulesElement);

        Map<String, Multimap<String, ObjectName>> moduleToInstances = mapInstancesToModules(instancesToMap);

        for (String localNamespace : moduleRuntimes.keySet()) {
            for (String moduleName : moduleRuntimes.get(localNamespace).keySet()) {
                Multimap<String, ObjectName> instanceToRbe = moduleToInstances.get(moduleName);

                if (instanceToRbe == null)
                    continue;

                for (String instanceName : instanceToRbe.keySet()) {
                    ModuleRuntime moduleRuntime = moduleRuntimes.get(localNamespace).get(moduleName);
                    Element innerXml = moduleRuntime.toXml(localNamespace, instanceName, instanceToRbe.get(instanceName), document);
                    modulesElement.appendChild(innerXml);
                }

            }
        }

        return root;
    }

}
