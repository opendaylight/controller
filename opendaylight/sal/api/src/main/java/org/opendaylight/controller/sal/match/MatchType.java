
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.match;

import java.net.InetAddress;

import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.utils.HexEncode;
import org.opendaylight.controller.sal.utils.NetUtils;

/**
 * Represents the binding between the id, the value and mask type and the range values
 * of the elements type that can be matched on the network frame/packet/message
 *
 *
 *
 */
public enum MatchType {
    IN_PORT("inPort", 1 << 0, NodeConnector.class, 1, 0), 
    DL_SRC("dlSrc", 1 << 1, Byte[].class, 0, 0xffffffffffffL), 
    DL_DST("dlDst", 1 << 2, Byte[].class, 0, 0xffffffffffffL), 
    DL_VLAN("dlVlan", 1 << 3, Short.class, 1, 0xfff), // 2 bytes
    DL_VLAN_PR("dlVlanPriority", 1 << 4, Byte.class, 0, 0x7), // 3 bits
    DL_OUTER_VLAN("dlOuterVlan", 1 << 5, Short.class, 1, 0xfff), 
    DL_OUTER_VLAN_PR("dlOuterVlanPriority", 1 << 6, Short.class, 0, 0x7), 
    DL_TYPE("dlType", 1 << 7, Short.class, 0, 0xffff), // 2 bytes
    NW_TOS("nwTOS", 1 << 8, Byte.class, 0, 0x3f), // 6 bits (DSCP field)
    NW_PROTO("nwProto", 1 << 9, Byte.class, 0, 0xff), // 1 byte
    NW_SRC("nwSrc", 1 << 10, InetAddress.class, 0, 0), 
    NW_DST("nwDst", 1 << 11, InetAddress.class, 0, 0), 
    TP_SRC("tpSrc", 1 << 12, Short.class, 1, 0xffff), // 2 bytes
    TP_DST("tpDst", 1 << 13, Short.class, 1, 0xffff); // 2 bytes

    private String id;
    private int index;
    private Class<?> dataType;
    private long minValue;
    private long maxValue;

    private MatchType(String id, int index, Class<?> dataType, long minValue,
            long maxValue) {
        this.id = id;
        this.index = index;
        this.dataType = dataType;
        this.minValue = minValue;
        this.maxValue = maxValue;
    }

    public String id() {
        return id;
    }

    public int getIndex() {
        return index;
    }

    public Class<?> dataType() {
        return dataType;
    }

    public String getRange() {
        return "[0x" + Long.toHexString(minValue) + "-0x"
                + Long.toHexString(maxValue) + "]";
    }

    /**
     *  Perform the assignment type validation
     * @param value
     * @param mask
     * @return
     */
    public boolean isCongruentType(Object value, Object mask) {
        // Mask type has to match value type
        if (mask != null && (mask.getClass() != value.getClass())) {
            return false;
        }

        Class<?> e = this.dataType();
        Class<?> g = value.getClass();

        // This is all what we need, if value type is same of match required type
        if (g.equals(e)) {
            return true;
        }

        // This is for the numeric class vs primitive congruence
        // For what concerns here, for instance, Integer is congruent to int
        if (e == Short.class) {
            return g.equals(short.class);
        }

        if (e == Integer.class) {
            return g.equals(int.class);
        }

        if (e == Byte.class) {
            return g.equals(byte.class);
        }

        if (e == Byte[].class) {
            return g.equals(byte[].class);
        }

        if (e == InetAddress.class) {
            return g.getSuperclass().equals(InetAddress.class);
        }
        return false;
    }

    /**
     * Perform the value and mask range validation
     * @param value
     * @param mask
     * @return
     */
    public boolean isValid(Object value, Object mask) {
        // Skip if main error
        if (mask != null && (mask.getClass() != value.getClass())) {
            return false;
        }
        // For the complex below types let's skip the value range check for now
        if (this.dataType == InetAddress.class) {
            return true;
        }
        if (this.dataType() == Byte[].class) {
            return true;
        }
        if (this.dataType() == NodeConnector.class) {
            return true;
        }

        int val = 0;
        int msk = 0;
        if (value.getClass() == Integer.class || value.getClass() == int.class) {
            val = ((Integer) value).intValue();
            msk = (mask != null) ? ((Integer) mask).intValue() : 0;

        } else if (value.getClass() == Short.class
                || value.getClass() == short.class) {
            val = ((Short) value).intValue() & 0xffff;
            msk = (mask != null) ? ((Short) mask).intValue() & 0xffff : 0;

        } else if (value.getClass() == Byte.class
                || value.getClass() == byte.class) {
            val = ((Byte) value).intValue() & 0xff;
            msk = (mask != null) ? ((Byte) mask).intValue() & 0xff : 0;
        }
        return ((val >= minValue && val <= maxValue) && (mask == null || (msk >= minValue && msk <= maxValue)));

    }

    /**
     * Return the mask value in 64 bits bitmask form
     * @param mask
     * @return
     */
    public long getBitMask(Object mask) {
        if (this.dataType == InetAddress.class) {
            //TODO handle Inet v4 and v6 v6 will have a second upper mask
            return 0;
        }
        if (this.dataType() == Byte[].class) {
            if (mask == null) {
                return 0xffffffffffffL;
            }
            byte mac[] = (byte[]) mask;
            long bitmask = 0;
            for (short i = 0; i < 6; i++) {
                //				bitmask |= (((long)mac[i] & 0xffL) << (long)((5-i)*8));
                bitmask |= (((long) mac[i] & 0xffL) << ((5 - i) * 8));
            }
            return bitmask;
        }
        if (this.dataType == Integer.class || this.dataType == int.class) {
            return (mask == null) ? this.maxValue : ((Integer) mask)
                    .longValue();

        }
        if (this.dataType == Short.class || this.dataType == short.class) {
            return (mask == null) ? this.maxValue : ((Short) mask).longValue();
        }
        if (this.dataType == Byte.class || this.dataType == byte.class) {
            return (mask == null) ? this.maxValue : ((Byte) mask).longValue();

        }
        return 0L;
    }

	public String stringify(Object value) {
		if (value == null) {
			return null;
		}
		
		switch (this) {
		case DL_DST:
		case DL_SRC:
			return HexEncode.bytesToHexStringFormat((byte[])value);
		case DL_TYPE:
		case DL_VLAN:
			return ((Integer) NetUtils.getUnsignedShort((Short)value))
					.toString();
		case NW_SRC:
		case NW_DST:
			return ((InetAddress)value).getHostAddress();
		case NW_TOS:
			return ((Integer) NetUtils.getUnsignedByte((Byte)value))
					.toString();
		case TP_SRC:
		case TP_DST:
			return ((Integer) NetUtils.getUnsignedShort((Short)value))
					.toString();
		default:
			break;
		}
		return value.toString();
	}
}
