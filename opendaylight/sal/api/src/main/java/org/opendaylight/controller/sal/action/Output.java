
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

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.opendaylight.controller.sal.core.NodeConnector;

/**
 * Represents the action of sending the packet out of a physical port
 *
 *
 *
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)

public class Output extends Action {
	@XmlElement
    private NodeConnector port;

    /* Dummy constructor for JAXB */
    private Output () {
    }

    public Output(NodeConnector port) {
        type = ActionType.OUTPUT;
        this.port = port;
        //checkValue(port);
    }

    public NodeConnector getPort() {
        return port;
    }

    @Override
    public boolean equals(Object other) {
        return EqualsBuilder.reflectionEquals(this, other);
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    @Override
    public String toString() {
        return type + "[" + port.toString() + "]";
    }
}
