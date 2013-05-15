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

import org.opendaylight.controller.sal.utils.Arguments;

/**
 * Set vlan CFI action
 * 
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
public class SetVlanCfi extends ActionRTO {

    private Integer cfi;

    @SuppressWarnings("unused")
    private SetVlanCfi() {
    
    }
    
    public SetVlanCfi(Integer cfi) {
        this.cfi = cfi;
    }

    /**
     * Returns the 802.1q CFI value that this action will set
     * 
     * @return
     */
    @XmlElement
    public int getCfi() {
        return cfi;
    }

    @Override
    public String toString() {
        return "setVlanCfi" + "[cfi = " + Integer.toHexString(cfi) + "]";
    }
}
