/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.rest.impl;

import java.util.ArrayList;
import java.util.Collection;
import org.apache.shiro.authz.permission.RolePermissionResolver;
import org.apache.shiro.authz.Permission;
import org.apache.shiro.authz.permission.WildcardPermission;

public class ODLRolePermissionResolver implements RolePermissionResolver {

    @Override
    public Collection<Permission> resolvePermissionsInRole(String roleString) {
        if (roleString.contains("admin")) {
            ArrayList<Permission> a = new ArrayList<Permission>();
            a.add(new WildcardPermission("*"));
            return a;
        }
        return new ArrayList<Permission>();
    }
}
