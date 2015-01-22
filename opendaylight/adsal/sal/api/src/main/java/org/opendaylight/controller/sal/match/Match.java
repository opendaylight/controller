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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.opendaylight.controller.sal.core.Property;
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
@Deprecated
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
    private int matches; // concise way to tell which fields the match
                         // is set for (may remove if not needed)
    private ConcurrentMap<String, Property> props;

    public Match() {
        fields = new HashMap<MatchType, MatchField>();
        matches = 0;
    }

    public Match(Match match) {
        fields = new HashMap<MatchType, MatchField>(match.fields);
        matches = match.matches;
    }

    /**
     * Gets the list of metadata currently registered with this match
     *
     * @return List of metadata currently registered
     */
    public List <Property> getMetadatas() {
        if (this.props != null) {
            // Return all the values in the map
            Collection res = this.props.values();
            if (res == null) {
                return Collections.emptyList();
            }
            return new ArrayList<Property>(res);
        }
        return Collections.emptyList();
    }

    /**
     * Gets the metadata registered with a name if present
     *
     * @param name the name of the property to be extracted
     *
     * @return List of metadata currently registered
     */
    public Property getMetadata(String name) {
        if (name == null) {
            return null;
        }
        if (this.props != null) {
            // Return the Property associated to the name
            return this.props.get(name);
        }
        return null;
    }

    /**
     * Sets the metadata associated to a name. If the name or prop is NULL,
     * an exception NullPointerException will be raised.
     *
     * @param name the name of the property to be set
     * @param prop, property to be set
     */
    public void setMetadata(String name, Property prop) {
        if (this.props == null) {
            props = new ConcurrentHashMap<String, Property>();
        }

        if (this.props != null) {
            this.props.put(name, prop);
        }
    }

    /**
     * Remove the metadata associated to a name. If the name is NULL,
     * nothing will be removed.
     *
     * @param name the name of the property to be set
     * @param prop, property to be set
     *
     * @return List of metadata currently registered
     */
    public void removeMetadata(String name) {
        if (this.props == null) {
            return;
        }

        if (this.props != null) {
            this.props.remove(name);
        }
        // It's intentional to keep the this.props still allocated
        // till the parent data structure will be alive, so to avoid
        // unnecessary allocation/deallocation, even if it's holding
        // nothing
    }

    /**
     * Generic setter for frame/packet/message's header fields against which to match
     * Note: For MAC addresses, please pass the cloned value to this function
     *
     * @param type      packet's header field type
     * @param value     field's value to assign to the match
     * @param mask      field's bitmask to apply to the match (has to be of the same class type of value)
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
     * @param type      packet's header field type
     * @param value     field's value to assign to the match
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
     * @param type  frame/packet/message's header field type
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
            cloned.matches = matches;
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
        for (Map.Entry<MatchType, MatchType> entry : Match.reversableMatches.entrySet()) {
            MatchType from = entry.getKey();
            MatchType to = entry.getValue();
            if (this.isPresent(from)) {
                reverse.setField(to, this.getField(from).getValue(), this.getField(from).getMask());
                if (!this.isPresent(to)) {
                    reverse.clearField(from);
                }
            }
        }

        // Reset asymmetric fields
        reverse.clearField(MatchType.IN_PORT);

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
     * described by the two matches is an empty.
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
     *         the intersection of this and the filter matches. null if the
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
        if (this.getMatches() == 0) {
            return other.clone();
        }
        if (other.getMatches() == 0) {
            return this.clone();
        }
        // Derive the intersection
        Match intersection = new Match();
        for (MatchType type : MatchType.values()) {
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
            case NW_SRC:
            case NW_DST:
                MatchField thisField = this.getField(type);
                MatchField otherField = other.getField(type);
                InetAddress thisAddress = (InetAddress) thisField.getValue();
                InetAddress otherAddress = (InetAddress) otherField.getValue();
                InetAddress thisMask = (InetAddress) thisField.getMask();
                InetAddress otherMask = (InetAddress) otherField.getMask();

                int thisMaskLen = (thisMask == null) ? ((thisAddress instanceof Inet4Address) ? 32 : 128) : NetUtils
                        .getSubnetMaskLength(thisMask);
                int otherMaskLen = (otherMask == null) ? ((otherAddress instanceof Inet4Address) ? 32 : 128) : NetUtils
                        .getSubnetMaskLength(otherMask);
                if (thisMaskLen < otherMaskLen) {
                    intersection.setField(new MatchField(type, NetUtils.getSubnetPrefix(otherAddress, otherMaskLen),
                            otherMask));
                } else {
                    intersection.setField(new MatchField(type, NetUtils.getSubnetPrefix(thisAddress, thisMaskLen),
                            thisMask));
                }
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
        if (this.getMatches() == 0 || other.getMatches() == 0) {
            return true;
        }
        // Iterate through the MatchType defined in the filter
        for (MatchType type : MatchType.values()) {
            if (this.isAny(type) || other.isAny(type)) {
                continue;
            }

            MatchField thisField = this.getField(type);
            MatchField otherField = other.getField(type);

            switch (type) {
            case DL_SRC:
            case DL_DST:
                if (!Arrays.equals((byte[]) thisField.getValue(), (byte[]) otherField.getValue())) {
                    return false;
                }
                break;
            case NW_SRC:
            case NW_DST:
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
        if (this.fields == null) {
            result = prime * result;
        } else {
            // use a tree map as the order of hashMap is not guaranteed.
            // 2 Match objects with fields in different order are still equal.
            // Hence the hashCode should be the same too.
            TreeMap<MatchType, MatchField> tm = new TreeMap<MatchType, MatchField>(this.fields);
            for (MatchType field : tm.keySet()) {
                MatchField f = tm.get(field);
                int fieldHashCode = (field==null ? 0 : field.calculateConsistentHashCode()) ^
                             (f==null ? 0 : f.hashCode());
                result = prime * result + fieldHashCode;
            }
        }
        result = prime * result + matches;
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
        if (getClass() != obj.getClass()) {
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
        if (matches != other.matches) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Match [fields=");
        builder.append(fields);
        builder.append(", matches=");
        builder.append(matches);
        builder.append("]");
        return builder.toString();
    }

}
