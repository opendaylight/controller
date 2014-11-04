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

class RoundRobinScheme extends AbstractScheme {
    protected RoundRobinScheme(IClusterGlobalServices clusterServices) {
        super(clusterServices, ConnectionMgmtScheme.ROUND_ROBIN);
        // TODO Auto-generated constructor stub
    }

    public static AbstractScheme getScheme(IClusterGlobalServices clusterServices) {
        return null;
    }

    @Override
    public boolean isConnectionAllowedInternal(Node node) {
        // TODO Auto-generated method stub
        return false;
    }

}
