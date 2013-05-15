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
import static org.opendaylight.controller.sal.utils.Arguments.*;

/**
 * Set network TOS action
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
public class SetNwTos extends ActionRTO {

    private Integer tos;

    @SuppressWarnings("unused")
    private SetNwTos() {
    }
    
    public SetNwTos(Integer tos) {
        this.tos = tos;
    }

    /**
     * Returns the network TOS value which the action will set
     * 
     * @return int
     */
    @XmlElement
    public int getTos() {
        return tos;
    }

    @Override
    public String toString() {
        return "setNwTos" + "[tos = 0x" + Integer.toHexString(tos) + "]";
    }
}
