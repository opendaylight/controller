
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
 * It represents the elementary resource along with
 * the access privilege associated to it
 */
@Deprecated
public class Resource implements Serializable {
    private static final long serialVersionUID = 1L;
    Object resource; // the generic resource
    Privilege privilege; // read/use/write privilege

    public Resource(Object resource, Privilege privilege) {
        this.resource = resource;
        this.privilege = privilege;
    }

    public Object getResource() {
        return resource;
    }

    public Privilege getPrivilege() {
        return privilege;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((privilege == null) ? 0 : privilege.hashCode());
        result = prime * result
                + ((resource == null) ? 0 : resource.hashCode());
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
        Resource other = (Resource) obj;
        if (privilege != other.privilege)
            return false;
        if (resource == null) {
            if (other.resource != null)
                return false;
        } else if (!resource.equals(other.resource))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "[" + resource + ", " + privilege.toString() + "]";
    }
}
