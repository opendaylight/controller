
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.samples.simpleforwarding.internal;

import java.io.Serializable;

import org.opendaylight.controller.hosttracker.hostAware.HostNodeConnector;
import org.opendaylight.controller.sal.core.Node;

/**
 * Class that represent a pair of {Host, Node}, the intent of it
 * is to be used as a key in the database kept by IPSwitching module
 * where for every Host, Switch we will have a Forwarding Rule that
 * will route the traffic toward the /32 destination
 *
 */
public class HostNodePair implements Serializable {
    private static final long serialVersionUID = 1L;
    private HostNodeConnector host;
    private Node node;

    public HostNodePair(HostNodeConnector h, Node s) {
        setNode(s);
        setHost(h);
    }

    public Node getNode() {
        return node;
    }

    public void setNode(Node nodeId) {
        this.node = nodeId;
    }

    public HostNodeConnector getHost() {
        return host;
    }

    public void setHost(HostNodeConnector host) {
        this.host = host;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((host == null) ? 0 : host.hashCode());
        result = prime * result + ((node == null) ? 0 : node.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        HostNodePair other = (HostNodePair) obj;
        if (host == null) {
            if (other.host != null)
                return false;
        } else if (!host.equals(other.host))
            return false;
        if (node == null) {
            if (other.node != null)
                return false;
        } else if (!node.equals(other.node))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "HostNodePair [host=" + host + ", node=" + node + "]";
    }
}
