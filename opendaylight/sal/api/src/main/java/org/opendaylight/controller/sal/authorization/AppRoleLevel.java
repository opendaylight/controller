
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
 * Represents the application user role levels
 * This level has meaning only inside the application context
 * In the controller space such a role will be seen as <code>APPUSER<code>
 * as specified in {@link UserLevel}
 */
public enum AppRoleLevel implements Serializable {
    APPADMIN(0, "App-Admin", "Application Administrator"), APPUSER(1,
            "App-User", "Application User"), APPOPERATOR(2, "App-Operator",
            "Application Operator"), NOUSER(255, "Unknown User", "Unknown User");

    private int userLevel;
    private String level;
    private String prettyLevel;

    private AppRoleLevel(int userlevel, String level, String prettyLevel) {
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
    
    public static AppRoleLevel fromString(String levelString) {
    	for (AppRoleLevel level : AppRoleLevel.values()) {
    		if (level.toString().equals(levelString)) {
    			return level;
    		}
    	}
    	return null;    		
    }
} 
