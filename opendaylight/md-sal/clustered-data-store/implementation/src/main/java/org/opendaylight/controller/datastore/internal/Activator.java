
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.datastore.internal;

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
        return new Object[] {
            ClusteredDataStoreManager.class
        };
    }


    @Override
    protected void configureGlobalInstance(org.apache.felix.dm.Component c, java.lang.Object imp){
        if (imp.equals(ClusteredDataStoreManager.class)) {
            Dictionary<String, Set<String>> props = new Hashtable<String, Set<String>>();

            c.setInterface(new String[] { ClusteredDataStore.class.getName() }, props);

            c.add(createServiceDependency().setService(
                    IClusterGlobalServices.class).setCallbacks(
                    "setClusterGlobalServices",
                    "unsetClusterGlobalServices").setRequired(true));

        }
    }


}
