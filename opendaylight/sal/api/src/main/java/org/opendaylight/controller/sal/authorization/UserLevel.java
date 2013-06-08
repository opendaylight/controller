
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
 * Describes the user role level in the controller space
 */
public enum UserLevel implements Serializable {
    SYSTEMADMIN(0, "System-Admin", "System Administrator"), 	// can do everything
    NETWORKADMIN(1, "Network-Admin", "Network Administrator"),	// can do everything but setting a system admin user profile
    NETWORKOPERATOR(2, "Network-Operator", "Network Operator"),	// can only see what is configured anywhere
    CONTAINERUSER(4, "Container-User", "Container User"),		// container context user
    APPUSER(5, "App-User", "Application User"), 				// application context user
    NOUSER(255, "Not Authorized", "Not Authorized");

    private int userLevel;
    private String level;
    private String prettyLevel;

    private UserLevel(int userlevel, String level, String prettyLevel) {
        this.userLevel = userlevel;
        this.level = level;
        this.prettyLevel = prettyLevel;
    }

    public int toNumber() {
        return this.userLevel;
    }

    public String toString() {
        return this.level;
    }

    public String toStringPretty() {
        return this.prettyLevel;
    }
}
