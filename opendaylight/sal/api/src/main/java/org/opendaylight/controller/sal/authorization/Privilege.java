
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.authorization;

/**
 * It represents the group/resource access privilege
 */
public enum Privilege {
    NONE(""), // no privilege
    READ("r"), // read only
    USE("u"), // use
    WRITE("w"); // modify

    private String p;

    private Privilege(String p) {
        this.p = p;
    }

    @Override
    public String toString() {
        return p;
    }

}
