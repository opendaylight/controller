/*
 * Copyright (c) 2011,2012 Big Switch Networks, Inc.
 *
 * Licensed under the Eclipse Public License, Version 1.0 (the
 * "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 *      http://www.eclipse.org/legal/epl-v10.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * permissions and limitations under the License.
 *
 * This file incorporates work covered by the following copyright and
 * permission notice:
 *
 *    Originally created by David Erickson, Stanford University
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the
 *    License. You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing,
 *    software distributed under the License is distributed on an "AS
 *    IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 *    express or implied. See the License for the specific language
 *    governing permissions and limitations under the License.
 */

package org.opendaylight.controller.hosttracker;

import java.util.Date;

import org.opendaylight.controller.sal.core.NodeConnector;

/**
 * An entity on the network is a visible trace of a device that corresponds to a
 * packet received from a particular interface on the edge of a network, with a
 * particular VLAN tag, and a particular MAC address, along with any other
 * packet characteristics we might want to consider as helpful for
 * disambiguating devices.
 *
 * Entities are the most basic element of devices; devices consist of one or
 * more entities. Entities are immutable once created, except for the last seen
 * timestamp.
 *
 * @author readams
 *
 */
public class Entity implements Comparable<Entity> {
    /**
     * Timeout for computing {@link Entity#activeSince}.
     *
     * @see {@link Entity#activeSince}
     */
    protected static int ACTIVITY_TIMEOUT = 30000;

    /**
     * The MAC address associated with this entity
     */
    protected long macAddress;

    /**
     * The IP address associated with this entity, or null if no IP learned from
     * the network observation associated with this entity
     */
    protected Integer ipv4Address;

    /**
     * The VLAN tag on this entity, or null if untagged
     */
    protected Short vlan;

    /**
     * The attachment point for this entity
     */
    NodeConnector port;

    /**
     * The last time we observed this entity on the network
     */
    protected Date lastSeenTimestamp;

    /**
     * The time between {@link Entity#activeSince} and
     * {@link Entity#lastSeenTimestamp} is a period of activity for this entity
     * where it was observed repeatedly. If, when the entity is observed, the is
     * longer ago than the activity timeout, {@link Entity#lastSeenTimestamp}
     * and {@link Entity#activeSince} will be set to the current time.
     */
    protected Date activeSince;

    private int hashCode = 0;

    // ************
    // Constructors
    // ************

    /**
     * Create a new entity
     *
     * @param macAddress
     * @param vlan
     * @param ipv4Address
     * @param switchDPID
     * @param switchPort
     * @param lastSeenTimestamp
     */
    public Entity(long macAddress, Short vlan, Integer ipv4Address,
            NodeConnector port, Date lastSeenTimestamp) {
        this.macAddress = macAddress;
        this.ipv4Address = ipv4Address;
        this.vlan = vlan;
        this.port = port;
        this.lastSeenTimestamp = lastSeenTimestamp;
        this.activeSince = lastSeenTimestamp;
    }

    // ***************
    // Getters/Setters
    // ***************

    // @JsonSerialize(using=MACSerializer.class)
    public long getMacAddress() {
        return macAddress;
    }

    // @JsonSerialize(using=IPv4Serializer.class)
    public Integer getIpv4Address() {
        return ipv4Address;
    }

    public Short getVlan() {
        return vlan;
    }

    public NodeConnector getPort() {
        return port;
    }

    // @JsonIgnore
    public boolean hasSwitchPort() {
        return port != null;
    }

    public Date getLastSeenTimestamp() {
        return lastSeenTimestamp;
    }

    /**
     * Set the last seen timestamp and also update {@link Entity#activeSince} if
     * appropriate
     *
     * @param lastSeenTimestamp
     *            the new last seen timestamp
     * @see {@link Entity#activeSince}
     */
    public void setLastSeenTimestamp(Date lastSeenTimestamp) {
        if (activeSince == null
                || (activeSince.getTime() + ACTIVITY_TIMEOUT) < lastSeenTimestamp
                        .getTime())
            this.activeSince = lastSeenTimestamp;
        this.lastSeenTimestamp = lastSeenTimestamp;
    }

    public Date getActiveSince() {
        return activeSince;
    }

    public void setActiveSince(Date activeSince) {
        this.activeSince = activeSince;
    }

    @Override
    public int hashCode() {
        if (hashCode != 0)
            return hashCode;
        final int prime = 31;
        hashCode = 1;
        hashCode = prime * hashCode
                + ((ipv4Address == null) ? 0 : ipv4Address.hashCode());
        hashCode = prime * hashCode + (int) (macAddress ^ (macAddress >>> 32));
        hashCode = prime * hashCode + ((port == null) ? 0 : port.hashCode());
        hashCode = prime * hashCode + ((vlan == null) ? 0 : vlan.hashCode());
        return hashCode;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Entity other = (Entity) obj;
        if (ipv4Address == null) {
            if (other.ipv4Address != null)
                return false;
        } else if (!ipv4Address.equals(other.ipv4Address))
            return false;
        if (macAddress != other.macAddress)
            return false;
        if (port == null) {
            if (other.port != null)
                return false;
        } else if (!port.equals(other.port))
            return false;
        if (vlan == null) {
            if (other.vlan != null)
                return false;
        } else if (!vlan.equals(other.vlan))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "Entity [macAddress=" + macAddress + ", ipv4Address="
                + ipv4Address + ", vlan=" + vlan + ", port=" + port + "]";
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public int compareTo(Entity o) {
        int r;
        if (port == null)
            r = o.port == null ? 0 : -1;
        else if (o.port == null)
            r = 1;
        else {
            // XXX - the node id is only defined as an object rather
            // than something useful. We're just going to have to
            // blindly cast to Comparable and hope it works.
            Comparable switchId = (Comparable) port.getNode().getID();
            Comparable oswitchId = (Comparable) o.port.getNode().getID();
            r = switchId.compareTo(oswitchId);
            if (r != 0)
                return r;

            Comparable portId = (Comparable) port.getID();
            Comparable oportId = (Comparable) o.port.getID();
            r = portId.compareTo(oportId);
        }
        return r;
    }

}
