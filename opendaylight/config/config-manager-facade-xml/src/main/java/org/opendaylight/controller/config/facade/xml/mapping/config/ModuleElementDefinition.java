/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.facade.xml.mapping.config;

import org.opendaylight.controller.config.facade.xml.exception.OperationNotPermittedException;
import org.opendaylight.controller.config.facade.xml.strategy.EditConfigStrategy;
import org.opendaylight.controller.config.facade.xml.strategy.EditStrategyType;
import org.opendaylight.controller.config.facade.xml.strategy.MissingInstanceHandlingStrategy;
import org.opendaylight.controller.config.facade.xml.strategy.NoneEditConfigStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModuleElementDefinition {

    public static final NoneEditConfigStrategy NONE_EDIT_CONFIG_STRATEGY = new NoneEditConfigStrategy();
    public static final MissingInstanceHandlingStrategy MISSING_INSTANCE_HANDLING_STRATEGY = new MissingInstanceHandlingStrategy();

    private final String instanceName;
    private final EditStrategyType editStrategy;
    private static final Logger LOG = LoggerFactory.getLogger(ModuleElementDefinition.class);

    public ModuleElementDefinition(String instanceName, String currentStrategy, EditStrategyType defaultStrategy) {
        this.instanceName = instanceName;
        if (currentStrategy == null || currentStrategy.isEmpty()) {
            this.editStrategy = defaultStrategy;
        } else {
            EditStrategyType _edStrategy = null;
            try {
                _edStrategy = InstanceConfigElementResolved.parseStrategy(currentStrategy, defaultStrategy);
            } catch (OperationNotPermittedException e) {
                _edStrategy = defaultStrategy;
                LOG.warn("Operation not permitted on current strategy {} while default strategy is {}. Element definition strategy set to default.",
                        currentStrategy,
                        defaultStrategy,
                        e);
            }
            this.editStrategy = _edStrategy;
        }

    }

    public String getInstanceName() {
        return instanceName;
    }

    public EditConfigStrategy getEditStrategy() {
        switch (editStrategy) {
        case delete :
        case remove :
        case none :
            return NONE_EDIT_CONFIG_STRATEGY;
        default :
            return MISSING_INSTANCE_HANDLING_STRATEGY;
        }
    }
}
