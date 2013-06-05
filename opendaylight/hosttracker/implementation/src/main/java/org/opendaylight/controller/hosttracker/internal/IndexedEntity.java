/*
 * Copyright (c) 2013 Big Switch Networks, Inc.
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
 */

package org.opendaylight.controller.hosttracker.internal;

import java.util.EnumSet;

import org.opendaylight.controller.hosttracker.Entity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.opendaylight.controller.hosttracker.IDeviceService;
import org.opendaylight.controller.hosttracker.IDeviceService.DeviceField;


/**
 * This is a thin wrapper around {@link Entity} that allows overriding
 * the behavior of {@link Object#hashCode()} and {@link Object#equals(Object)}
 * so that the keying behavior in a hash map can be changed dynamically
 * @author readams
 */
public class IndexedEntity {
    protected EnumSet<DeviceField> keyFields;
    protected Entity entity;
    private int hashCode = 0;
    protected static Logger logger =
            LoggerFactory.getLogger(IndexedEntity.class);
    /**
     * Create a new {@link IndexedEntity} for the given {@link Entity} using 
     * the provided key fields.
     * @param keyFields The key fields that will be used for computing
     * {@link IndexedEntity#hashCode()} and {@link IndexedEntity#equals(Object)}
     * @param entity the entity to wrap
     */
    public IndexedEntity(EnumSet<DeviceField> keyFields, Entity entity) {
        super();
        this.keyFields = keyFields;
        this.entity = entity;
    }

    /**
     * Check whether this entity has non-null values in any of its key fields
     * @return true if any key fields have a non-null value
     */
    public boolean hasNonNullKeys() {
        for (DeviceField f : keyFields) {
            switch (f) {
                case MAC:
                    return true;
                case IPV4:
                    if (entity.getIpv4Address() != null) return true;
                    break;
                case SWITCHPORT:
                    if (entity.getPort() != null) return true;
                    break;
                case VLAN:
                    if (entity.getVlan() != null) return true;
                    break;
            }
        }
        return false;
    }
    
    @Override
    public int hashCode() {
    	
        if (hashCode != 0) {
        	return hashCode;
        }

        final int prime = 31;
        hashCode = 1;
        for (DeviceField f : keyFields) {
            switch (f) {
                case MAC:
                    hashCode = prime * hashCode
                        + (int) (entity.getMacAddress() ^ 
                                (entity.getMacAddress() >>> 32));
                    break;
                case IPV4:
                    hashCode = prime * hashCode
                        + ((entity.getIpv4Address() == null) 
                            ? 0 
                            : entity.getIpv4Address().hashCode());
                    break;
                case SWITCHPORT:
                    hashCode = prime * hashCode
                        + ((entity.getPort() == null) 
                            ? 0 
                            : entity.getPort().hashCode());
                    break;
                case VLAN:
                    hashCode = prime * hashCode 
                        + ((entity.getVlan() == null) 
                            ? 0 
                            : entity.getVlan().hashCode());
                    break;
            }
        }
        return hashCode;
    }
    
    @Override
    public boolean equals(Object obj) {
       if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        IndexedEntity other = (IndexedEntity) obj;
        
        if (!keyFields.equals(other.keyFields))
            return false;

        for (IDeviceService.DeviceField f : keyFields) {
            switch (f) {
                case MAC:
                    if (entity.getMacAddress() != other.entity.getMacAddress())
                        return false;
                    break;
                case IPV4:
                    if (entity.getIpv4Address() == null) {
                        if (other.entity.getIpv4Address() != null) return false;
                    } else if (!entity.getIpv4Address().
                            equals(other.entity.getIpv4Address())) return false;
                    break;
                case SWITCHPORT:
                    if (entity.getPort() == null) {
                        if (other.entity.getPort() != null) return false;
                    } else if (!entity.getPort().
                            equals(other.entity.getPort())) return false;
                    break;
                case VLAN:
                    if (entity.getVlan() == null) {
                        if (other.entity.getVlan() != null) return false;
                    } else if (!entity.getVlan().
                            equals(other.entity.getVlan())) return false;
                    break;
            }
        }
        
        return true;
    }
    
    
}
