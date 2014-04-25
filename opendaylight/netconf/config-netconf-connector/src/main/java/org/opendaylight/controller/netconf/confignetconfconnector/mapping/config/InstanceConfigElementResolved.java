/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.confignetconfconnector.mapping.config;

import com.google.common.collect.Multimap;
import java.util.Map;
import org.opendaylight.controller.netconf.api.NetconfDocumentedException;
import org.opendaylight.controller.netconf.confignetconfconnector.exception.OperationNotPermittedException;
import org.opendaylight.controller.netconf.confignetconfconnector.mapping.attributes.fromxml.AttributeConfigElement;
import org.opendaylight.controller.netconf.confignetconfconnector.operations.editconfig.EditConfigStrategy;
import org.opendaylight.controller.netconf.confignetconfconnector.operations.editconfig.EditStrategyType;

/**
 * Parsed xml element containing whole configuration for an instance of some
 * module. Contains preferred edit strategy type.
 */
public class InstanceConfigElementResolved {

    private final EditStrategyType editStrategy;
    private final Map<String, AttributeConfigElement> configuration;
    private final Multimap<String, String> providedServices;

    public InstanceConfigElementResolved(String currentStrategy, Map<String, AttributeConfigElement> configuration, EditStrategyType defaultStrategy, Multimap<String, String> providedServices) throws NetconfDocumentedException {
        EditStrategyType valueOf = null;
        valueOf = parseStrategy(currentStrategy, defaultStrategy);
        this.editStrategy = valueOf;
        this.configuration = configuration;
        this.providedServices = providedServices;
    }

    public InstanceConfigElementResolved(Map<String, AttributeConfigElement> configuration, EditStrategyType defaultStrategy, Multimap<String, String> providedServices) {
        editStrategy = defaultStrategy;
        this.configuration = configuration;
        this.providedServices = providedServices;
    }


    static EditStrategyType parseStrategy(String currentStrategy, EditStrategyType defaultStrategy) throws OperationNotPermittedException {
        EditStrategyType parsedStrategy = EditStrategyType.valueOf(currentStrategy);
        EditStrategyType.compareParsedStrategyToDefaultEnforcing(parsedStrategy,defaultStrategy);
        return parsedStrategy;
    }


    public EditConfigStrategy getEditStrategy() {
        return editStrategy.getFittingStrategy(providedServices);
    }

    public Map<String, AttributeConfigElement> getConfiguration() {
        return configuration;
    }
}
