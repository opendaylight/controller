
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.authorization;

import java.util.List;
import java.util.Set;

import org.opendaylight.controller.sal.utils.Status;

/**
 * Interface for applications which maintain an authorization
 * database for their resources. Respective application web bundle
 * and User Manager make use of this interface to retrieve
 * authorization information at user or and role level.
 */
public interface IResourceAuthorization {

    /**
     * Create a Role name for the application
     *
     * @param role  the role name
     * @param userLevel the user level in the application context
     * @return the status of the request
     */
    public Status createRole(String role, AppRoleLevel userLevel);

    /**
     * Remove a Role
     *
     * @param role the role name
     * @return the status of the request
     */
    public Status removeRole(String role);

    /**
     * Return the list of roles configured for the application
     *
     * @return the list of roles
     */
    public List<String> getRoles();

    /**
     * Returns the application role level for the specified role. If the role is
     * not known to this application NOUSER will be returned as specified in
     * {@link AppRoleLevel}
     *
     * @param roleName
     *            the role name to query
     * @return the application level of the given role in the application
     *         context as specified by {@link AppRoleLevel}. If the role is not
     *         part of this application's roles, NOUSER is returned.
     */
    public AppRoleLevel getApplicationRoleLevel(String roleName);

    /**
     * Returns whether the specified role is part of this application's roles
     *
     * @param roleName the role name to test
     * @return  true if the role belongs to this application, false otherwise
     */
    public boolean isApplicationRole(String roleName);

    /**
     * Create a resource group for application.
     *
     * NOTE: Resource addition is "best effort", if an object is not of correct type,
     * it is discarded.
     *
     * @param groupName
     *            the name for the resource group
     * @param resources
     *            the list of resources for the group
     * @return the status of the request
     */
    public Status createResourceGroup(String groupName, List<Object> resources);

    /**
     * Removes a resource group
     *
     * @param groupName the name of the group
     * @return the status of the request
     */
    public Status removeResourceGroup(String groupName);

    /**
     * Returns the list of resource groups configured for the application
     *
     * @return the list of resource group names
     */
    public List<String> getResourceGroups();

    /**
     * Assign a resource group to a role
     *
     * @param groupName the name of the resource group
     * @param privilege the access privilege role will have on the resource group
     * @param role the role name
     * @return the status of the request
     */
    @Deprecated
    public Status assignResourceGroupToRole(String groupName,
            Privilege privilege, String role);

    /**
     * Assign a resource group to a role. The access privilege on the resources
     * is inferred by the AppRoleLevel associated to role.
     *
     * @param groupName the name of the resource group
     * @param role the role name
     * @return the status of the request
     */
    public Status assignResourceGroupToRole(String groupName, String role);

    /**
     * Unassign the passed resource group from the specified role
     *
     * @param groupName the name of the resource group
     * @param role the role name
     * @return the status of the request
     */
    public Status unassignResourceGroupFromRole(String groupName, String role);

    /**
     * Returns the list of resource groups the given Role is authorized to use
     * The returning object expresses the resource group name and the access
     * its privilege for the given user role
     *
     * @param role  the role name
     * @return list of resources
     */
    public List<ResourceGroup> getAuthorizedGroups(String role);

    /**
     * Returns the list of resources contained in the given resource group
     *
     * @param groupName the resource group name
     * @return
     */
    public List<Object> getResources(String groupName);

    /**
     * Returns the list of authorized resources for the given role
     * For each resource only the highest privilege occurrence is returned
     * @param role  the role name
     * @return the list of Resource
     */
    public List<Resource> getAuthorizedResources(String role);

    /*
     * Per user name API
     */
    /**
     * Returns the controller user role level the passed user name is associated with
     *
     * @param userName the user name
     * @return the user role level as specified in {@link UserLevel}
     */
    public UserLevel getUserLevel(String userName);

    /**
     * Returns the application context user role level the passed user name is associated with
     *
     * @param userName the user name
     * @return the user role level as specified in {@link AppRoleLevel}
     */
    public AppRoleLevel getUserApplicationLevel(String userName);

    /**
     * Returns the list of resources (resource + privilege) associated
     * with the passed user name for this application context
     * For each resource only the highest privilege occurrence is returned
     *
     * @param userName the user name
     * @return the list of resources associated with this user name in this application context
     */
    public Set<Resource> getAllResourcesforUser(String userName);

    /**
     * Returns the highest privilege that the user has on the specified
     * resource in this application context
     *
     * @param userName the user name
     * @param resource the given resource
     * @return the privilege the user has on the passed resource
     */
    public Privilege getResourcePrivilege(String userName, Object resource);

    /**
     * Add a resource to a group
     *
     * @param groupName
     *            the resource group
     * @param resource
     *            the resource object
     * @return the status of the request
     */
    public Status addResourceToGroup(String groupName, Object resource);

    /**
     * Remove a resource from a group
     *
     * @param groupName
     *            the resource group
     * @param resource
     *            the resource object
     * @return the status of the request
     */
    public Status removeResourceFromGroup(String groupName, Object resource);

    /**
     * Return whether the specified user has access to this application. In
     * other words if the user is associated any roles belonging to this
     * application.
     *
     * @param userName
     *            the user name
     * @return true if the user has access to this application's resources,
     *         false otherwise
     */
    boolean isApplicationUser(String userName);
}
