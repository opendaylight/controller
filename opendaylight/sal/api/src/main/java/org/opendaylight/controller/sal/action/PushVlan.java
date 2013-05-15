
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

import org.opendaylight.controller.sal.utils.EtherType;
import org.opendaylight.controller.sal.utils.Vlan;

/**
 * Insert a 802.1q (outermost) header action
 * Execute it multiple times to achieve QinQ
 *
 * 802.1q = [TPID(16) + TCI(16)]
 * 			TCI = [PCP(3) + CFI(1) + VID(12)]
 *
 *
 *
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)

public class PushVlan extends AbstractParameterAction<Vlan> {

    @Deprecated
    public PushVlan(int tag, int pcp, int cfi, int vlanId) {
        super(new Vlan(tag, pcp, cfi, vlanId));
        
    }

    @Deprecated
    public PushVlan(EtherType tag, int pcp, int cfi, int vlanId) {
        super(new Vlan(tag, pcp, cfi, vlanId));
    }

    public PushVlan(Vlan vlan){
        super(vlan);
    }
    


    /**
     * Returns the VID portion of the 802.1q header this action will insert
     * VID - (12 bits)
     * @return byte[]
     */
    @Deprecated
    public int getVlanId() {
        return getValue().getVlanId();
    }

    /**
     * Returns the CFI portion of the 802.1q header this action will insert
     * CFI - (1 bit)
     * @return
     */
    @Deprecated
    public int getCfi() {
        return getValue().getCfi();
    }

    /**
     * Returns the vlan PCP portion of the 802.1q header this action will insert
     * PCP - (3 bits)
     * @return byte[]
     */
    @Deprecated
    public int getPcp() {
        return getValue().getPcp();
    }

    /**
     * Returns the TPID portion of the 802.1q header this action will insert
     * TPID - (16 bits)
     */
    @Deprecated
    public int getTag() {
        return getValue().getTag();
    }

    /**
     * Returns the TCI portion of the 802.1q header this action will insert
     * TCI = [PCP + CFI + VID] - (16 bits)
     * @return
     */
    @Deprecated
    public int getTci() {
        return getValue().getTci();
    }

    /**
     * Returns the full 802.1q header this action will insert
     * header = [TPID + TIC] (32 bits)
     *
     * @return int
     */
    @Deprecated
    @XmlElement(name="VlanHeader")
    public int getHeader() {
        return getValue().getHeader();
    }

    @Override
    public String toString() {
        return "pushVlan" + "[vlan = " + getValue() + "]";
    }

}
