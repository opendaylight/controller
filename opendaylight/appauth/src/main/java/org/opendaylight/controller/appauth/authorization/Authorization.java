
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.appauth.authorization;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

import org.opendaylight.controller.containermanager.IContainerAuthorization;
import org.opendaylight.controller.sal.authorization.AppRoleLevel;
import org.opendaylight.controller.sal.authorization.IResourceAuthorization;
import org.opendaylight.controller.sal.authorization.Privilege;
import org.opendaylight.controller.sal.authorization.Resource;
import org.opendaylight.controller.sal.authorization.ResourceGroup;
import org.opendaylight.controller.sal.authorization.UserLevel;
import org.opendaylight.controller.sal.utils.ServiceHelper;
import org.opendaylight.controller.sal.utils.Status;
import org.opendaylight.controller.sal.utils.StatusCode;
import org.opendaylight.controller.usermanager.IUserManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This abstract class implements the methods defined by IResourceAuthorization
 * interface for each application class.
 */
public abstract class Authorization<T> implements IResourceAuthorization {
private static final Logger logger = LoggerFactory.getLogger(Authorization.class);
    private static final String namesRegex = "^[a-zA-Z0-9]+[{\\.|\\_|\\-}[a-zA-Z0-9]]*$";
    /*
     * The configured resource groups
     */
    protected ConcurrentMap<String, Set<T>> resourceGroups;
    /*
     * The configured roles along with their level
     */
    protected ConcurrentMap<String, AppRoleLevel> roles;
    /*
     * The association of groups to roles
     */
    protected ConcurrentMap<String, Set<ResourceGroup>> groupsAuthorizations;
    /*
     * The name of the default group. It is the group which contains all the
     * resources
     */
    protected String allResourcesGroupName;

    @Override
    public Status createRole(String role, AppRoleLevel level) {
        if (role == null || role.trim().isEmpty()
                || !role.matches(Authorization.namesRegex)) {
            return new Status(StatusCode.BADREQUEST,
                    "Role name must start with alphanumeric, no special characters.");
        }
        if (isControllerRole(role)) {
            return new Status(StatusCode.NOTALLOWED,
                    "Controller roles cannot be explicitely "
                            + "created in App context");
        }
        if (isContainerRole(role)) {
            return new Status(StatusCode.NOTALLOWED,
                    "Container roles cannot be explicitely "
                            + "created in App context");
        }
        if (isRoleInUse(role)) {
            return new Status(StatusCode.CONFLICT, "Role already in use");
        }
        if (roles.containsKey(role)) {
            if (roles.get(role).equals(level)) {
                return new Status(StatusCode.SUCCESS, "Role is already present");
            } else {
                return new Status(StatusCode.BADREQUEST,
                        "Role exists and has different level");
            }
        }

        return createRoleInternal(role, level);
    }

    protected Status createRoleInternal(String role, AppRoleLevel level) {
        roles.put(role, level);
        groupsAuthorizations.put(role, new HashSet<ResourceGroup>());
        return new Status(StatusCode.SUCCESS);
    }

    @Override
    public Status removeRole(String role) {
        if (role == null || role.trim().isEmpty()) {
            return new Status(StatusCode.BADREQUEST, "Role name can't be empty");
        }
        if (isControllerRole(role)) {
            return new Status(StatusCode.NOTALLOWED,
                    "Controller roles cannot be removed");
        }
        if (isContainerRole(role)) {
            return new Status(StatusCode.NOTALLOWED,
                    "Container roles cannot be removed");
        }
        return removeRoleInternal(role);
    }

    protected Status removeRoleInternal(String role) {
        groupsAuthorizations.remove(role);
        roles.remove(role);
        return new Status(StatusCode.SUCCESS);
    }

    @Override
    public List<String> getRoles() {
        return new ArrayList<String>(groupsAuthorizations.keySet());
    }

    @SuppressWarnings("unchecked")
    @Override
    public Status createResourceGroup(String groupName, List<Object> resources) {
        //verify group name not null/empty
        if (groupName == null || groupName.trim().isEmpty()
                || !groupName.matches(Authorization.namesRegex)) {
            return new Status(StatusCode.BADREQUEST, "Group name must start with alphanumeric, no special characters");
        }
        //verify group name is not same as all-resources
        if (groupName.equals(this.allResourcesGroupName)) {
            return new Status(StatusCode.NOTALLOWED, "All resource group cannot be created");
        }
        //verify group name is unique
        if (resourceGroups.containsKey(groupName)) {
            return new Status(StatusCode.CONFLICT, "Group name already exists");
        }

        //try adding resources, discard if not of type T
        Set<T> toBeAdded = new HashSet<T>();
        boolean allAdded = true;
        for (Object obj : resources) {
            try {
                toBeAdded.add((T) obj);
            } catch (ClassCastException e) {
                logger.debug("Attempt to add a resource with invalid type");
                allAdded = false;
            }
        }
        resourceGroups.put(groupName, toBeAdded);
        return (allAdded ? new Status(StatusCode.SUCCESS, "All resources added succesfully") :
            new Status(StatusCode.SUCCESS, "One or more resources couldn't be added"));
    }

    @SuppressWarnings("unchecked")
    @Override
    public Status addResourceToGroup(String groupName, Object resource) {
        if (groupName == null || groupName.trim().isEmpty()) {
            return new Status(StatusCode.BADREQUEST, "Invalid group name");
        }

        if (resource == null) {
            return new Status(StatusCode.BADREQUEST, "Null resource");
        }

        T castedResource = null;
        try {
            castedResource = (T) resource;
        } catch (ClassCastException e) {
            logger.debug("Attempt to add a resource with invalid type");
            return new Status(StatusCode.BADREQUEST, "Incompatible resource");
        }

        Set<T> group = resourceGroups.get(groupName);
        if (group == null) {
            return new Status(StatusCode.NOTFOUND, "Group not found");
        }

        return addResourceToGroupInternal(groupName, castedResource);
    }

    /*
     * Method child classes can overload if they need application specific
     * checks on the resource
     */
    protected Status addResourceToGroupInternal(String groupName, T resource) {
        Set<T> group = resourceGroups.get(groupName);
        // Update group and cluster
        group.add(resource);
        resourceGroups.put(groupName, group);

        return new Status(StatusCode.SUCCESS, "Resource added successfully");

    }

    private Status removeRoleResourceGroupMapping(String groupName) {
        List<String> affectedRoles = new ArrayList<String>();
        Status result;
        for (Entry<String, Set<ResourceGroup>> pairs : groupsAuthorizations.entrySet()) {
            String role = pairs.getKey();
            Set<ResourceGroup> groups = pairs.getValue();
            for (ResourceGroup group : groups) {
                if (group.getGroupName().equals(groupName)) {
                    affectedRoles.add(role);
                    break;
                }
            }
        }
        StringBuffer msg = new StringBuffer();
        for (String role : affectedRoles) {
            result = unassignResourceGroupFromRole(groupName, role);
            if (!result.isSuccess()) {
                msg.append(result.getDescription());
                msg.append(' ');
            }
        }

        if (msg.length() != 0) {
            return new Status(StatusCode.BADREQUEST, msg.toString());
        } else {
            return new Status(StatusCode.SUCCESS);
        }
    }

    @Override
    public Status removeResourceGroup(String groupName) {
        // Default resource group cannot be deleted
        if (groupName == null || groupName.trim().isEmpty()) {
            return new Status(StatusCode.BADREQUEST, "Invalid group name");
        }
        if (groupName.equals(this.allResourcesGroupName)) {
            return new Status(StatusCode.NOTALLOWED,
                    "All resource group cannot be removed");
        }
        resourceGroups.remove(groupName);
        Status result = removeRoleResourceGroupMapping(groupName);

        return result.isSuccess() ? result :
            new Status(StatusCode.SUCCESS, "Failed removing group from: " + result.getDescription());
    }


    @Override
    public Status removeResourceFromGroup(String groupName, Object resource) {
        if (groupName == null || groupName.trim().isEmpty()) {
            return new Status(StatusCode.BADREQUEST, "Invalid group name");
        }

        Set<T> group = resourceGroups.get(groupName);
        if (group != null && group.remove(resource)) {
            // Update cluster
            resourceGroups.put(groupName, group);
            return new Status(StatusCode.SUCCESS, "Resource removed successfully");
        }

        return new Status(StatusCode.NOTFOUND, "Group/Resource not found");
    }


    /**
     * Relay the call to user manager
     */
    @Override
    public UserLevel getUserLevel(String userName) {
        IUserManager userManager = (IUserManager) ServiceHelper
                .getGlobalInstance(IUserManager.class, this);

        if (logger.isDebugEnabled()) {
            logger.debug("User {} has UserLevel {}", userName, userManager.getUserLevel(userName));
        }

        return (userManager == null) ? UserLevel.NOUSER : userManager
                .getUserLevel(userName);
    }

    @Override
    public Set<Resource> getAllResourcesforUser(String userName) {
        Set<Resource> resources = new HashSet<Resource>();
        IUserManager userManager = (IUserManager) ServiceHelper
                .getGlobalInstance(IUserManager.class, this);
        if (userManager == null) {
            return new HashSet<Resource>(0);
        }

        // Get the roles associated with this user
        List<String> roles = userManager.getUserRoles(userName);
        if (roles == null) {
            return resources;
        }

        for (String role : roles) {
            // Get our resource groups associated with this role
            List<ResourceGroup> groups = this.getAuthorizedGroups(role);
            if (groups.isEmpty()) {
                continue;
            }
            for (ResourceGroup group : groups) {
                // Get the list of resources in this group
                List<Object> list = this.getResources(group.getGroupName());
                if (list.isEmpty()) {
                    continue;
                }
                for (Object resource : list) {
                    Resource toBeAdded = new Resource(resource,
                            group.getPrivilege());
                    /*
                     * Add the resource to the set if the same resource with
                     * higher privilege is not present. If the same resource
                     * with lower privilege is present, remove it. No check on
                     * same privilege resource as set guarantees no duplicates.
                     */
                    Resource existing = higherPrivilegeResourcePresent(
                            resources, toBeAdded);
                    if (existing == null) {
                        resources.add(toBeAdded);
                    }
                    existing = lowerPrivilegeResourcePresent(resources,
                            toBeAdded);
                    if (existing != null) {
                        resources.remove(existing);
                    }
                }
            }
        }

        if (logger.isDebugEnabled()) {
            logger.debug("User {} has resources {}", userName, resources);
        }

        return resources;
    }

    @Override
    public List<Resource> getAuthorizedResources(String role) {
        Set<Resource> resources = new HashSet<Resource>();

        // Get our resource groups associated with this role
        List<ResourceGroup> groups = this.getAuthorizedGroups(role);
        if (groups.isEmpty()) {
            return new ArrayList<Resource>(0);
        }
        for (ResourceGroup group : groups) {
            // Get the list of resources in this group
            List<Object> list = this.getResources(group.getGroupName());
            if (list.isEmpty()) {
                continue;
            }
            for (Object resource : list) {
                Resource toBeAdded = new Resource(resource,
                        group.getPrivilege());
                /*
                 * Add the resource to the set if the same resource with higher
                 * privilege is not present. If the same resource with lower
                 * privilege is present, remove it. No check on same privilege
                 * resource as set guarantees no duplicates.
                 */
                Resource existing = higherPrivilegeResourcePresent(resources,
                        toBeAdded);
                if (existing == null) {
                    resources.add(toBeAdded);
                }
                existing = lowerPrivilegeResourcePresent(resources, toBeAdded);
                if (existing != null) {
                    resources.remove(existing);
                }
            }
        }

        if (logger.isDebugEnabled()) {
            logger.debug("For the Role {}, Authorized Resources are {}", role, resources);
        }

        return new ArrayList<Resource>(resources);
    }

    /**
     * Given a set of resources and a resource to test, it returns the element
     * in the set which represent the same resource with a lower privilege
     * associated to it, if present.
     *
     * @param resources
     * @param resource
     * @return
     */
    private Resource lowerPrivilegeResourcePresent(Set<Resource> resources,
            Resource resource) {

        if (resource == null || resources == null) {
            return null;
        }

        Object resourceElement = resource.getResource();
        Privilege resourcePrivilege = resource.getPrivilege();
        for (Resource element : resources) {
            if (element.getResource().equals(resourceElement)
                    && element.getPrivilege().ordinal() < resourcePrivilege
                            .ordinal()) {
                return element;
            }
        }
        return null;
    }

    /**
     * Given a set of resources and a resource to test, it returns the element
     * in the set which represents the same resource with an higher privilege,
     * if present.
     *
     * @param resources
     * @param resource
     * @return
     */
    private Resource higherPrivilegeResourcePresent(Set<Resource> resources,
            Resource resource) {

        if (resource == null || resources == null) {
            return null;
        }

        Object resourceElement = resource.getResource();
        Privilege resourcePrivilege = resource.getPrivilege();
        for (Resource element : resources) {
            if (element.getResource().equals(resourceElement)
                    && element.getPrivilege().ordinal() > resourcePrivilege
                            .ordinal()) {
                return element;
            }
        }

        return null;
    }

    @Override
    public Privilege getResourcePrivilege(String userName, Object resource) {

        if (userName == null || userName.trim().isEmpty() || resource == null) {
            return Privilege.NONE;
        }

        Set<Resource> hisResources = getAllResourcesforUser(userName);
        for (Resource element : hisResources) {
            if (element.getResource().equals(resource)) {
                return element.getPrivilege();
            }
        }

        return Privilege.NONE;
    }

    @Override
    public List<String> getResourceGroups() {
        return new ArrayList<String>(resourceGroups.keySet());
    }

    @Override
    public Status assignResourceGroupToRole(String group, Privilege privilege,
            String role) {
        if (group == null || group.trim().isEmpty()) {
            return new Status(StatusCode.BADREQUEST, "Invalid group name");
        }
        if (role == null || role.trim().isEmpty()) {
            return new Status(StatusCode.BADREQUEST, "Role name can't be empty");
        }
        if (isControllerRole(role)) {
            return new Status(StatusCode.NOTALLOWED,
                    "No group assignment is accepted for Controller roles");
        }

        return assignResourceGroupToRoleInternal(group, privilege, role);
    }

    protected Status assignResourceGroupToRoleInternal(String group, Privilege privilege, String role) {
        Set<ResourceGroup> roleGroups = groupsAuthorizations.get(role);
        roleGroups.add(new ResourceGroup(group, privilege));
        // Update cluster
        groupsAuthorizations.put(role, roleGroups);
        return new Status(StatusCode.SUCCESS);
    }

    @Override
    public Status assignResourceGroupToRole(String groupName, String roleName) {
        if (groupName == null || groupName.trim().isEmpty()) {
            return new Status(StatusCode.BADREQUEST, "Group name can't be empty");
        }
        if (roleName == null || roleName.trim().isEmpty()) {
            return new Status(StatusCode.BADREQUEST, "Role name can't be empty");
        }
        // Infer group privilege from role's level
        Privilege privilege = Privilege.NONE;
        switch (this.getApplicationRoleLevel(roleName)) {
        case APPADMIN:
            privilege = Privilege.WRITE;
            break;
        case APPUSER:
            privilege = Privilege.USE;
            break;
        case APPOPERATOR:
            privilege = Privilege.READ;
            break;
        default:
            break;
        }
        return this.assignResourceGroupToRole(groupName, privilege, roleName);
    }

    @Override
    public Status unassignResourceGroupFromRole(String group, String role) {
        if (group == null || group.trim().isEmpty()) {
            return new Status(StatusCode.BADREQUEST, "Group name can't be empty");
        }
        if (role == null || role.trim().isEmpty()) {
            return new Status(StatusCode.BADREQUEST, "Role name can't be empty");
        }
        if (isControllerRole(role)) {
            return new Status(StatusCode.NOTALLOWED,
                    "No group assignment change is allowed for "
                            + "Controller roles");
        }

        return unassignResourceGroupFromRoleInternal(group, role);
    }

    protected Status unassignResourceGroupFromRoleInternal(String group, String role) {
        ResourceGroup target = null;
        for (ResourceGroup rGroup : groupsAuthorizations.get(role)) {
            if (rGroup.getGroupName().equals(group)) {
                target = rGroup;
                break;
            }
        }
        if (target == null) {
            return new Status(StatusCode.SUCCESS, "Group " + group + " was not assigned to " + role);
        } else {
            Set<ResourceGroup> groups = groupsAuthorizations.get(role);
            groups.remove(target);
            // Update cluster
            groupsAuthorizations.put(role, groups);
            return new Status(StatusCode.SUCCESS);

        }
    }

    @Override
    public List<ResourceGroup> getAuthorizedGroups(String role) {
        return (groupsAuthorizations.containsKey(role)) ? new ArrayList<ResourceGroup>(
                groupsAuthorizations.get(role))
                : new ArrayList<ResourceGroup>();
    }

    @Override
    public List<Object> getResources(String groupName) {
        return (resourceGroups.containsKey(groupName)) ? new ArrayList<Object>(
                resourceGroups.get(groupName)) : new ArrayList<Object>(0);
    }

    @Override
    public boolean isApplicationRole(String roleName) {
        if (roleName == null) {
            return false;
        }
        return roles.containsKey(roleName);
    }

    @Override
    public boolean isApplicationUser(String userName) {
        IUserManager userManager = (IUserManager) ServiceHelper
                .getGlobalInstance(IUserManager.class, this);
        if (userManager == null) {
            return false;
        }
        List<String> roles = userManager.getUserRoles(userName);
        if (roles != null && !roles.isEmpty()) {
            for (String role : roles) {
                if (isApplicationRole(role)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public AppRoleLevel getApplicationRoleLevel(String roleName) {
        if (roleName == null || roleName.trim().isEmpty()) {
            return AppRoleLevel.NOUSER;
        }

        if (isControllerRole(roleName)) {
            if (roleName.equals(UserLevel.NETWORKADMIN.toString()) ||
                    roleName.equals(UserLevel.SYSTEMADMIN.toString())) {
                return AppRoleLevel.APPADMIN;
            } else {
                return AppRoleLevel.APPOPERATOR;
            }
        }

        return (roles.containsKey(roleName)) ? roles.get(roleName)
                : AppRoleLevel.NOUSER;
    }

    @Override
    public AppRoleLevel getUserApplicationLevel(String userName) {
        List<String> roles = null;
        IUserManager userManager = (IUserManager) ServiceHelper
                .getGlobalInstance(IUserManager.class, this);

        if (userName == null || userName.trim().isEmpty()
                || (userManager == null)
                || (roles = userManager.getUserRoles(userName)).isEmpty()) {
            return AppRoleLevel.NOUSER;
        }
        AppRoleLevel highestLevel = AppRoleLevel.NOUSER;
        for (String role : roles) {
            AppRoleLevel level = getApplicationRoleLevel(role);
            if (level.ordinal() < highestLevel.ordinal()) {
                highestLevel = level;
            }
        }
        return highestLevel;
    }

    /**
     * Returns the highest role the specified user has in this application
     * context
     *
     * @param user
     *            The user name
     * @return The highest role associated to the user in this application
     *         context
     */
    public String getHighestUserRole(String user) {
        IUserManager userManager = (IUserManager) ServiceHelper.getGlobalInstance(IUserManager.class, this);
        String highestRole = "";
        if (userManager != null && !userManager.getUserRoles(user).isEmpty()) {
            List<String> roles = userManager.getUserRoles(user);
            AppRoleLevel highestLevel = AppRoleLevel.NOUSER;
            for (String role : roles) {
                AppRoleLevel current;
                if (isApplicationRole(role)
                        && (current = getApplicationRoleLevel(role)).ordinal() < highestLevel.ordinal()) {
                    highestRole = role;
                    highestLevel = current;
                }
            }
        }
        return highestRole;
    }

    private boolean isControllerRole(String role) {
        return (role.equals(UserLevel.NETWORKADMIN.toString())
                || role.equals(UserLevel.SYSTEMADMIN.toString()) || role
                    .equals(UserLevel.NETWORKOPERATOR.toString()));
    }

    private boolean isContainerRole(String role) {
        IContainerAuthorization containerAuth = (IContainerAuthorization) ServiceHelper.getGlobalInstance(
                IContainerAuthorization.class, this);
        if (containerAuth == null) {
            return false;
        }
        return containerAuth.isApplicationRole(role);
    }

    private boolean isRoleInUse(String role) {
        IUserManager userManager = (IUserManager) ServiceHelper
                .getGlobalInstance(IUserManager.class, this);

        if (userManager == null) {
            return true;
        }
        return userManager.isRoleInUse(role);
    }
}
