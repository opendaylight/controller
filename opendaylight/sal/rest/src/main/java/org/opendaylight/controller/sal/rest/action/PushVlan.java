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

import org.opendaylight.controller.sal.utils.EtherType;
import org.opendaylight.controller.sal.utils.Vlan;

/**
 * Insert a 802.1q (outermost) header action Execute it multiple times to
 * achieve QinQ
 * 
 * 802.1q = [TPID(16) + TCI(16)] TCI = [PCP(3) + CFI(1) + VID(12)]
 * 
 * 
 * 
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
public class PushVlan extends ActionRTO {

    private int tag;
    private int cfi;
    private int vlanId;
    private int pcp;

    @SuppressWarnings("unused")
    private PushVlan() {
    }
    
    @Deprecated
    public PushVlan(int tag, int pcp, int cfi, int vlanId) {
        this.tag = tag;
        this.pcp = pcp;
        this.cfi = cfi;
        this.vlanId = vlanId;
    }

    @Deprecated
    public PushVlan(EtherType tag, int pcp, int cfi, int vlanId) {
        this.tag = tag.intValue();
        this.pcp = pcp;
        this.cfi = cfi;
        this.vlanId = vlanId;
    }

    public PushVlan(Vlan vlan) {
        this.tag = vlan.getTag();
        this.pcp = vlan.getPcp();
        this.cfi = vlan.getCfi();
        this.vlanId = vlan.getVlanId();
    }

    /**
     * Returns the VID portion of the 802.1q header this action will insert VID
     * - (12 bits)
     * 
     * @return byte[]
     */
    @Deprecated
    public int getVlanId() {
        return vlanId;
    }

    /**
     * Returns the CFI portion of the 802.1q header this action will insert CFI
     * - (1 bit)
     * 
     * @return
     */
    @Deprecated
    public int getCfi() {
        return cfi;
    }

    /**
     * Returns the vlan PCP portion of the 802.1q header this action will insert
     * PCP - (3 bits)
     * 
     * @return byte[]
     */
    @Deprecated
    public int getPcp() {
        return pcp;
    }

    /**
     * Returns the TPID portion of the 802.1q header this action will insert
     * TPID - (16 bits)
     */
    @Deprecated
    public int getTag() {
        return tag;
    }

    /**
     * Returns the TCI portion of the 802.1q header this action will insert TCI
     * = [PCP + CFI + VID] - (16 bits)
     * 
     * @return
     */
    @Deprecated
    public int getTci() {
        return computeTci();
    }

    /**
     * Returns the full 802.1q header this action will insert header = [TPID +
     * TIC] (32 bits)
     * 
     * @return int
     */
    @Deprecated
    @XmlElement(name = "VlanHeader")
    public int getHeader() {
        return computeHeader();
    }

    private int computeTci() {
        return (pcp & 0x7) << 13 | (cfi & 0x1) << 12 | (vlanId & 0xfff);
    }

    private int computeHeader() {
        return (tag & 0xffff) << 16 | (pcp & 0x7) << 13 | (cfi & 0x1) << 12
                | (vlanId & 0xfff);
    }

}
