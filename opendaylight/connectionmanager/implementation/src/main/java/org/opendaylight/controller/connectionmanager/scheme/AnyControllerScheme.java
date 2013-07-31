package org.opendaylight.controller.connectionmanager.scheme;

import java.net.InetAddress;
import java.util.Set;
import org.opendaylight.controller.clustering.services.IClusterGlobalServices;
import org.opendaylight.controller.connectionmanager.ConnectionMgmtScheme;
import org.opendaylight.controller.sal.core.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class AnyControllerScheme extends AbstractScheme {
    private static final Logger logger = LoggerFactory.getLogger(AnyControllerScheme.class);
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

    @SuppressWarnings("deprecation")
    @Override
    public boolean isConnectionAllowedInternal(Node node) {
        if (nodeConnections == null) return true;
        Set <InetAddress> controllers = nodeConnections.get(node);
        if (controllers == null || controllers.size() == 0) return true;
        return (controllers.size() == 1 && controllers.contains(clusterServices.getMyAddress()));
    }
}
