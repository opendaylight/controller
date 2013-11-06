
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.datastore.internal;

import org.apache.felix.dm.Component;
import org.opendaylight.controller.clustering.services.IClusterGlobalServices;
import org.opendaylight.controller.datastore.ClusteredDataStore;
import org.opendaylight.controller.sal.core.ComponentActivatorAbstractBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Set;

public class Activator extends ComponentActivatorAbstractBase {
    protected static final Logger logger = LoggerFactory
            .getLogger(Activator.class);


    @Override
    protected Object[] getGlobalImplementations(){
    	logger.debug("Calling getGlobalImplementations to return:", ClusteredDataStoreManager.class);
        return new Object[] {
            ClusteredDataStoreManager.class
        };
    }


    @Override
    protected void configureGlobalInstance(Component c, Object imp){
        if (imp.equals(ClusteredDataStoreManager.class)) {
            Dictionary<String, Set<String>> props = new Hashtable<String, Set<String>>();

            c.setInterface(new String[] { ClusteredDataStore.class.getName() }, props);
            logger.debug("configureGlobalInstance adding dependency:", IClusterGlobalServices.class);
            
            c.add(createServiceDependency().setService(
                    IClusterGlobalServices.class).setCallbacks(
                    "setClusterGlobalServices",
                    "unsetClusterGlobalServices").setRequired(true));

        }
    }


}
