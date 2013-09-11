
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

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
