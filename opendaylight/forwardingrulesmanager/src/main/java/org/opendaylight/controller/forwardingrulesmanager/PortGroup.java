
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.forwardingrulesmanager;

import java.util.HashSet;
import java.util.Set;

/**
 * PortGroup is a simple data-structure to represent any arbitrary group of ports
 * on a Switch (that is represented using its switch-ID).
 *
 * PortGroup is used by PortGroupProvider application to signal a set of ports that
 * represent a configured PortGroupConfig.
 *
 *
 */
public class PortGroup {
    private long matrixSwitchId;
    private Set<Short> ports;

    /**
     * PortGroup Constructor using Switch and Ports.
     *
     * @param matrixSwitchId Switch Id that represents an openflow Switch
     * @param ports Set of short values representing openflow port-ids.
     */
    public PortGroup(long matrixSwitchId, Set<Short> ports) {
        super();
        this.matrixSwitchId = matrixSwitchId;
        this.ports = ports;
    }

    /**
     * PortGroup Constructor using Switch.
     *
     * @param matrixSwitchId Switch-Id that represents an openflow Switch
     */
    public PortGroup(long matrixSwitchId) {
        this.matrixSwitchId = matrixSwitchId;
        this.ports = new HashSet<Short>();
    }

    /**
     * Returns the switchId representing the Switch that makes this PortGroup.
     *
     * @return long switchId
     */
    public long getMatrixSwitchId() {
        return matrixSwitchId;
    }

    /**
     * Assigns a Switch to this PortGroup
     *
     * @param matrixSwitchId Switch-Id that represents an openflow Switch
     */
    public void setMatrixSwitchId(long matrixSwitchId) {
        this.matrixSwitchId = matrixSwitchId;
    }

    /**
     * Returns the Set of Ports that makes this PortGroup.
     *
     * @return Set of short values representing openflow port-ids.
     */
    public Set<Short> getPorts() {
        return ports;
    }

    /**
     * Assigns a set of openflow ports to this PortGroup
     *
     * @param ports Set of short values representing openflow port-ids.
     */
    public void setPorts(Set<Short> ports) {
        this.ports = ports;
    }

    /**
     * Adds a port to this PortGroup
     *
     * @param port Short value of a openflow port.
     */
    public void addPort(short port) {
        ports.add(port);
    }

    @Override
    public String toString() {
        return "PortGroup [matrixSwitchId=" + matrixSwitchId + ", ports="
                + ports + "]";
    }
}
