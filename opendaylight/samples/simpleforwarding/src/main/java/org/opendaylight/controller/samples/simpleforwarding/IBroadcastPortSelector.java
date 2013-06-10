package org.opendaylight.controller.samples.simpleforwarding;

import java.util.Set;

import org.opendaylight.controller.sal.core.NodeConnector;

public interface IBroadcastPortSelector {
    public Set<NodeConnector> getBroadcastPorts();
}
