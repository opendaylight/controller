/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.usermanager.internal;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.opendaylight.controller.sal.authorization.UserLevel;
import org.opendaylight.controller.usermanager.ODLUserLevel;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * Represents a user that was successfully authenticated and authorized
 * It contains the user role for which the user was authorized and the
 * date on which it was authenticated and authorized
 */
public class AuthenticatedUser implements Serializable {
    private static final long serialVersionUID = 1L;
    private List<String> userRoles;
    private Date accessDate;

    public AuthenticatedUser(String name) {
        userRoles = null;
        accessDate = new Date();
    }

    public void setRoleList(List<String> roleList) {
        this.userRoles = roleList;
    }

    public void setRoleList(String[] roleArray) {
        userRoles = new ArrayList<String>(roleArray.length);
        for (String role : roleArray) {
            userRoles.add(role);
        }
    }

    public List<String> getUserRoles() {
        return userRoles;
    }

    public void addUserRole(String string) {
        userRoles.add(string);
    }

    public String getAccessDate() {
        return accessDate.toString();
    }

    public List<GrantedAuthority> getGrantedAuthorities(UserLevel usrLvl) {
        List<GrantedAuthority> roles = new ArrayList<GrantedAuthority>();
        roles.add(new SimpleGrantedAuthority(new ODLUserLevel(usrLvl)
                .getAuthority()));
        return roles;
    }

}
