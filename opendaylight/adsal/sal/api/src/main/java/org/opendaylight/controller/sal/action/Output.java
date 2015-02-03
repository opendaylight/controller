/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.action;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.opendaylight.controller.sal.core.NodeConnector;

/**
 * Represents the action of sending the packet out of a physical port
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@Deprecated
public class Output extends Action {
    private static final long serialVersionUID = 1L;
    @XmlElement
    private NodeConnector port;

    /* Dummy constructor for JAXB */
    @SuppressWarnings("unused")
    private Output() {
    }

    public Output(NodeConnector port) {
        type = ActionType.OUTPUT;
        this.port = port;
        // checkValue(port);
    }

    public NodeConnector getPort() {
        return port;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        Output other = (Output) obj;
        if (port == null) {
            if (other.port != null) {
                return false;
            }
        } else if (!port.equals(other.port)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((port == null) ? 0 : port.hashCode());
        return result;
    }

    @Override
    public String toString() {
        return type + "[" + port.toString() + "]";
    }
}
