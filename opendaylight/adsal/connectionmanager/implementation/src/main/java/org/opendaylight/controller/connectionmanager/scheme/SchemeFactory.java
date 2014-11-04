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

public class SchemeFactory {
    public static AbstractScheme getScheme(ConnectionMgmtScheme scheme, IClusterGlobalServices clusterServices) {
        if (scheme == ConnectionMgmtScheme.SINGLE_CONTROLLER) {
            return SingleControllerScheme.getScheme(clusterServices);
        } else if (scheme == ConnectionMgmtScheme.ROUND_ROBIN) {
            return RoundRobinScheme.getScheme(clusterServices);
        } else if (scheme == ConnectionMgmtScheme.LOAD_BALANCED) {
            return LoadBalancedScheme.getScheme(clusterServices);
        } else if (scheme == ConnectionMgmtScheme.ANY_CONTROLLER_ONE_MASTER) {
            return AnyControllerScheme.getScheme(clusterServices);
        }
        return null;
    }
}
