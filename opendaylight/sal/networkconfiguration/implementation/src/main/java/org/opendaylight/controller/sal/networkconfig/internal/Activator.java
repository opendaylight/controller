/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.networkconfig.internal;

import org.apache.felix.dm.Component;
import org.opendaylight.controller.sal.core.ComponentActivatorAbstractBase;
import org.opendaylight.controller.sal.networkconfig.bridgedomain.IBridgeDomainConfigService;
import org.opendaylight.controller.sal.networkconfig.bridgedomain.IPluginInBridgeDomainConfigService;
import org.opendaylight.controller.sal.networkconfig.bridgedomain.internal.BridgeDomainConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Activator extends ComponentActivatorAbstractBase {
    protected static final Logger logger = LoggerFactory
            .getLogger(Activator.class);


    /**
     * Function that is used to communicate to dependency manager the list of
     * known Global implementations
     *
     *
     * @return An array containing all the CLASS objects that will be
     *         instantiated in order to get an fully working implementation
     *         Object
     */
    public Object[] getGlobalImplementations() {
        Object[] res = { BridgeDomainConfigService.class};
        return res;
    }

    /**
     * Function that is called when configuration of the dependencies is required.
     *
     * @param c
     *            dependency manager Component object, used for configuring the
     *            dependencies exported and imported
     * @param imp
     *            Implementation class that is being configured, needed as long
     *            as the same routine can configure multiple implementations
     */
    public void configureGlobalInstance(Component c, Object imp) {
        if (imp.equals(BridgeDomainConfigService.class)) {
            c.setInterface(
                    new String[] { IBridgeDomainConfigService.class.getName()},
                                   null);

            c.add(createServiceDependency()
                    .setService(IPluginInBridgeDomainConfigService.class)
                    .setCallbacks("setPluginInService", "unsetPluginInService")
                    .setRequired(false));
        }
    }
}
