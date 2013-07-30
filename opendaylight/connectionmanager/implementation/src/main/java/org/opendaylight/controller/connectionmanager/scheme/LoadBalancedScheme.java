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
