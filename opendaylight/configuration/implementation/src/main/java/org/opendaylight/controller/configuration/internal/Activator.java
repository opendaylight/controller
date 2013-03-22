
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.configuration.internal;

import org.apache.felix.dm.Component;
import org.opendaylight.controller.clustering.services.IClusterGlobalServices;
import org.opendaylight.controller.configuration.IConfigurationAware;
import org.opendaylight.controller.configuration.IConfigurationContainerAware;
import org.opendaylight.controller.configuration.IConfigurationContainerService;
import org.opendaylight.controller.configuration.IConfigurationService;
import org.opendaylight.controller.sal.core.ComponentActivatorAbstractBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @file Activator.java
 *
 * @brief Component Activator for Configuration Management.
 *
 *
 *
 */
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
    public Object[] getImplementations() {
        Object[] res = { ConfigurationContainerImpl.class };
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
     * @param containerName The containerName being configured, this allow
     * also optional per-container different behavior if needed, usually
     * should not be the case though.
     */
    public void configureInstance(Component c, Object imp, String containerName) {
        if (imp.equals(ConfigurationContainerImpl.class)) {
            // export the service
            c.setInterface(new String[] {
                    IConfigurationContainerService.class.getName(),
                    IConfigurationAware.class.getName() }, null);

            c.add(createContainerServiceDependency(containerName).setService(
                    IConfigurationContainerAware.class).setCallbacks(
                    "addConfigurationContainerAware",
                    "removeConfigurationContainerAware").setRequired(false));
        }
    }

    /**
     * Method which tells how many Global implementations are
     * supported by the bundle. This way we can tune the number of
     * components created. This components will be created ONLY at the
     * time of bundle startup and will be destroyed only at time of
     * bundle destruction, this is the major difference with the
     * implementation retrieved via getImplementations where all of
     * them are assumed to be in a container!
     *
     *
     * @return The list of implementations the bundle will support,
     * in Global version
     */
    protected Object[] getGlobalImplementations() {
        Object[] res = { ConfigurationImpl.class };
        return res;
    }

    /**
     * Configure the dependency for a given instance Global
     *
     * @param c Component assigned for this instance, this will be
     * what will be used for configuration
     * @param imp implementation to be configured
     * @param containerName container on which the configuration happens
     */
    protected void configureGlobalInstance(Component c, Object imp) {
        if (imp.equals(ConfigurationImpl.class)) {

            // export the service
            c.setInterface(
                    new String[] { IConfigurationService.class.getName() },
                    null);

            c.add(createServiceDependency().setService(
                    IClusterGlobalServices.class).setCallbacks(
                    "setClusterServices", "unsetClusterServices").setRequired(
                    true));

            c.add(createServiceDependency().setService(
                    IConfigurationAware.class).setCallbacks(
                    "addConfigurationAware", "removeConfigurationAware")
                    .setRequired(false));
        }
    }
}
