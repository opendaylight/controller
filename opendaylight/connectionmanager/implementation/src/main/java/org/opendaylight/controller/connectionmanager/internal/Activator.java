
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.connectionmanager.internal;

import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

import org.opendaylight.controller.clustering.services.ICacheUpdateAware;
import org.opendaylight.controller.clustering.services.IClusterGlobalServices;
import org.opendaylight.controller.clustering.services.ICoordinatorChangeAware;
import org.opendaylight.controller.connectionmanager.ConnectionMgmtScheme;
import org.opendaylight.controller.connectionmanager.IConnectionManager;
import org.opendaylight.controller.sal.connection.IConnectionListener;
import org.opendaylight.controller.sal.connection.IConnectionService;
import org.apache.felix.dm.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.opendaylight.controller.sal.core.ComponentActivatorAbstractBase;
import org.opendaylight.controller.sal.inventory.IListenInventoryUpdates;

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
        Object[] res = { ConnectionManager.class };
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
        if (imp.equals(ConnectionManager.class)) {
            Dictionary<String, Object> props = new Hashtable<String, Object>();
            Set<String> propSet = new HashSet<String>();
            for (ConnectionMgmtScheme scheme:ConnectionMgmtScheme.values()) {
                propSet.add("connectionmanager."+scheme.name()+".nodeconnections");
            }
            props.put("cachenames", propSet);
            props.put("scope", "Global");

            // export the service
            c.setInterface(new String[] { IConnectionManager.class.getName(),
                                          IConnectionListener.class.getName(),
                                          ICoordinatorChangeAware.class.getName(),
                                          IListenInventoryUpdates.class.getName(),
                                          ICacheUpdateAware.class.getName()},
                                          props);

            c.add(createServiceDependency()
                    .setService(IClusterGlobalServices.class)
                    .setCallbacks("setClusterServices", "unsetClusterServices")
                    .setRequired(true));

            c.add(createServiceDependency().setService(IConnectionService.class)
                    .setCallbacks("setConnectionService", "unsetConnectionService")
                    .setRequired(true));
        }
    }
}
