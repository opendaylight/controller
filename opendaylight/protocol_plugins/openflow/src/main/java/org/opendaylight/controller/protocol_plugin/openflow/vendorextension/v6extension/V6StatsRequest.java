
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.protocol_plugin.openflow.vendorextension.v6extension;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.openflow.protocol.statistics.OFVendorStatistics;
import java.nio.ByteBuffer;


/**
 * This Class creates the OpenFlow Vendor Extension IPv6 Flow Stats Request 
 * messages and also reads the Reply of a stats request message.
 * 
 */

public class V6StatsRequest extends OFVendorStatistics {
    private static final long serialVersionUID = 1L;
    protected int msgsubtype;
    protected short outPort;
    protected short match_len;
    protected byte tableId;

    public static final int NICIRA_VENDOR_ID = 0x00002320; //Nicira ID
    private static final int NXST_FLOW = 0x0; //Nicira Flow Stats Request Id

    public V6StatsRequest() {
        this.vendor = NICIRA_VENDOR_ID;
        this.msgsubtype = NXST_FLOW;
        this.match_len = 0;
    }

    /**
     * @param None. Being set with local variable (TBD).
     */
    public void setVendorId() {
        this.vendor = NICIRA_VENDOR_ID;
    }

    /**
     * @return vendor id
     */
    public int getVendorId() {
        return vendor;
    }

    /**
     * @param None. Being set with local variable (TBD).
     */
    public void setMsgtype() {
        this.msgsubtype = NXST_FLOW;
    }

    /**
     * @return vendor_msgtype
     */
    public int getMsgtype() {
        return msgsubtype;
    }

    /**
     * @param outPort the outPort to set
     */
    public void setOutPort(short outPort) {
        this.outPort = outPort;
    }

    /**
     * @return the outPort
     */
    public short getOutPort() {
        return outPort;
    }

    /**
     * @param match_len the match_len to set
     */
    public void setMatchLen(short match_len) {
        this.match_len = match_len;
    }

    /**
     * @return the match_len
     */
    public short getMatchLen() {
        return match_len;
    }

    /**
     * @param tableId the tableId to set
     */
    public void setTableId(byte tableId) {
        this.tableId = tableId;
    }

    /**
     * @return the tableId
     */
    public byte getTableId() {
        return tableId;
    }

    @Override
    public int getLength() {
        return 20;// 4(vendor)+4(msgsubtype)+4(pad)+2(outPort)+2(match_len)+1(tableid)+3(pad)
    }

    @Override
    public void readFrom(ByteBuffer data) {
        this.vendor = data.getInt();
        this.msgsubtype = data.getInt();
        data.getInt();//pad 4 bytes
        this.outPort = data.getShort();
        this.match_len = data.getShort();
        this.tableId = data.get();
        for (int i = 0; i < 3; i++)
            data.get();//pad byte

    }

    @Override
    public void writeTo(ByteBuffer data) {
        data.putInt(this.vendor);
        data.putInt(this.msgsubtype);
        data.putInt((int) 0x0);//pad0
        data.putShort(this.outPort);
        data.putShort(this.match_len);
        data.put(this.tableId);
        for (int i = 0; i < 3; i++)
            data.put((byte) 0x0);//pad byte
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    @Override
    public String toString() {
        return "V6StatsRequest[" + ReflectionToStringBuilder.toString(this)
                + "]";
    }

    @Override
    public boolean equals(Object obj) {
        return EqualsBuilder.reflectionEquals(this, obj);
    }
}
