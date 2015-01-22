
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.match.extensible;

import java.io.Serializable;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;


import org.opendaylight.controller.sal.utils.NetUtils;

/**
 * Represents the generic match criteria for a network frame/packet/message
 * It contains a collection of individual field match
 *
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@Deprecated
public class Match implements Cloneable, Serializable {
        private static final long serialVersionUID = 1L;
    private Map<String, MatchField<?>> fields;

    public Match() {
        fields = new HashMap<String, MatchField<?>>();
    }

    public Match(Match match) {
        fields = new HashMap<String, MatchField<?>>(match.fields);
    }

    /**
     * Generic setter for frame/packet/message's header field against which to match
     *
     * @param field the fields parameters as MAtchField object
     */
    public void setField(MatchField<?> field) {
        if (field.isValid()) {
            fields.put(field.getType(), field);
        }
    }

    /**
     * Generic method to clear a field from the match
     */
    public void clearField(String type) {
        fields.remove(type);
    }

    /**
     * Generic getter for fields against which the match is programmed
     *
     * @param type  frame/packet/message's header field type
     * @return
     */
    public MatchField<?> getField(String type) {
        return fields.get(type);
    }

    /**
     * Returns the list of MatchType fields the match is set for
     *
     * @return List of individual MatchType fields.
     */
    public List<String> getMatchesList() {
        return new ArrayList<String>(fields.keySet());
    }

    /**
     * Returns the list of MatchFields the match is set for
     *
     * @return List of individual MatchField values.
     */
    @XmlElement(name="matchField")
    public List<MatchField<?>> getMatchFields() {
        return new ArrayList<MatchField<?>>(fields.values());
    }

    /**
     * Returns whether this match is for an IPv6 flow
     */
    public boolean isIPv6() {
        if (isPresent(DlType.TYPE)) {
            for (MatchField<?> field : fields.values()) {
                if (!field.isV6()) {
                    return false;
                }
            }
        }
        return true;
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
    public boolean isAny(String type) {
        return !fields.containsKey(type);
    }

    /**
     * Returns whether a match for the specified field type is configured
     *
     * @param type
     * @return
     */
    public boolean isPresent(String type) {
        return (fields.get(type) != null);
    }

    public boolean isEmpty() {
        return fields.isEmpty();
    }

    @Override
    public Match clone() {
        Match cloned = null;
        try {
            cloned = (Match) super.clone();
            cloned.fields = new HashMap<String, MatchField<?>>();
            for (Entry<String, MatchField<?>> entry : this.fields.entrySet()) {
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
        Match reverse = new Match();
        for (MatchField<?> field : fields.values()) {
            reverse.setField(field.hasReverse()? field.getReverse() : field.clone());
        }

        // Reset asymmetric fields
        reverse.clearField(InPort.TYPE);

        return reverse;
    }

    /**
     * Check whether the current match conflicts with the passed filter match
     * This match conflicts with the filter if for at least a MatchType defined
     * in the filter match, the respective MatchFields differ or are not
     * compatible
     *
     * In other words the function returns true if the set of packets described
     * by one match and the set of packets described by the other match are
     * disjoint. Equivalently, if the intersection of the two sets of packets
     * described by the two org.opendaylight.controller.sal.matches is an empty.
     *
     * For example, Let's suppose the filter has the following MatchFields:
     * DL_TYPE = 0x800
     * NW_DST = 172.20.30.110/24
     *
     * while this match has the following MatchFields:
     * DL_TYPE = 0x800
     * NW_DST = 172.20.30.45/24
     * TP_DST = 80
     *
     * Then the function would return false as the two Match are not
     * conflicting.
     *
     * Note: the mask value is taken into account only for MatchType.NW_SRC and
     * MatchType.NW_DST
     *
     * @param match
     *            the Match describing the filter
     * @return true if the set of packets described by one match and the set of
     *         packets described by the other match are disjoint, false
     *         otherwise
     */
    public boolean conflictWithFilter(Match filter) {
        return !this.intersetcs(filter);
    }

    /**
     * Merge the current Match fields with the fields of the filter Match. A
     * check is first run to see if this Match is compatible with the filter
     * Match. If it is not, the merge is not attempted.
     *
     * The result is the match object representing the intersection of the set
     * of packets described by this match with the set of packets described by
     * the filter match. If the intersection of the two sets is empty, the
     * return match will be null.
     *
     * @param filter
     *            the match with which attempting the merge
     * @return a new Match object describing the set of packets represented by
     *         the intersection of this and the filter org.opendaylight.controller.sal.matches. null if the
     *         intersection is empty.
     */
    public Match mergeWithFilter(Match filter) {
        return this.getIntersection(filter);
    }

    /**
     * Return the match representing the intersection of the set of packets
     * described by this match with the set of packets described by the other
     * match. Such as m.getIntersection(m) == m, m.getIntersection(u) == m and
     * m.getIntersection(o) == o where u is an empty match (universal set, all
     * packets) and o is the null match (empty set).
     *
     * @param other
     *            the match with which computing the intersection
     * @return a new Match object representing the intersection of the set of
     *         packets described by this match with the set of packets described
     *         by the other match. null when the intersection is the empty set.
     */
    public Match getIntersection(Match other) {
        // If no intersection, return the empty set
        if (!this.intersetcs(other)) {
            return null;
        }
        // Check if any of the two is the universal match
        if (this.isEmpty()) {
            return other.clone();
        }
        if (other.isEmpty()) {
            return this.clone();
        }
        // Get all the match types for both filters
        Set<String> allTypes = new HashSet<String>(this.fields.keySet());
        allTypes.addAll(new HashSet<String>(other.fields.keySet()));
        // Derive the intersection
        Match intersection = new Match();
        for (String type : allTypes) {
            if (this.isAny(type) && other.isAny(type)) {
                continue;
            }
            if (this.isAny(type)) {
                intersection.setField(other.getField(type).clone());
                continue;
            } else if (other.isAny(type)) {
                intersection.setField(this.getField(type).clone());
                continue;
            }
            // Either they are equal or it is about IP address
            switch (type) {
            // When it is about IP address, take the wider prefix address
            // between the twos
            case NwSrc.TYPE:
            case NwDst.TYPE:
                MatchField<?> thisField = this.getField(type);
                MatchField<?> otherField = other.getField(type);
                InetAddress thisAddress = (InetAddress) thisField.getValue();
                InetAddress otherAddress = (InetAddress) otherField.getValue();
                InetAddress thisMask = (InetAddress) thisField.getMask();
                InetAddress otherMask = (InetAddress) otherField.getMask();

                int thisMaskLen = (thisMask == null) ? ((thisAddress instanceof Inet4Address) ? 32 : 128) : NetUtils
                        .getSubnetMaskLength(thisMask);
                int otherMaskLen = (otherMask == null) ? ((otherAddress instanceof Inet4Address) ? 32 : 128) : NetUtils
                        .getSubnetMaskLength(otherMask);

                InetAddress subnetPrefix = null;
                InetAddress subnetMask = null;
                if (thisMaskLen < otherMaskLen) {
                    subnetPrefix = NetUtils.getSubnetPrefix(otherAddress, otherMaskLen);
                    subnetMask = otherMask;
                } else {
                    subnetPrefix = NetUtils.getSubnetPrefix(thisAddress, thisMaskLen);
                    subnetMask = thisMask;
                }
                MatchField<?> field = (type.equals(NwSrc.TYPE)) ? new NwSrc(subnetPrefix, subnetMask) : new NwDst(
                        subnetPrefix, subnetMask);
                intersection.setField(field);
                break;
            default:
                // this and other match field are equal for this type, pick this
                // match field
                intersection.setField(this.getField(type).clone());
            }
        }
        return intersection;
    }

    /**
     * Checks whether the intersection of the set of packets described by this
     * match with the set of packets described by the other match is non empty
     *
     * For example, if this match is: DL_SRC = 00:cc:bb:aa:11:22
     *
     * and the other match is: DL_TYPE = 0x800 NW_SRC = 1.2.3.4
     *
     * then their respective matching packets set intersection is non empty:
     * DL_SRC = 00:cc:bb:aa:11:22 DL_TYPE = 0x800 NW_SRC = 1.2.3.4
     *
     * @param other
     *            the other match with which testing the intersection
     * @return true if the intersection of the respective matching packets sets
     *         is non empty
     */
    public boolean intersetcs(Match other) {
        // No intersection with the empty set
        if (other == null) {
            return false;
        }
        // Always intersection with the universal set
        if (this.isEmpty() || other.isEmpty()) {
            return true;
        }

        // Get all the match types for both filters
        Set<String> allTypes = new HashSet<String>(this.fields.keySet());
        allTypes.addAll(new HashSet<String>(other.fields.keySet()));

        // Iterate through all the match types defined in the two filters
        for (String type : allTypes) {
            if (this.isAny(type) || other.isAny(type)) {
                continue;
            }

            MatchField<?> thisField = this.getField(type);
            MatchField<?> otherField = other.getField(type);

            switch (type) {
            case DlSrc.TYPE:
            case DlDst.TYPE:
                if (!Arrays.equals((byte[]) thisField.getValue(), (byte[]) otherField.getValue())) {
                    return false;
                }
                break;
            case NwSrc.TYPE:
            case NwDst.TYPE:
                InetAddress thisAddress = (InetAddress) thisField.getValue();
                InetAddress otherAddress = (InetAddress) otherField.getValue();
                // Validity check
                if (thisAddress instanceof Inet4Address && otherAddress instanceof Inet6Address
                        || thisAddress instanceof Inet6Address && otherAddress instanceof Inet4Address) {
                    return false;
                }
                InetAddress thisMask = (InetAddress) thisField.getMask();
                InetAddress otherMask = (InetAddress) otherField.getMask();
                if (NetUtils.inetAddressConflict(thisAddress, otherAddress, thisMask, otherMask)
                        && NetUtils.inetAddressConflict(otherAddress, thisAddress, otherMask, thisMask)) {
                    return false;
                }
                break;
            default:
                if (!thisField.getValue().equals(otherField.getValue())) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((fields == null) ? 0 : fields.hashCode());
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
        if (!(obj instanceof Match)) {
            return false;
        }
        Match other = (Match) obj;
        if (fields == null) {
            if (other.fields != null) {
                return false;
            }
        } else if (!fields.equals(other.fields)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "Match[" + fields.values() + "]";
    }
}
