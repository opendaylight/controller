/**
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.messagebus.app.impl;

/**
 * Created by rgallas on 12/10/2014.
 */
public enum TopologyException {
    NETWORK_TOPOLOGY_CREATION_FAILED("Failed to create NetworkTopology root node."),
    TOPOLOGY_CREATION_FAILED("Failed to create Topology node"),
    TOPOLOGY_TYPE_CREATION_FAILED("Failed to create Topology Type");

    private final String msg;

    TopologyException(String msg) {
        this.msg = msg;
    }

    public void raise() {
        throw new RuntimeException(this.msg);
    }

    public void raise(String message) {
        throw new RuntimeException(formatMessage(message));
    }

    public void raise(Exception e) {
        throw new RuntimeException(this.msg, e);
    }

    public void raise(String message, Exception e) {
        throw new RuntimeException(formatMessage(message), e);
    }

    private String formatMessage(String message) {
        return this.msg + " : " + message;
    }
}
