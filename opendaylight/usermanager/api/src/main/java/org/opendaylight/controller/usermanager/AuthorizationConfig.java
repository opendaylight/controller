/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.usermanager;

import java.util.ArrayList;
import java.util.List;

import org.opendaylight.controller.sal.utils.Status;

/**
 * Configuration Java Object which represents a Local configured Authorization
 * for a remote authenticated user for User Manager.
 */
public class AuthorizationConfig extends UserConfig {
    private static final long serialVersionUID = 1L;

    public AuthorizationConfig() {
        super();
    }

    // Constructor may be needed for autocontainer logic
    public AuthorizationConfig(String user, List<String> roles) {
        super();
        this.user = user;
        this.roles = (roles == null) ? new ArrayList<String>()
                : new ArrayList<String>(roles);
    }

    @Override
    public Status validate() {
        Status status = validateUsername();
        if (status.isSuccess()) {
            status = validateRoles();
        }
        return status;
    }

    @Override
    public String toString() {
        return "AuthorizationConfig=[user: " + user + ", roles: " + roles + "]";
    }

    @Override
    public String getRolesString() {
        return super.getRolesString();
    }

}
