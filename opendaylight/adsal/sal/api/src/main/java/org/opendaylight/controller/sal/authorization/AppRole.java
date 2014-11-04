
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
 * Represents a user role level in the application space
 * It contains the role name and the role level in the
 * application context as specified by {@link AppRoleLevel}
 */
public class AppRole implements Serializable {
    private static final long serialVersionUID = 1L;
    String name;
    AppRoleLevel level;

    public AppRole(String name, AppRoleLevel level) {
        this.name = name;
        this.level = level;
    }

    /**
     * Returns the application role name
     * @return the string containing the role name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the application role level
     * @return the application user level
     */
    public AppRoleLevel getLevel() {
        return level;
    }
}