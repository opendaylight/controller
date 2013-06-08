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
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.opendaylight.controller.sal.authorization.AuthResultEnum;
import org.opendaylight.controller.sal.utils.Status;
import org.opendaylight.controller.sal.utils.StatusCode;
import org.opendaylight.controller.usermanager.AuthResponse;

/**
 * Configuration Java Object which represents a Local AAA user configuration
 * information for User Manager.
 */
public class UserConfig implements Serializable {
    private static final long serialVersionUID = 1L;

    /*
     * Clear text password as we are moving to some MD5 digest for when saving
     * configurations
     */
    protected String user;
    protected List<String> roles;
    private String password;
    private static final int USERNAME_MAXLENGTH = 32;
    private static final int PASSWORD_MINLENGTH = 5;
    private static final int PASSWORD_MAXLENGTH = 256;
    private static final Pattern INVALID_USERNAME_CHARACTERS = Pattern
            .compile("([/\\s\\.\\?#%;\\\\]+)");

    public UserConfig() {
    }

    public UserConfig(String user, String password, List<String> roles) {
        this.user = user;
        this.password = password;
        this.roles = (roles == null) ? new ArrayList<String>()
                : new ArrayList<String>(roles);
    }

    public String getUser() {
        return user;
    }

    public String getPassword() {
        return password;
    }

    public List<String> getRoles() {
        return new ArrayList<String>(roles);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((password == null) ? 0 : password.hashCode());
        result = prime * result + ((roles == null) ? 0 : roles.hashCode());
        result = prime * result + ((user == null) ? 0 : user.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        UserConfig other = (UserConfig) obj;
        if (password == null) {
            if (other.password != null)
                return false;
        } else if (!password.equals(other.password))
            return false;
        if (roles == null) {
            if (other.roles != null)
                return false;
        } else if (!roles.equals(other.roles))
            return false;
        if (user == null) {
            if (other.user != null)
                return false;
        } else if (!user.equals(other.user))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "UserConfig[user=" + user + ", password=" + password + ", roles=" + roles +"]";
    }

    public Status validate() {
        Status validCheck = validateRoles();
        if (validCheck.isSuccess()) {
            validCheck = validateUsername();
        }
        if (validCheck.isSuccess()) {
            validCheck = validatePassword();
        }
        return validCheck;
    }

    protected Status validateUsername() {
        if (user == null || user.isEmpty()) {
            return new Status(StatusCode.BADREQUEST, "Username cannot be empty");
        }

        Matcher mUser = UserConfig.INVALID_USERNAME_CHARACTERS.matcher(user);
        if (user.length() > UserConfig.USERNAME_MAXLENGTH
                || mUser.find() == true) {
            return new Status(StatusCode.BADREQUEST,
                    "Username can have 1-32 non-whitespace "
                            + "alphanumeric characters and any special "
                            + "characters except ./#%;?\\");
        }

        return new Status(StatusCode.SUCCESS);
    }

    private Status validatePassword() {
        if (password == null || password.isEmpty()) {
            return new Status(StatusCode.BADREQUEST, "Password cannot be empty");
        }

        if (password.length() < UserConfig.PASSWORD_MINLENGTH
                || password.length() > UserConfig.PASSWORD_MAXLENGTH) {
            return new Status(StatusCode.BADREQUEST,
                    "Password should have 5-256 characters");
        }
        return new Status(StatusCode.SUCCESS);
    }

    protected Status validateRoles() {
        if (roles == null || roles.isEmpty()) {
            return new Status(StatusCode.BADREQUEST, "No role specified");
        }
        return new Status(StatusCode.SUCCESS);
    }

    public Status update(String currentPassword, String newPassword,
            List<String> newRoles) {
        // To make any changes to a user configured profile, current password
        // must always be provided
        if (!this.password.equals(currentPassword)) {
            return new Status(StatusCode.BADREQUEST,
                    "Current password is incorrect");
        }

        // Create a new object with the proposed modifications
        UserConfig proposed = new UserConfig();
        proposed.user = this.user;
        proposed.password = (newPassword != null)? newPassword : this.password;
        proposed.roles = (newRoles != null)? newRoles : this.roles;

        // Validate it
        Status status = proposed.validate();
        if (!status.isSuccess()) {
            return status;
        }

        // Accept the modifications
        this.user = proposed.user;
        this.password = proposed.password;
        this.roles = new ArrayList<String>(proposed.roles);

        return status;
    }

    public AuthResponse authenticate(String clearTextPass) {
        AuthResponse locResponse = new AuthResponse();
        if (password.equals(clearTextPass)) {
            locResponse.setStatus(AuthResultEnum.AUTH_ACCEPT_LOC);
            locResponse.addData(getRolesString());
        } else {
            locResponse.setStatus(AuthResultEnum.AUTH_REJECT_LOC);
        }
        return locResponse;
    }

    protected String getRolesString() {
        StringBuffer buffer = new StringBuffer();
        if (!roles.isEmpty()) {
            Iterator<String> iter = roles.iterator();
            buffer.append(iter.next());
            while (iter.hasNext()) {
                buffer.append(" ");
                buffer.append(iter.next());
            }
        }
        return buffer.toString();
    }
}
