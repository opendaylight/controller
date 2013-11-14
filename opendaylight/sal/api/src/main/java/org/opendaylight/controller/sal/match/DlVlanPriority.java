package org.opendaylight.controller.sal.match;

import org.opendaylight.controller.sal.utils.NetUtils;

public class DlVlanPriority extends MatchField2<Short> {
    private static final long serialVersionUID = 1L;
    public static final String TYPE = "DL_VLAN_PR";
    private static final short MAX = 7;
    private short vlanPriority;

    /**
     * Creates a Match2 field for the data layer type
     *
     * @param address
     *            the data layer type
     */
    public DlVlanPriority(short vlanPriority) {
        super(TYPE);
        this.vlanPriority = vlanPriority;
    }

    @Override
    public Short getValue() {
        return vlanPriority;
    }

    @Override
    protected String getValueString() {
        return String.format("0X%s", Integer.toHexString(NetUtils.getUnsignedShort(vlanPriority)));
    }

    @Override
    public Short getMask() {
        return null;
    }

    @Override
    protected String getMaskString() {
        return null;
    }

    @Override
    public boolean isValid() {
        return vlanPriority >= 0 && vlanPriority <= MAX;
    }

    @Override
    public boolean hasReverse() {
        return false;
    }

    @Override
    public DlVlanPriority getReverse() {
        return this.clone();
    }

    @Override
    public DlVlanPriority clone() {
        return new DlVlanPriority(vlanPriority);
    }

    @Override
    public boolean isV6() {
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + vlanPriority;
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
        if (!(obj instanceof DlVlanPriority)) {
            return false;
        }
        DlVlanPriority other = (DlVlanPriority) obj;
        if (vlanPriority != other.vlanPriority) {
            return false;
        }
        return true;
    }
}