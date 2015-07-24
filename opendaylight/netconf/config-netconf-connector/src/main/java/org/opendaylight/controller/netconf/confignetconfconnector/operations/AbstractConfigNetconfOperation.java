/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.confignetconfconnector.operations;

import org.opendaylight.controller.config.facade.xml.ConfigSubsystemFacade;
import org.opendaylight.controller.netconf.util.mapping.AbstractLastNetconfOperation;

public abstract class AbstractConfigNetconfOperation extends AbstractLastNetconfOperation {

    private final ConfigSubsystemFacade configSubsystemFacade;

    protected AbstractConfigNetconfOperation(ConfigSubsystemFacade configSubsystemFacade,
            String netconfSessionIdForReporting) {
        super(netconfSessionIdForReporting);
        this.configSubsystemFacade = configSubsystemFacade;
    }

    public ConfigSubsystemFacade getConfigSubsystemFacade() {
        return configSubsystemFacade;
    }
}
