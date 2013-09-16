package org.opendaylight.controller.connectionmanager.scheme;

import java.net.InetAddress;
import java.util.Set;

import org.opendaylight.controller.clustering.services.IClusterGlobalServices;
import org.opendaylight.controller.connectionmanager.ConnectionMgmtScheme;
import org.opendaylight.controller.sal.core.Node;

class AnyControllerScheme extends AbstractScheme {
    private static AbstractScheme myScheme= null;

    protected AnyControllerScheme(IClusterGlobalServices clusterServices) {
        super(clusterServices, ConnectionMgmtScheme.ANY_CONTROLLER_ONE_MASTER);
    }

    public static AbstractScheme getScheme(IClusterGlobalServices clusterServices) {
        if (myScheme == null) {
            myScheme = new AnyControllerScheme(clusterServices);
        }
        return myScheme;
    }

    @Override
    public boolean isConnectionAllowedInternal(Node node) {
        Set <InetAddress> controllers = nodeConnections.get(node);
        if (controllers == null || controllers.size() == 0) return true;
        return (controllers.size() == 1 && controllers.contains(clusterServices.getMyAddress()));
    }
}
