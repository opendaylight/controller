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

    public ModuleConfig(final String moduleName, final InstanceConfig mbeanMapping) {
        this.moduleName = moduleName;
        this.instanceConfig = mbeanMapping;
    }

    public Element toXml(final ObjectName instanceON, final Document document, final String namespace, final EnumResolver enumResolver) {
        final Optional<String> configNs =
                Optional.of(XmlMappingConstants.URN_OPENDAYLIGHT_PARAMS_XML_NS_YANG_CONTROLLER_CONFIG);
        Element root = XmlUtil.createElement(document, XmlMappingConstants.MODULE_KEY, configNs);

        // type belongs to config.yang namespace, but needs to be <type prefix:moduleNS>prefix:moduleName</type>

        Element typeElement = XmlUtil.createTextElementWithNamespacedContent(document, XmlMappingConstants.TYPE_KEY,
                XmlMappingConstants.PREFIX, namespace, moduleName, configNs);

        root.appendChild(typeElement);
        // name belongs to config.yang namespace
        String instanceName = ObjectNameUtil.getInstanceName(instanceON);
        Element nameElement = XmlUtil.createTextElement(document, XmlMappingConstants.NAME_KEY, instanceName, configNs);

        root.appendChild(nameElement);

        root = instanceConfig.toXml(instanceON, namespace, document, root, enumResolver);

        return root;
    }

    public ModuleElementResolved fromXml(final XmlElement moduleElement, final ServiceRegistryWrapper depTracker, final String instanceName,
                                         final String moduleNamespace, final EditStrategyType defaultStrategy, final Map<String, Map<Date, IdentityMapping>> identityMap, final EnumResolver enumResolver) throws DocumentedException {

        InstanceConfigElementResolved ice = instanceConfig.fromXml(moduleElement, depTracker, moduleNamespace, defaultStrategy, identityMap, enumResolver);
        return new ModuleElementResolved(instanceName, ice);
    }

}
