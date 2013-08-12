/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.protocol_plugin.openflow.internal;

import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;

import org.apache.felix.dm.Component;
import org.opendaylight.controller.protocol_plugin.openflow.core.internal.EnhancedController;
import org.opendaylight.controller.protocol_plugin.openflow.core.internal.Controller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Openflow protocol plugin Activator
 *
 *
 */
public class EnhancedActivator extends Activator {
    protected static final Logger logger = LoggerFactory
            .getLogger(EnhancedActivator.class);

    // Default Constructor for the activator
    public EnhancedActivator() {
        super();
        logger.debug("Enhanced activator called!");
    }

    /**
     * Function that is used to communicate to dependency manager the list of
     * known implementations for services that are container independent.
     *
     *
     * @return An array containing all the CLASS objects that will be
     *         instantiated in order to get an fully working implementation
     *         Object
     */
    @Override
    public Object[] getGlobalImplementations() {
        Object[] res = super.getGlobalImplementations();
        // Now remove the Controller.class and return the
        // EnhancedController
        List resList = new ArrayList(Arrays.asList(res));
        resList.remove(Controller.class);
        resList.add(EnhancedController.class);
        return resList.toArray();
    }

    /**
     * Function that is called when configuration of the dependencies is
     * required.
     *
     * @param c
     *            dependency manager Component object, used for configuring the
     *            dependencies exported and imported
     * @param imp
     *            Implementation class that is being configured, needed as long
     *            as the same routine can configure multiple implementations
     */
    @Override
    public void configureGlobalInstance(Component c, Object imp) {
        if (imp.equals(EnhancedController.class)) {
            // Configure it like if was the Controller.class
            super.configureGlobalInstance(c, Controller.class);
        } else {
            super.configureGlobalInstance(c, imp);
        }
    }
}
