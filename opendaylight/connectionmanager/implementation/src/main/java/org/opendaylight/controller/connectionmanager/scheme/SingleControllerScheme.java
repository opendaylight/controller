/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.connectionmanager.scheme;

import org.opendaylight.controller.clustering.services.IClusterGlobalServices;
import org.opendaylight.controller.connectionmanager.ConnectionMgmtScheme;
import org.opendaylight.controller.sal.core.Node;

class SingleControllerScheme extends AbstractScheme {

    private static AbstractScheme myScheme = null;

    protected SingleControllerScheme(IClusterGlobalServices clusterServices) {
        super(clusterServices, ConnectionMgmtScheme.SINGLE_CONTROLLER);
    }

    public static AbstractScheme getScheme(IClusterGlobalServices clusterServices) {
        if (myScheme == null) {
            myScheme = new SingleControllerScheme(clusterServices);
        }
        return myScheme;
    }

    @Override
    public boolean isConnectionAllowedInternal(Node node) {
        // Lets make it simple. The Cluster Coordinator is the master
        return clusterServices.amICoordinator();
    }
}
