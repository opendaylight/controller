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
