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
 * Set vlan PCP action
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
public class SetVlanPcp extends ActionRTO {

    private int pcp;

    @SuppressWarnings("unused")
    private SetVlanPcp() {
    }
    
    public SetVlanPcp(int pcp) {
        this.pcp = (pcp);
    }

    /**
     * Returns the value of the vlan PCP this action will set
     * 
     * @return int
     */
    @XmlElement
    public int getPcp() {
        return pcp;
    }

    @Override
    public String toString() {
        return "setVlanPcp" + "[pcp = " + Integer.toHexString(pcp) + "]";
    }
}
