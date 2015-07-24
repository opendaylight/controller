/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.facade.xml.mapping.config;

import com.google.common.base.Optional;
import java.util.Date;
import java.util.Map;
import javax.management.ObjectName;
import org.opendaylight.controller.config.api.jmx.ObjectNameUtil;
import org.opendaylight.controller.config.facade.xml.mapping.IdentityMapping;
import org.opendaylight.controller.config.facade.xml.osgi.EnumResolver;
import org.opendaylight.controller.config.facade.xml.strategy.EditStrategyType;
import org.opendaylight.controller.config.util.xml.DocumentedException;
import org.opendaylight.controller.config.util.xml.XmlElement;
import org.opendaylight.controller.config.util.xml.XmlMappingConstants;
import org.opendaylight.controller.config.util.xml.XmlUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class ModuleConfig {

    private final String moduleName;
    private final InstanceConfig instanceConfig;

    public ModuleConfig(String moduleName, InstanceConfig mbeanMapping) {
        this.moduleName = moduleName;
        this.instanceConfig = mbeanMapping;
    }

    public Element toXml(ObjectName instanceON, Document document, String namespace, final EnumResolver enumResolver) {
        Element root = XmlUtil.createElement(document, XmlMappingConstants.MODULE_KEY, Optional.<String>absent());

        // type belongs to config.yang namespace, but needs to be <type prefix:moduleNS>prefix:moduleName</type>

        Element typeElement = XmlUtil.createTextElementWithNamespacedContent(document, XmlMappingConstants.TYPE_KEY,
                XmlMappingConstants.PREFIX, namespace, moduleName);

        root.appendChild(typeElement);
        // name belongs to config.yang namespace
        String instanceName = ObjectNameUtil.getInstanceName(instanceON);
        Element nameElement = XmlUtil.createTextElement(document, XmlMappingConstants.NAME_KEY, instanceName, Optional.<String>absent());

        root.appendChild(nameElement);

        root = instanceConfig.toXml(instanceON, namespace, document, root, enumResolver);

        return root;
    }

    public ModuleElementResolved fromXml(XmlElement moduleElement, ServiceRegistryWrapper depTracker, String instanceName,
                                         String moduleNamespace, EditStrategyType defaultStrategy, Map<String, Map<Date, IdentityMapping>> identityMap, final EnumResolver enumResolver) throws DocumentedException {

        InstanceConfigElementResolved ice = instanceConfig.fromXml(moduleElement, depTracker, moduleNamespace, defaultStrategy, identityMap, enumResolver);
        return new ModuleElementResolved(instanceName, ice);
    }

}
