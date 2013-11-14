package org.opendaylight.controller.sal.match;

import org.opendaylight.controller.sal.utils.HexEncode;
import org.opendaylight.controller.sal.utils.NetUtils;

public class DlType extends MatchField2 {
    private static final long serialVersionUID = 1L;
    public static final String TYPE = "DL_TYPE";
    private short ethertype;

    /**
     * Creates a Match2 field for the data layer type
     *
     * @param address
     *            the data layer type
     */
    public DlType(short ethertype) {
        super(TYPE);
        this.ethertype = ethertype;
    }

    @Override
    public Object getValue() {
        return ethertype;
    }

    @Override
    protected String getValueString() {
        return HexEncode.longToHexString(NetUtils.getUnsignedShort(ethertype));
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
        return true;
    }

    @Override
    public boolean hasReverse() {
        return false;
    }

    @Override
    public MatchField2 getReverse() {
        return this.clone();
    }

    @Override
    public MatchField2 clone() {
        return new DlType(ethertype);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ethertype;
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
        if (!(obj instanceof DlType)) {
            return false;
        }
        DlType other = (DlType) obj;
        if (ethertype != other.ethertype) {
            return false;
        }
        return true;
    }
}