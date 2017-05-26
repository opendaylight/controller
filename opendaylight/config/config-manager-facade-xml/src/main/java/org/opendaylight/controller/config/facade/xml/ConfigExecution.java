/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.facade.xml;

import com.google.common.collect.Multimap;
import java.util.Map;
import org.opendaylight.controller.config.api.ServiceReferenceReadableRegistry;
import org.opendaylight.controller.config.facade.xml.mapping.config.Config;
import org.opendaylight.controller.config.facade.xml.mapping.config.ModuleElementDefinition;
import org.opendaylight.controller.config.facade.xml.mapping.config.ModuleElementResolved;
import org.opendaylight.controller.config.facade.xml.mapping.config.ServiceRegistryWrapper;
import org.opendaylight.controller.config.facade.xml.mapping.config.Services;
import org.opendaylight.controller.config.facade.xml.strategy.EditStrategyType;
import org.opendaylight.controller.config.util.xml.DocumentedException;
import org.opendaylight.controller.config.util.xml.XmlElement;

public class ConfigExecution {

    private final TestOption testOption;
    private final EditStrategyType defaultEditStrategyType;
    private final Services services;
    private final Config configResolver;
    private final XmlElement configElement;

    public ConfigExecution(Config configResolver, XmlElement configElement, TestOption testOption, EditStrategyType defaultStrategy) throws DocumentedException {
        Config.checkUnrecognisedChildren(configElement);
        this.configResolver = configResolver;
        this.configElement = configElement;
        this.services = configResolver.fromXmlServices(configElement);
        this.testOption = testOption;
        this.defaultEditStrategyType = defaultStrategy;
    }

    public boolean shouldTest() {
        return testOption == TestOption.testOnly || testOption == TestOption.testThenSet;
    }

    public boolean shouldSet() {
        return testOption == TestOption.set || testOption == TestOption.testThenSet;
    }

    public Map<String, Multimap<String, ModuleElementResolved>> getResolvedXmlElements(ServiceReferenceReadableRegistry serviceRegistry) throws DocumentedException {
        return configResolver.fromXmlModulesResolved(configElement, defaultEditStrategyType, getServiceRegistryWrapper(serviceRegistry));
    }

    public ServiceRegistryWrapper getServiceRegistryWrapper(ServiceReferenceReadableRegistry serviceRegistry) {
        // TODO cache service registry
        return new ServiceRegistryWrapper(serviceRegistry);
    }

    public Map<String, Multimap<String,ModuleElementDefinition>> getModulesDefinition(ServiceReferenceReadableRegistry serviceRegistry) throws DocumentedException {
        return configResolver.fromXmlModulesMap(configElement, defaultEditStrategyType, getServiceRegistryWrapper(serviceRegistry));
    }

    public EditStrategyType getDefaultStrategy() {
        return defaultEditStrategyType;
    }

    public Services getServices() {
        return services;
    }

    public XmlElement getConfigElement() {
        return configElement;
    }
}
