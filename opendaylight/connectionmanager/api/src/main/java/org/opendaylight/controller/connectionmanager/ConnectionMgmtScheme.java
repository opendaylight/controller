/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.connectionmanager;

/**
 * Enumeration that represents the Connectivity Scheme / Algorithm for South-Bound nodes
 * towards an Active-Active Clustered Controllers.
 */
public enum ConnectionMgmtScheme {
    /**
     * All the nodes are connected with a Single Controller.
     * The SingleControllerScheme algorithm will determine that one controller to which all
     * the nodes are connected with.
     * This is like Active-Standby model from a South-Bound perspective.
     */
    SINGLE_CONTROLLER("All nodes connected with a Single Controller"),

    /**
     * Any node can be connected with any controller. But with just 1 master controller.
     */
    ANY_CONTROLLER_ONE_MASTER("Nodes can to connect with any controller in the cluster"),

    /**
     * Simple Round Robin Scheme that will let the nodes connect with each controller in
     * Active-Active cluster in a round robin fashion.
     */
    ROUND_ROBIN("Each node is connected with individual Controller in Round-Robin fashion"),

    /**
     * Complex Load Balancing scheme that will let the nodes connect with controller based
     * on the resource usage in each of the controllers in a cluster.
     */
    LOAD_BALANCED("Connect nodes to controllers based on the Controller Load"),

    /**
     * Container based scheme will let the nodes connect with controller based
     * on the container configuration.
     */
    CONTAINER_BASED("Connect nodes to controllers based on Container they belong to");

    private ConnectionMgmtScheme(String name) {
        this.name = name;
    }

    private String name;

    public String toString() {
        return name;
    }

    public static ConnectionMgmtScheme fromString(String pName) {
        for(ConnectionMgmtScheme p:ConnectionMgmtScheme.values()) {
            if (p.toString().equals(pName)) {
                return p;
            }
        }
        return null;
    }
}
