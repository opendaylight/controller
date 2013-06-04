
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.clustering.test.internal;

import org.opendaylight.controller.sal.core.ComponentActivatorAbstractBase;
import org.opendaylight.controller.clustering.services.IClusterServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.felix.dm.Component;

public class Activator extends ComponentActivatorAbstractBase {
    protected static final Logger logger = LoggerFactory
            .getLogger(Activator.class);

    /**
     * Function called when the activator starts just after some
     * initializations are done by the
     * ComponentActivatorAbstractBase.
     *
     */
    public void init() {
    }

    /**
     * Function called when the activator stops just before the
     * cleanup done by ComponentActivatorAbstractBase
     *
     */
    public void destroy() {
    }

    /**
     * Function that is used to communicate to dependency manager the
     * list of known implementations for services inside a container
     *
     *
     * @return An array containing all the CLASS objects that will be
     * instantiated in order to get an fully working implementation
     * Object
     */
    public Object[] getGlobalImplementations() {
        Object[] res = { SimpleClient.class };
        return res;
    }

    /**
     * Function that is called when configuration of the dependencies
     * is required.
     *
     * @param c dependency manager Component object, used for
     * configuring the dependencies exported and imported
     * @param imp Implementation class that is being configured,
     * needed as long as the same routine can configure multiple
     * implementations
     */
    public void configureGlobalInstance(Component c, Object imp) {
        if (imp.equals(SimpleClient.class)) {
            // export the service for Apps and Plugins
            c.setInterface(new String[] { org.eclipse.osgi.framework.console.CommandProvider.class.getName() },
                           null);
            c.add(createServiceDependency().setService(IClusterServices.class)
                  .setCallbacks("setIClusterServices", "unsetIClusterServices")
                  .setRequired(true));
        }
    }
}
