/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.facade.xml.runtime;

import com.google.common.base.Optional;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import javax.management.ObjectName;
import org.opendaylight.controller.config.api.jmx.ObjectNameUtil;
import org.opendaylight.controller.config.facade.xml.mapping.config.Config;
import org.opendaylight.controller.config.facade.xml.mapping.config.ModuleConfig;
import org.opendaylight.controller.config.facade.xml.osgi.EnumResolver;
import org.opendaylight.controller.config.util.xml.XmlMappingConstants;
import org.opendaylight.controller.config.util.xml.XmlUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class Runtime {

    private final Map<String, Map<String, ModuleRuntime>> moduleRuntimes;
    private final Map<String, Map<String, ModuleConfig>> moduleConfigs;

    public Runtime(Map<String, Map<String, ModuleRuntime>> moduleRuntimes,
            Map<String, Map<String, ModuleConfig>> moduleConfigs) {
        this.moduleRuntimes = moduleRuntimes;
        this.moduleConfigs = moduleConfigs;
    }

    private Map<String, Multimap<String, ObjectName>> mapInstancesToModules(Set<ObjectName> instancesToMap) {
        Map<String, Multimap<String, ObjectName>> retVal = Maps.newHashMap();

        // TODO map to namepsace, prevent module name conflicts
        // this code does not support same module names from different namespaces
        // Namespace should be present in ObjectName

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

    public Element toXml(Set<ObjectName> instancesToMap, Set<ObjectName> configBeans, Document document, final EnumResolver enumResolver) {
        Element root = XmlUtil.createElement(document, XmlMappingConstants.DATA_KEY, Optional.<String>absent());

        Element modulesElement = XmlUtil.createElement(document, XmlMappingConstants.MODULES_KEY, Optional.of(XmlMappingConstants.URN_OPENDAYLIGHT_PARAMS_XML_NS_YANG_CONTROLLER_CONFIG));
        root.appendChild(modulesElement);

        Map<String, Multimap<String, ObjectName>> moduleToRuntimeInstance = mapInstancesToModules(instancesToMap);
        Map<String, Map<String, Collection<ObjectName>>> moduleToConfigInstance = Config
            .getMappedInstances(configBeans, moduleConfigs);

        for (String localNamespace : moduleConfigs.keySet()) {

            Map<String, Collection<ObjectName>> instanceToMbe = moduleToConfigInstance.get(localNamespace);

            for (String moduleName : moduleConfigs.get(localNamespace).keySet()) {
                Multimap<String, ObjectName> instanceToRbe = moduleToRuntimeInstance.get(moduleName);

                for (ObjectName instanceON : instanceToMbe.get(moduleName)) {
                    String instanceName = ObjectNameUtil.getInstanceName(instanceON);

                    Element runtimeXml;
                    ModuleConfig moduleConfig = moduleConfigs.get(localNamespace).get(moduleName);
                    if(instanceToRbe==null || !instanceToRbe.containsKey(instanceName)) {
                        runtimeXml = moduleConfig.toXml(instanceON, document, localNamespace, enumResolver);
                    } else {
                        ModuleRuntime moduleRuntime = moduleRuntimes.get(localNamespace).get(moduleName);
                        runtimeXml = moduleRuntime.toXml(localNamespace, instanceToRbe.get(instanceName), document,
                                moduleConfig, instanceON, enumResolver);
                    }
                    modulesElement.appendChild(runtimeXml);
                }

            }
        }

        return root;
    }

}
