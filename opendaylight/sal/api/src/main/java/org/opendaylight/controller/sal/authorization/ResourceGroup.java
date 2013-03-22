
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.authorization;

import java.io.Serializable;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * Represents a group of resources along with the privilege associated to it
 *
 *
 *
 */
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
        return HashCodeBuilder.reflectionHashCode(this);
    }

    @Override
    public boolean equals(Object obj) {
        return EqualsBuilder.reflectionEquals(this, obj);
    }

    @Override
    public String toString() {
        return "[" + groupName + ", " + privilege.toString() + "]";
    }
}
