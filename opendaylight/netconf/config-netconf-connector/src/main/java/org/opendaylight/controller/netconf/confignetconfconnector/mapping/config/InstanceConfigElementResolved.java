/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.confignetconfconnector.mapping.config;

import com.google.common.base.Preconditions;
import com.google.common.collect.Multimap;
import org.opendaylight.controller.netconf.confignetconfconnector.mapping.attributes.fromxml.AttributeConfigElement;
import org.opendaylight.controller.netconf.confignetconfconnector.operations.editconfig.EditConfigStrategy;
import org.opendaylight.controller.netconf.confignetconfconnector.operations.editconfig.EditConfigXmlParser;
import org.opendaylight.controller.netconf.confignetconfconnector.operations.editconfig.EditStrategyType;

import java.util.Map;

/**
 * Parsed xml element containing whole configuration for an instance of some
 * module. Contains preferred edit strategy type.
 */
public class InstanceConfigElementResolved {

    private final EditStrategyType editStrategy;
    private final Map<String, AttributeConfigElement> configuration;
    private final Multimap<String, String> providedServices;

    public InstanceConfigElementResolved(String currentStrategy, Map<String, AttributeConfigElement> configuration, EditStrategyType defaultStrategy, Multimap<String, String> providedServices) {
        EditStrategyType valueOf = parseStrategy(currentStrategy, defaultStrategy);
        this.editStrategy = valueOf;
        this.configuration = configuration;
        this.providedServices = providedServices;
    }

    public InstanceConfigElementResolved(Map<String, AttributeConfigElement> configuration, EditStrategyType defaultStrategy, Multimap<String, String> providedServices) {
        editStrategy = defaultStrategy;
        this.configuration = configuration;
        this.providedServices = providedServices;
    }


    static EditStrategyType parseStrategy(String currentStrategy, EditStrategyType defaultStrategy) {
        EditStrategyType parsedStrategy = EditStrategyType.valueOf(currentStrategy);
        if (defaultStrategy.isEnforcing()) {
            Preconditions
                    .checkArgument(
                            parsedStrategy == defaultStrategy,
                            "With "
                                    + defaultStrategy
                                    + " as "
                                    + EditConfigXmlParser.DEFAULT_OPERATION_KEY
                                    + " operations on module elements are not permitted since the default option is restrictive");
        }
        return parsedStrategy;
    }


    public EditConfigStrategy getEditStrategy() {
        return editStrategy.getFittingStrategy(providedServices);
    }

    public Map<String, AttributeConfigElement> getConfiguration() {
        return configuration;
    }
}
