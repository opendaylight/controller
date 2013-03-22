
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
 * It represents the elementary resource along with
 * the access privilege associated to it
 */
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
        return HashCodeBuilder.reflectionHashCode(this);
    }

    @Override
    public boolean equals(Object obj) {
        return EqualsBuilder.reflectionEquals(this, obj);
    }

    @Override
    public String toString() {
        return "[" + resource + ", " + privilege.toString() + "]";
    }
}
