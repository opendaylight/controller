package org.opendaylight.controller.connectionmanager.scheme;

import java.net.InetAddress;
import java.util.Set;
import org.opendaylight.controller.clustering.services.IClusterGlobalServices;
import org.opendaylight.controller.connectionmanager.ConnectionMgmtScheme;
import org.opendaylight.controller.sal.core.Node;

class SingleControllerScheme extends AbstractScheme {

    private static AbstractScheme myScheme= null;

    protected SingleControllerScheme(IClusterGlobalServices clusterServices) {
        super(clusterServices, ConnectionMgmtScheme.SINGLE_CONTROLLER);
    }

    public static AbstractScheme getScheme(IClusterGlobalServices clusterServices) {
        if (myScheme == null) {
            myScheme = new SingleControllerScheme(clusterServices);
        }
        return myScheme;
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean isConnectionAllowedInternal(Node node) {
        if (nodeConnections == null) return true;
        for (Node existingNode : nodeConnections.keySet()) {
            Set<InetAddress> controllers = nodeConnections.get(existingNode);
            if (controllers == null || controllers.size() == 0) continue;
            if (!controllers.contains(clusterServices.getMyAddress())) return false;
        }
        return true;
    }
}
