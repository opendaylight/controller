/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.rest.action;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Set source transport port action
 * 
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
public class SetTpSrc extends ActionRTO {

    private Integer port;

    @SuppressWarnings("unused")
    private SetTpSrc() {
    }

    public SetTpSrc(Integer port) {
        this.port = port;
    }

    /**
     * Returns the transport port the action will set
     * 
     * @return
     */
    @XmlElement
    public int getPort() {
        return port;
    }

    @Override
    public String toString() {
        return "setTpSrc" + "[port = " + getPort() + "]";
    }
}
