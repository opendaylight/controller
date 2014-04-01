/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.action;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.utils.EtherTypes;

/**
 * Insert a 802.1q (outermost) header action Execute it multiple times to
 * achieve QinQ
 *
 * 802.1q = [TPID(16) + TCI(16)] TCI = [PCP(3) + CFI(1) + VID(12)]
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
public class PushVlan extends Action {
    private static final long serialVersionUID = 1L;
    public static final String NAME = "PUSH_VLAN";
    public static final Pattern PATTERN = Pattern.compile(NAME + "=\\((.*)\\)", Pattern.CASE_INSENSITIVE);
    private int tag; // TPID - 16 bits
    private int pcp; // PCP - 3 bits
    private int cfi; // CFI - 1 bit (drop eligible)
    private int vlanId; // VID - 12 bits
    private int tci; // TCI = [PCP + CFI + VID] - 16 bits
    private int header; // full 802.1q header [TPID + TCI] - 32 bits

    public PushVlan() {
        super(NAME);
    }

    public PushVlan(int tag, int pcp, int cfi, int vlanId) {
        super(NAME);
        this.tag = tag;
        this.cfi = cfi;
        this.pcp = pcp;
        this.vlanId = vlanId;
        this.tci = createTci();
        this.header = createHeader();
    }

    public PushVlan(EtherTypes tag, int pcp, int cfi, int vlanId) {
        super(NAME);
        this.tag = tag.intValue();
        this.cfi = cfi;
        this.pcp = pcp;
        this.vlanId = vlanId;
        this.tci = createTci();
        this.header = createHeader();
    }

    private int createTci() {
        return (pcp & 0x7) << 13 | (cfi & 0x1) << 12 | (vlanId & 0xfff);
    }

    private int createHeader() {
        return (tag & 0xffff) << 16 | (pcp & 0x7) << 13 | (cfi & 0x1) << 12 | (vlanId & 0xfff);
    }

    /**
     * Returns the VID portion of the 802.1q header this action will insert VID
     * - (12 bits)
     *
     * @return byte[]
     */
    public int getVlanId() {
        return vlanId;
    }

    /**
     * Returns the CFI portion of the 802.1q header this action will insert CFI
     * - (1 bit)
     *
     * @return
     */
    public int getCfi() {
        return cfi;
    }

    /**
     * Returns the vlan PCP portion of the 802.1q header this action will insert
     * PCP - (3 bits)
     *
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
     * Returns the TCI portion of the 802.1q header this action will insert TCI
     * = [PCP + CFI + VID] - (16 bits)
     *
     * @return
     */
    public int getTci() {
        return tci;
    }

    /**
     * Returns the full 802.1q header this action will insert header = [TPID +
     * TIC] (32 bits)
     *
     * @return int
     */
    @XmlElement(name = "VlanHeader")
    public int getHeader() {
        return header;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + cfi;
        result = prime * result + header;
        result = prime * result + pcp;
        result = prime * result + tag;
        result = prime * result + tci;
        result = prime * result + vlanId;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof PushVlan)) {
            return false;
        }
        PushVlan other = (PushVlan) obj;
        if (cfi != other.cfi) {
            return false;
        }
        if (header != other.header) {
            return false;
        }
        if (pcp != other.pcp) {
            return false;
        }
        if (tag != other.tag) {
            return false;
        }
        if (tci != other.tci) {
            return false;
        }
        if (vlanId != other.vlanId) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return String.format("%s=(0x%s:%s:%s:%s)", NAME, Integer.toHexString(tag), pcp, cfi, vlanId);
    }

    @Override
    public PushVlan fromString(String actionString, Node node) {
        Matcher matcher = PATTERN.matcher(removeSpaces(actionString));
        if (matcher.matches()) {
            String pieces[] = matcher.group(1).replace("(","").replace(")", "").split(":");
            if (pieces.length == 4) {
                try {
                    return new PushVlan(Integer.decode(pieces[0]), Integer.decode(pieces[1]),
                            Integer.decode(pieces[2]), Integer.decode(pieces[3]));
                } catch (NumberFormatException nfe) {
                    return null;
                }
            }
        }
        return null;
    }

    @Override
    public boolean isValid() {
        return (tag == EtherTypes.VLANTAGGED.intValue() || tag == EtherTypes.QINQ.intValue()
                || tag == EtherTypes.OLDQINQ.intValue() || tag == EtherTypes.CISCOQINQ.intValue())
                && pcp >= 0 && pcp <= 7 && cfi >= 0 && cfi <= 1 && vlanId >= 1 && vlanId <= 4095;
    }

}
