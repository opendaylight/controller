
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.authorization;

import java.io.Serializable;

/**
 * Represents a group of resources along with the privilege associated to it
 *
 *
 *
 */
@Deprecated
public class ResourceGroup implements Serializable {
    private static final long serialVersionUID = 1L;
    private String groupName; // the resource group name
    private Privilege privilege; // the privilege for this profile on the resource group

    public ResourceGroup(String groupName, Privilege privilege) {
        this.groupName = groupName;
        this.privilege = privilege;
    }

    /**
     * Returns the name for this resource group
     * @return
     */
    public String getGroupName() {
        return groupName;
    }

    /**
     * Returns the privilege for this group on its resources
     * @return
     */
    public Privilege getPrivilege() {
        return privilege;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((groupName == null) ? 0 : groupName.hashCode());
        result = prime * result
                + ((privilege == null) ? 0 : privilege.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ResourceGroup other = (ResourceGroup) obj;
        if (groupName == null) {
            if (other.groupName != null)
                return false;
        } else if (!groupName.equals(other.groupName))
            return false;
        if (privilege != other.privilege)
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "[" + groupName + ", " + privilege.toString() + "]";
    }
}
