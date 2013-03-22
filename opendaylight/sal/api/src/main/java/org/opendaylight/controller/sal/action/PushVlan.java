
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
import org.opendaylight.controller.sal.utils.EtherTypes;

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

public class PushVlan extends Action {
    private int tag; // TPID - 16 bits
    private int pcp; // PCP - 3 bits
    private int cfi; // CFI - 1 bit (drop eligible)
    private int vlanId; // VID - 12 bits
    private transient int tci; // TCI = [PCP + CFI + VID] - 16 bits
    private transient int header; // full 802.1q header [TPID + TCI] - 32 bits

    /* Dummy constructor for JAXB */
    private PushVlan () {
    }

    public PushVlan(int tag, int pcp, int cfi, int vlanId) {
        type = ActionType.PUSH_VLAN;
        this.tag = tag;
        this.cfi = cfi;
        this.pcp = pcp;
        this.vlanId = vlanId;
        this.tci = createTci();
        this.header = createHeader();
        runChecks();
    }

    public PushVlan(EtherTypes tag, int pcp, int cfi, int vlanId) {
        type = ActionType.PUSH_VLAN;
        this.tag = tag.intValue();
        this.cfi = cfi;
        this.pcp = pcp;
        this.vlanId = vlanId;
        this.tci = createTci();
        this.header = createHeader();
        runChecks();
    }

    private int createTci() {
        return (pcp & 0x7) << 13 | (cfi & 0x1) << 12 | (vlanId & 0xfff);
    }

    private int createHeader() {
        return (tag & 0xffff) << 16 | (pcp & 0x7) << 13 | (cfi & 0x1) << 12
                | (vlanId & 0xfff);
    }

    private void runChecks() {
        checkValue(ActionType.SET_DL_TYPE, tag);
        checkValue(ActionType.SET_VLAN_PCP, pcp);
        checkValue(ActionType.SET_VLAN_CFI, cfi);
        checkValue(ActionType.SET_VLAN_ID, vlanId);
        checkValue(tci);

        // Run action specific check which cannot be run by parent
        if (tag != EtherTypes.VLANTAGGED.intValue()
                && tag != EtherTypes.QINQ.intValue()
                && tag != EtherTypes.OLDQINQ.intValue()
                && tag != EtherTypes.CISCOQINQ.intValue()) {
            // pass a value which will tell fail and tell something about the original wrong value
            checkValue(ActionType.SET_DL_TYPE, 0xBAD << 16 | tag);
        }
    }

    /**
     * Returns the VID portion of the 802.1q header this action will insert
     * VID - (12 bits)
     * @return byte[]
     */
    public int getVlanId() {
        return vlanId;
    }

    /**
     * Returns the CFI portion of the 802.1q header this action will insert
     * CFI - (1 bit)
     * @return
     */
    public int getCfi() {
        return cfi;
    }

    /**
     * Returns the vlan PCP portion of the 802.1q header this action will insert
     * PCP - (3 bits)
     * @return byte[]
     */
    public int getPcp() {
        return pcp;
    }

    /**
     * Returns the TPID portion of the 802.1q header this action will insert
     * TPID - (16 bits)
     */
    public int getTag() {
        return tag;
    }

    /**
     * Returns the TCI portion of the 802.1q header this action will insert
     * TCI = [PCP + CFI + VID] - (16 bits)
     * @return
     */
    public int getTci() {
        return tci;
    }

    /**
     * Returns the full 802.1q header this action will insert
     * header = [TPID + TIC] (32 bits)
     *
     * @return int
     */
    @XmlElement(name="VlanHeader")
    public int getHeader() {
        return header;
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
        return type + "[tag = " + tag + ", pcp = " + pcp + ", cfi = " + cfi
                + ", vlanId = " + vlanId + "]";
    }

}
