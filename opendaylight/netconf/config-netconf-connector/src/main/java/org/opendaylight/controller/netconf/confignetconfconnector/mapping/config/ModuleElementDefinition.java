/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.confignetconfconnector.mapping.config;

import org.opendaylight.controller.netconf.confignetconfconnector.operations.editconfig.EditConfigStrategy;
import org.opendaylight.controller.netconf.confignetconfconnector.operations.editconfig.EditStrategyType;
import org.opendaylight.controller.netconf.confignetconfconnector.operations.editconfig.MissingInstanceHandlingStrategy;
import org.opendaylight.controller.netconf.confignetconfconnector.operations.editconfig.NoneEditConfigStrategy;

public class ModuleElementDefinition {

    public static final NoneEditConfigStrategy NONE_EDIT_CONFIG_STRATEGY = new NoneEditConfigStrategy();
    public static final MissingInstanceHandlingStrategy MISSING_INSTANCE_HANDLING_STRATEGY = new MissingInstanceHandlingStrategy();

    private final String instanceName;
    private final EditStrategyType editStrategy;

    public ModuleElementDefinition(String instanceName, String currentStrategy, EditStrategyType defaultStrategy) {
        this.instanceName = instanceName;
        if (currentStrategy == null || currentStrategy.isEmpty())
            this.editStrategy = defaultStrategy;
        else
            this.editStrategy = InstanceConfigElementResolved.parseStrategy(currentStrategy, defaultStrategy);
    }

    public String getInstanceName() {
        return instanceName;
    }

    public EditStrategyType getEditStrategyType() {
        return editStrategy;
    }

    public EditConfigStrategy getEditStrategy() {
        switch (editStrategy) {
            case delete :
            case remove :
            case none : return NONE_EDIT_CONFIG_STRATEGY;
            default : return MISSING_INSTANCE_HANDLING_STRATEGY;
        }
    }
}
