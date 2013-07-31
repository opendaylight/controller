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
