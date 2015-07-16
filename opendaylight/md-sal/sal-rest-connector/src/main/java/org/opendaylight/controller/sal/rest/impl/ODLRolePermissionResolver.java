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
