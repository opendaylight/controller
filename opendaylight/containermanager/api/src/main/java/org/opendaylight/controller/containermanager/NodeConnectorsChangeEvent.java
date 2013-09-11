package org.opendaylight.controller.containermanager;

import java.io.Serializable;
import java.util.List;

import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.core.UpdateType;

/**
 * Class that represent the event of a configuration change for a container
 */
public class NodeConnectorsChangeEvent implements Serializable {
    private static final long serialVersionUID = 1L;
    private List<NodeConnector> ncList;
    private UpdateType updateType;

    public NodeConnectorsChangeEvent(List<NodeConnector> ncList, UpdateType updateType) {
        this.ncList = ncList;
        this.updateType = updateType;
    }

    public List<NodeConnector> getNodeConnectors() {
        return ncList;
    }

    public UpdateType getUpdateType() {
        return updateType;
    }

    @Override
    public String toString() {
        return "ContainerConnectorsChangeEvent [ncList: " + ncList + " updateType: " + updateType + "]";
    }
}
