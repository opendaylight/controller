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
