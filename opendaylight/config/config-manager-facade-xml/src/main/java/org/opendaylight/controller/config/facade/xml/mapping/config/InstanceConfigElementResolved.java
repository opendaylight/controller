/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.facade.xml.mapping.config;

import java.util.Map;
import org.opendaylight.controller.config.facade.xml.exception.OperationNotPermittedException;
import org.opendaylight.controller.config.facade.xml.mapping.attributes.fromxml.AttributeConfigElement;
import org.opendaylight.controller.config.facade.xml.strategy.EditConfigStrategy;
import org.opendaylight.controller.config.facade.xml.strategy.EditStrategyType;
import org.opendaylight.controller.config.util.xml.DocumentedException;

/**
 * Parsed xml element containing whole configuration for an instance of some
 * module. Contains preferred edit strategy type.
 */
public class InstanceConfigElementResolved {

    private final EditStrategyType editStrategy;
    private final Map<String, AttributeConfigElement> configuration;

    public InstanceConfigElementResolved(String currentStrategy, Map<String, AttributeConfigElement> configuration,
                                         EditStrategyType defaultStrategy)
            throws DocumentedException {
        this.editStrategy = parseStrategy(currentStrategy, defaultStrategy);
        this.configuration = configuration;
    }

    public InstanceConfigElementResolved(Map<String, AttributeConfigElement> configuration, EditStrategyType defaultStrategy) {
        editStrategy = defaultStrategy;
        this.configuration = configuration;
    }


    static EditStrategyType parseStrategy(String currentStrategy, EditStrategyType defaultStrategy) throws OperationNotPermittedException {
        EditStrategyType parsedStrategy = EditStrategyType.valueOf(currentStrategy);
        EditStrategyType.compareParsedStrategyToDefaultEnforcing(parsedStrategy,defaultStrategy);
        return parsedStrategy;
    }


    public EditConfigStrategy getEditStrategy() {
        return editStrategy.getFittingStrategy();
    }

    public Map<String, AttributeConfigElement> getConfiguration() {
        return configuration;
    }
}
