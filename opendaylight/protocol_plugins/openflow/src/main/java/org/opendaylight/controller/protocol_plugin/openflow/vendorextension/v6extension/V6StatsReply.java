
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.protocol_plugin.openflow.vendorextension.v6extension;

import java.nio.ByteBuffer;
import java.util.List;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.openflow.protocol.action.OFAction;
import org.openflow.protocol.statistics.OFVendorStatistics;
import org.openflow.util.U16;

/**
 * This Class processes the OpenFlow Vendor Extension Reply message of a Stats
 * Request. It parses the reply message and initializes fields of  V6StatsReply
 * object. Multiple instances of this class objects are created and used by 
 * OpenDaylight's Troubleshooting Application.
 * 
 */

public class V6StatsReply extends OFVendorStatistics {
    private static final long serialVersionUID = 1L;

    public static int MINIMUM_LENGTH = 48; //48 for nx_flow_stats

    protected short length = (short) MINIMUM_LENGTH;
    protected byte tableId;
    protected int durationSeconds;
    protected int durationNanoseconds;
    protected short priority;
    protected short idleTimeout;
    protected short hardTimeout;
    protected short match_len;
    protected short idleAge;
    protected short hardAge;
    protected long cookie;
    protected long packetCount;
    protected long byteCount;
    protected V6Match match;
    protected List<OFAction> actions;

    /**
     * @return vendor id
     */
    public int getVendorId() {
        return vendor;
    }

    /**
     * @param vendor the vendor to set
     */
    public void setVendorId(int vendor) {
        this.vendor = vendor;
    }

    /**
     * @return the tableId
     */
    public byte getTableId() {
        return tableId;
    }

    /**
     * @param tableId the tableId to set
     */
    public void setTableId(byte tableId) {
        this.tableId = tableId;
    }

    /**
     * @return the durationSeconds
     */
    public int getDurationSeconds() {
        return durationSeconds;
    }

    /**
     * @param durationSeconds the durationSeconds to set
     */
    public void setDurationSeconds(int durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    /**
     * @return the durationNanoseconds
     */
    public int getDurationNanoseconds() {
        return durationNanoseconds;
    }

    /**
     * @param durationNanoseconds the durationNanoseconds to set
     */
    public void setDurationNanoseconds(int durationNanoseconds) {
        this.durationNanoseconds = durationNanoseconds;
    }

    /**
     * @return the priority
     */
    public short getPriority() {
        return priority;
    }

    /**
     * @param priority the priority to set
     */
    public void setPriority(short priority) {
        this.priority = priority;
    }

    /**
     * @return the idleTimeout
     */
    public short getIdleTimeout() {
        return idleTimeout;
    }

    /**
     * @param idleTimeout the idleTimeout to set
     */
    public void setIdleTimeout(short idleTimeout) {
        this.idleTimeout = idleTimeout;
    }

    /**
     * @return the hardTimeout
     */
    public short getHardTimeout() {
        return hardTimeout;
    }

    /**
     * @param hardTimeout the hardTimeout to set
     */
    public void setHardTimeout(short hardTimeout) {
        this.hardTimeout = hardTimeout;
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
     * @return the idleAge
     */
    public short getIdleAge() {
        return idleAge;
    }

    /**
     * @param idleAge the idleAge to set
     */
    public void setIdleAge(short idleAge) {
        this.idleAge = idleAge;
    }

    /**
     * @return the hardAge
     */
    public short getHardAge() {
        return hardAge;
    }

    /**
     * @param hardAge the hardAge to set
     */
    public void setHardAge(short hardAge) {
        this.hardAge = hardAge;
    }

    /**
     * @return the cookie
     */
    public long getCookie() {
        return cookie;
    }

    /**
     * @param cookie the cookie to set
     */
    public void setCookie(long cookie) {
        this.cookie = cookie;
    }

    /**
     * @return the packetCount
     */
    public long getPacketCount() {
        return packetCount;
    }

    /**
     * @param packetCount the packetCount to set
     */
    public void setPacketCount(long packetCount) {
        this.packetCount = packetCount;
    }

    /**
     * @return the byteCount
     */
    public long getByteCount() {
        return byteCount;
    }

    /**
     * @param byteCount the byteCount to set
     */
    public void setByteCount(long byteCount) {
        this.byteCount = byteCount;
    }

    /**
     * @param length the length to set
     */
    public void setLength(short length) {
        this.length = length;
    }

    @Override
    public int getLength() {
        return U16.f(length);
    }

    /**
     * @return the match
     */
    public V6Match getMatch() {
        return match;
    }

    /**
     * @return the actions
     */
    public List<OFAction> getActions() {
        return actions;
    }

    /**
     * @param actions the actions to set
     */
    public void setActions(List<OFAction> actions) {
        this.actions = actions;
    }

    @Override
    public void readFrom(ByteBuffer data) {
        short i;
        this.length = data.getShort();
        if (length < MINIMUM_LENGTH)
            return; //TBD - Spurious Packet?
        this.tableId = data.get();
        data.get(); // pad
        this.durationSeconds = data.getInt();
        this.durationNanoseconds = data.getInt();
        this.priority = data.getShort();
        this.idleTimeout = data.getShort();
        this.hardTimeout = data.getShort();
        this.match_len = data.getShort();
        this.idleAge = data.getShort();
        this.hardAge = data.getShort();
        this.cookie = data.getLong();
        this.packetCount = data.getLong();
        this.byteCount = data.getLong();
        if (this.length == MINIMUM_LENGTH) {
            return; //TBD - can this happen??
        }
        if (this.match == null)
            this.match = new V6Match();
        ByteBuffer mbuf = ByteBuffer.allocate(match_len);
        for (i = 0; i < match_len; i++) {
            mbuf.put(data.get());
        }
        mbuf.rewind();
        this.match.readFrom(mbuf);
        if (this.actionFactory == null)
            throw new RuntimeException("OFActionFactory not set");
        /*
         * action list may be preceded by a padding of 0 to 7 bytes based upon this:
         */
        short pad_size = (short) (((match_len + 7) / 8) * 8 - match_len);
        for (i = 0; i < pad_size; i++)
            data.get();
        int action_len = this.length - MINIMUM_LENGTH - (match_len + pad_size);
        if (action_len > 0)
            this.actions = this.actionFactory.parseActions(data, action_len);
    }

    @Override
    public void writeTo(ByteBuffer data) {
        super.writeTo(data);//TBD. This Fn needs work. Should never get called though.

    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    @Override
    public String toString() {
        return "V6StatsReply[" + ReflectionToStringBuilder.toString(this) + "]";
    }

    @Override
    public boolean equals(Object obj) {
        return EqualsBuilder.reflectionEquals(this, obj);
    }

}
