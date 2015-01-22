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

/**
 * Set destination transport port action
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@Deprecated
public class SetTpDst extends Action {
    private static final long serialVersionUID = 1L;
    @XmlElement
    private int port;

    /* Dummy constructor for JAXB */
    @SuppressWarnings("unused")
    private SetTpDst() {
    }

    public SetTpDst(int port) {
        type = ActionType.SET_TP_DST;
        this.port = port;
        checkValue(port);
    }

    /**
     * Returns the transport port the action will set
     *
     * @return
     */
    public int getPort() {
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
        SetTpDst other = (SetTpDst) obj;
        if (port != other.port) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + port;
        return result;
    }

    @Override
    public String toString() {
        return type + "[port = " + port + "]";
    }
}