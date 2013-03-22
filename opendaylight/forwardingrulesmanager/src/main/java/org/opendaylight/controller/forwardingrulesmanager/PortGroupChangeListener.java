
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.forwardingrulesmanager;

import java.util.Map;

import org.opendaylight.controller.sal.core.Node;

/**
 * PortGroupChangeListener listens to the PortGroup updates provided by the PortGroupProvider.
 *
 *
 */
public interface PortGroupChangeListener {
    /**
     * This method is invoked by PortGroupProvider whenever it detects a change in PortGroup
     * membership for a given PortGroupConfig.
     *
     * @param config Port Group Configuration
     * @param portGroupData HashMap of Node id to PortGroup that represents the updated ports as detected by PortGroupProvider.
     * @param add true indicates that the PortGroup is added. False indicates that the PortGroup is removed.
     */
    void portGroupChanged(PortGroupConfig config,
            Map<Node, PortGroup> portGroupData, boolean add);
}
