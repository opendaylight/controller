package org.opendaylight.controller.sal.utils;


import static org.opendaylight.controller.sal.utils.Arguments.*;

public class Vlan {

    private final int tag; // TPID - 16 bits
    private final int pcp; // PCP - 3 bits
    private final int cfi; // CFI - 1 bit (drop eligible)
    private final int vlanId; // VID - 12 bits
    private final transient int tci; // TCI = [PCP + CFI + VID] - 16 bits
    private final transient int header; // full 802.1q header [TPID + TCI] - 32 bits
    
    
    public Vlan(EtherType tag, int pcp, int cfi, int vlanId) {
        this(tag.intValue(),pcp,cfi,vlanId);
    }
    
    public Vlan(int tag, int pcp, int cfi, int vlanId) {
        argInRange(0, 0xffff, tag,"tag");
        argInRange(0, 0x1, cfi, "cfi");
        argInRange(0, 0x7, pcp, "pcp");
        argInRange(0, 0xfff,vlanId,"vlanId");
   
     // FIXME: This seems like a special case handling, think about better
     //  extensible solution
     // Action specific check which cannot be run by parent
        if (tag != EtherTypes.VLANTAGGED.intValue()
                && tag != EtherTypes.QINQ.intValue()
                && tag != EtherTypes.OLDQINQ.intValue()
                && tag != EtherTypes.CISCOQINQ.intValue()) {
            // pass a value which will tell fail and tell something about the original wrong value
            argInRange(0x0,0xffff, 0xBAD << 16 | tag,"tag");
        }
        
        this.tag = tag;
        this.cfi = cfi;
        this.pcp = pcp;
        this.vlanId = vlanId;
        this.tci = computeTci();
        this.header = computeHeader();
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
    
    private int computeTci() {
        return (pcp & 0x7) << 13 | (cfi & 0x1) << 12 | (vlanId & 0xfff);
    }

    private int computeHeader() {
        return (tag & 0xffff) << 16 | (pcp & 0x7) << 13 | (cfi & 0x1) << 12
                | (vlanId & 0xfff);
    }
    
    @Deprecated
    public int getHeader() {
        return header;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        Vlan other = (Vlan) obj;
        if (cfi != other.cfi)
            return false;
        if (pcp != other.pcp)
            return false;
        if (tag != other.tag)
            return false;
        if (vlanId != other.vlanId)
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + cfi;
        result = prime * result + pcp;
        result = prime * result + tag;
        result = prime * result + vlanId;
        return result;
    }

    @Override
    public String toString() {
        return "Vlan [tag=" + tag + ", pcp=" + pcp + ", cfi=" + cfi
                + ", vlanId=" + vlanId + "]";
    }

}
