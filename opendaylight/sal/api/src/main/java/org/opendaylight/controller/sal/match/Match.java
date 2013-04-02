
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.match;

import java.io.Serializable;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.opendaylight.controller.sal.utils.EtherTypes;
import org.opendaylight.controller.sal.utils.IPProtocols;
import org.opendaylight.controller.sal.utils.NetUtils;

/**
 * Represents the generic match criteria for a network frame/packet/message
 * It contains a collection of individual field match
 *
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
public class Match implements Cloneable, Serializable {
	private static final long serialVersionUID = 1L;
	private static final Map<MatchType, MatchType> reversableMatches;
    static {
        Map<MatchType, MatchType> map = new HashMap<MatchType, MatchType>();
        map.put(MatchType.DL_SRC, MatchType.DL_DST);
        map.put(MatchType.DL_DST, MatchType.DL_SRC);
        map.put(MatchType.NW_SRC, MatchType.NW_DST);
        map.put(MatchType.NW_DST, MatchType.NW_SRC);
        map.put(MatchType.TP_SRC, MatchType.TP_DST);
        map.put(MatchType.TP_DST, MatchType.TP_SRC);
        reversableMatches = Collections.unmodifiableMap(map);
    }
    private Map<MatchType, MatchField> fields;
    private int matches; // concise way to tell which fields the match is set for (may remove if not needed)

    public Match() {
        fields = new HashMap<MatchType, MatchField>();
        matches = 0;
    }

    public Match(Match match) {
        fields = new HashMap<MatchType, MatchField>(match.fields);
        matches = match.matches;
    }

    /**
     * Generic setter for frame/packet/message's header fields against which to match
     * Note: For MAC addresses, please pass the cloned value to this function
     *
     * @param type		packet's header field type
     * @param value 	field's value to assign to the match
     * @param mask		field's bitmask to apply to the match (has to be of the same class type of value)
     */
    public void setField(MatchType type, Object value, Object mask) {
        MatchField field = new MatchField(type, value, mask);
        if (field.isValid()) {
            fields.put(type, field);
            matches |= type.getIndex();
        }
    }

    /**
     * Generic setter for frame/packet/message's header fields against which to match
     * Note: For MAC addresses, please pass the cloned value to this function
     *
     * @param type		packet's header field type
     * @param value 	field's value to assign to the match
     */
    public void setField(MatchType type, Object value) {
        MatchField field = new MatchField(type, value);
        if (field.isValid()) {
            fields.put(type, field);
            matches |= type.getIndex();
        }
    }

    /**
     * Generic setter for frame/packet/message's header field against which to match
     *
     * @param field the fields parameters as MAtchField object
     */
    public void setField(MatchField field) {
        if (field.isValid()) {
            fields.put(field.getType(), field);
            matches |= field.getType().getIndex();
        }
    }

    /**
     * Generic method to clear a field from the match
     */
    public void clearField(MatchType type) {
        fields.remove(type);
        matches &= ~type.getIndex();
    }

    /**
     * Generic getter for fields against which the match is programmed
     *
     * @param type	frame/packet/message's header field type
     * @return
     */
    public MatchField getField(MatchType type) {
        return fields.get(type);
    }

    /**
     * Returns the fields the match is set for in a bitmask fashion
     * Each bit represents a field the match is configured for
     *
     * @return the 32 bit long mask (Refer to {@code}org.opendaylight.controller.sal.match.MatchElement)
     */
    public int getMatches() {
        return matches;
    }

    /**
     * Returns the list of MatchType fields the match is set for
     *
     * @return List of individual MatchType fields. 
     */
    public List<MatchType> getMatchesList() {
        return new ArrayList<MatchType>(fields.keySet());
    }

    /**
     * Returns the list of MatchFields the match is set for
     *
     * @return List of individual MatchField values. 
     */
    @XmlElement(name="matchField")
    public List<MatchField> getMatchFields() {
    	return new ArrayList<MatchField>(fields.values());
    }
    
    /**
     * Returns whether this match is for an IPv6 flow
     */
    public boolean isIPv6() {
        return (isPresent(MatchType.DL_TYPE)
                && ((Short) getField(MatchType.DL_TYPE).getValue())
                        .equals(EtherTypes.IPv6.shortValue())
                || isPresent(MatchType.NW_PROTO)
                && ((Byte) getField(MatchType.NW_PROTO).getValue())
                        .equals(IPProtocols.IPV6ICMP.byteValue())
                || isPresent(MatchType.NW_SRC)
                && getField(MatchType.NW_SRC).getValue() instanceof Inet6Address || isPresent(MatchType.NW_DST)
                && getField(MatchType.NW_DST).getValue() instanceof Inet6Address);
    }

    /**
     * Returns whether this match is for an IPv4 flow
     */
    public boolean isIPv4() {
        return !isIPv6();
    }

    /**
     * Returns whether for the specified field type the match is to be considered "any"
     * Equivalent to say this match does not care about the value of the specified field
     *
     * @param type
     * @return
     */
    public boolean isAny(MatchType type) {
        //return ((fields.get(type) == null) || (fields.get(type).getBitMask() == 0L));
        return !fields.containsKey(type);
    }

    /**
     * Returns whether a match for the specified field type is configured
     *
     * @param type
     * @return
     */
    public boolean isPresent(MatchType type) {
        return (fields.get(type) != null);
    }

    @Override
    public Match clone() {
        Match cloned = null;
        try {
            cloned = (Match) super.clone();
            cloned.fields = new HashMap<MatchType, MatchField>();
            for (Entry<MatchType, MatchField> entry : this.fields.entrySet()) {
                cloned.fields.put(entry.getKey(), entry.getValue().clone());
            }
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
        return cloned;
    }

    /**
     * Returns a reversed version of this match
     * For example, in the reversed version the network source and destination
     * addresses will be exchanged. Non symmetric match field will not be
     * copied over into the reversed match version, like input port.
     *
     * @return
     */
    public Match reverse() {
        // Copy over all fields
        Match reverse = this.clone();

        // Flip symmetric fields
        for (Map.Entry<MatchType, MatchType> entry : Match.reversableMatches
                .entrySet()) {
            MatchType from = entry.getKey();
            MatchType to = entry.getValue();
            if (this.isPresent(from)) {
                reverse.setField(to, this.getField(from).getValue(), this
                        .getField(from).getMask());
            }
        }

        // Reset asymmetric fields
        reverse.clearField(MatchType.IN_PORT);

        return reverse;
    }

    /**
     * Check whether the current match conflicts with the passed filter match
     * This match conflicts with the filter if for at least a MatchType defined
     * in the filter match, the respective MatchFields differ or are not compatible
     *
     * For example, Let's suppose the filter has the following MatchFields:
     * DL_TYPE = 0x800
     * NW_DST =  172.20.30.110/24
     *
     * while this match has the following MatchFields:
     * NW_DST = 172.20.30.45/24
     * TP_DST = 80
     *
     * Then the function would return false as the two Match are not conflicting
     *
     * Note: the mask value is taken into account only for MatchType.NW_SRC and MatchType.NW_DST
     *
     * @param match the MAtch describing the filter
     * @return true if the match is conflicting with the filter, false otherwise
     */
    public boolean conflictWithFilter(Match filter) {
        // Iterate through the MatchType defined in the filter
        for (MatchType type : filter.getMatchesList()) {
            if (this.isAny(type)) {
                continue;
            }

            MatchField thisField = this.getField(type);
            MatchField filterField = filter.getField(type);

            switch (type) {
            case DL_SRC:
            case DL_DST:
                if (Arrays.equals((byte[]) thisField.getValue(),
                        (byte[]) filterField.getValue())) {
                    return false;
                }
                break;
            case NW_SRC:
            case NW_DST:
                InetAddress thisAddress = (InetAddress) thisField.getValue();
                InetAddress filterAddress = (InetAddress) filterField
                        .getValue();
                // Validity check
                if (thisAddress instanceof Inet4Address
                        && filterAddress instanceof Inet6Address
                        || thisAddress instanceof Inet6Address
                        && filterAddress instanceof Inet4Address) {
                    return true;
                }
                InetAddress thisMask = (InetAddress) filter.getField(type)
                        .getMask();
                InetAddress filterMask = (InetAddress) filter.getField(type)
                        .getMask();
                // thisAddress has to be in same subnet of filterAddress
                if (NetUtils.inetAddressConflict(thisAddress, filterAddress,
                        thisMask, filterMask)) {
                    return true;
                }
                break;
            default:
                if (!thisField.getValue().equals(filterField.getValue())) {
                    return true;
                }
            }
            //TODO: check v4 v6 incompatibility
        }
        return false;
    }

    /**
     * Merge the current Match fields with the fields of the filter Match
     * A check is first run to see if this Match is compatible with the
     * filter Match. If it is not, the merge is not attempted.
     *
     *
     * @param filter
     * @return
     */
    public Match mergeWithFilter(Match filter) {
        if (!this.conflictWithFilter(filter)) {
            /*
             * No conflict with the filter
             * We can copy over the fields which this match does not have
             */
            for (MatchType type : filter.getMatchesList()) {
                if (this.isAny(type)) {
                    this.setField(filter.getField(type).clone());
                }
            }
        }
        return this;
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    @Override
    public boolean equals(Object obj) {
        return EqualsBuilder.reflectionEquals(this, obj);
    }

    @Override
    public String toString() {
        return "Match[" + fields.values() + "]";
    }
}
