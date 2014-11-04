/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.connection;

public enum ConnectionLocality {
    /**
     * This controller is the (or one of the) master for a given node
     */
    LOCAL("This controller is the (or one of the) master for a given node"),

    /**
     * This controller is not the master for a given node
     */
    NOT_LOCAL("This controller is not the master for a given node"),

    /**
     * The given node is not connected to any of the controllers in the cluster
     */
    NOT_CONNECTED("The given node is not connected to any of the controllers in the cluster");

    private ConnectionLocality(String description) {
        this.description = description;
    }

    private String description;

    public String toString() {
        return description;
    }
}
