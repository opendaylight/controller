/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.rest.impl;

import org.apache.shiro.authz.Permission;
import org.apache.shiro.authz.permission.*;

import java.util.Collection;
import java.util.Collections;

/**
 * Shiro class that resolves Roles to Permissions.  Permissions are slightly finer grain.
 *
 * TODO This may not be needed for RBAC.  Re-evaluate after the Authorization resolver is
 * working.
 *
 * @author ryandgoulding@gmail.com
 */
public class SimpleRolePermissionResolver implements RolePermissionResolver, PermissionResolverAware {

    private PermissionResolver permissionResolver = new WildcardPermissionResolver();

    public void setPermissionResolver(PermissionResolver permissionResolver) {
        this.permissionResolver = permissionResolver;
    }

    public Collection<Permission> resolvePermissionsInRole(String roleString) {
        return Collections.<Permission>singleton(permissionResolver.resolvePermission(roleString));
    }
}
