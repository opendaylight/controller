/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.connector.remoterpc.impl;

import org.apache.felix.dm.Component;
import org.opendaylight.controller.clustering.services.ICacheUpdateAware;
import org.opendaylight.controller.clustering.services.IClusterGlobalServices;
import org.opendaylight.controller.sal.connector.remoterpc.api.RouteChangeListener;
import org.opendaylight.controller.sal.connector.remoterpc.api.RoutingTable;
import org.opendaylight.controller.sal.core.ComponentActivatorAbstractBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

/**
 * @author: syedbahm
 */
public class Activator extends ComponentActivatorAbstractBase {

    protected static final Logger logger = LoggerFactory
            .getLogger(Activator.class);
    private static final String CACHE_UPDATE_AWARE_REGISTRY_KEY = "cachenames" ;


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

    @Override
    protected Object[] getGlobalImplementations(){
        logger.debug("Calling getGlobalImplementations to return:", RoutingTableImpl.class);
        return new Object[] {
                RoutingTableImpl.class
        };
    }

    /**
     * Configure the dependency for a given instance Global
     *
     * @param c Component assigned for this instance, this will be
     * what will be used for configuration
     * @param imp implementation to be configured
     *
     */
    @Override
    protected void configureGlobalInstance(Component c, Object imp){
        if (imp.equals(RoutingTableImpl.class)) {
            Dictionary<String, Set<String>> props = new Hashtable<String, Set<String>>();
            Set<String> propSet = new HashSet<String>();
            propSet.add(RoutingTableImpl.ROUTING_TABLE_GLOBAL_CACHE);
            props.put(CACHE_UPDATE_AWARE_REGISTRY_KEY, propSet);

            c.setInterface(new String[] { RoutingTable.class.getName(),ICacheUpdateAware.class.getName()  }, props);
            logger.debug("configureGlobalInstance adding dependency:", IClusterGlobalServices.class);


            // RouteChangeListener services will be none or many so the
            // dependency is optional
            c.add(createServiceDependency()
                    .setService(RouteChangeListener.class)
                    .setCallbacks("setRouteChangeListener", "unsetRouteChangeListener")
                    .setRequired(false));

            //dependency is required as it provides us the caching support
            c.add(createServiceDependency().setService(
                    IClusterGlobalServices.class).setCallbacks(
                    "setClusterGlobalServices",
                    "unsetClusterGlobalServices").setRequired(true));

        }
    }


}
