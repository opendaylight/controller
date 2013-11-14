package org.opendaylight.controller.sal.match;

import org.opendaylight.controller.sal.utils.HexEncode;
import org.opendaylight.controller.sal.utils.NetUtils;

public class DlVlan extends MatchField2 {
    private static final long serialVersionUID = 1L;
    public static final String TYPE = "DL_VLAN";
    private static final short MAX = 4095;
    private short vlan;

    /**
     * Creates a Match2 field for the data layer type
     *
     * @param address
     *            the data layer type
     */
    public DlVlan(short vlan) {
        super(TYPE);
        this.vlan = vlan;
    }

    @Override
    public Object getValue() {
        return vlan;
    }

    @Override
    protected String getValueString() {
        return HexEncode.longToHexString(NetUtils.getUnsignedShort(vlan));
    }

    @Override
    public Object getMask() {
        return null;
    }

    @Override
    protected String getMaskString() {
        return null;
    }

    @Override
    public boolean isValid() {
        return vlan >= 0 && vlan <= MAX;
    }

    @Override
    public MatchField2 getReverse() {
        return this.clone();
    }

    @Override
    public boolean hasReverse() {
        return false;
    }

    @Override
    public MatchField2 clone() {
        return new DlVlan(vlan);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + vlan;
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
        if (!(obj instanceof DlVlan)) {
            return false;
        }
        DlVlan other = (DlVlan) obj;
        if (vlan != other.vlan) {
            return false;
        }
        return true;
    }
}