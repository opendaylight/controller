package org.openflow.protocol;

public enum OFPort {
    OFPP_MAX                ((short)0xff00),
    OFPP_IN_PORT            ((short)0xfff8),
    OFPP_TABLE              ((short)0xfff9),
    OFPP_NORMAL             ((short)0xfffa),
    OFPP_FLOOD              ((short)0xfffb),
    OFPP_ALL                ((short)0xfffc),
    OFPP_CONTROLLER         ((short)0xfffd),
    OFPP_LOCAL              ((short)0xfffe),
    OFPP_NONE               ((short)0xffff);

    protected short value;

    private OFPort(short value) {
        this.value = value;
    }

    /**
     * @return the value
     */
    public short getValue() {
        return value;
    }
}
