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

/**
 * Load Balancing scheme will let the nodes connect with controller based
 * on the resource usage in each of the controllers in a cluster.
 *
 * Incomplete and Currently not used.
 */

class LoadBalancedScheme extends AbstractScheme {

    protected LoadBalancedScheme(IClusterGlobalServices clusterServices) {
        super(clusterServices, ConnectionMgmtScheme.LOAD_BALANCED);
    }

    public static AbstractScheme getScheme(IClusterGlobalServices clusterServices) {
        return null;
    }

    @Override
    public boolean isConnectionAllowedInternal(Node node) {
        return false;
    }

}
