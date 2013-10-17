/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.usermanager;

import java.io.Serializable;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.opendaylight.controller.sal.authorization.AuthResultEnum;
import org.opendaylight.controller.sal.utils.HexEncode;
import org.opendaylight.controller.sal.utils.Status;
import org.opendaylight.controller.sal.utils.StatusCode;

/**
 * Configuration Java Object which represents a Local AAA user configuration
 * information for User Manager.
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
public class UserConfig implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * User Id
     */
    @XmlElement
    protected String user;

    /**
     * List of roles a user can have
     * example
     * System-Admin
     * Network-Admin
     * Netowrk-Operator
     */
    @XmlElement
    protected List<String> roles;

    /**
     * Password
     * Should be 8 to 256 characters long,
     * contain both upper and lower case letters, at least one number,
     * and at least one non alphanumeric character.
     */
    @XmlElement
    private String password;

    private static final boolean strongPasswordCheck = Boolean.getBoolean("enableStrongPasswordCheck");
    private static final String BAD_PASSWORD = "Bad Password";
    private static final int USERNAME_MAXLENGTH = 32;
    protected static final String PASSWORD_REGEX = "(?=.*[^a-zA-Z0-9])(?=.*\\d)(?=.*[a-z])(?=.*[A-Z]).{8,256}$";
    private static final Pattern INVALID_USERNAME_CHARACTERS = Pattern.compile("([/\\s\\.\\?#%;\\\\]+)");
    private static MessageDigest oneWayFunction = null;

    static {
        try {
            UserConfig.oneWayFunction = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    public UserConfig() {
    }

    /**
     * Construct a UserConfig object and takes care of hashing the user password
     *
     * @param user
     *            the user name
     * @param password
     *            the plain text password
     * @param roles
     *            the list of roles
     */
    public UserConfig(String user, String password, List<String> roles) {
        this.user = user;

        /*
         * Password validation to be done on clear text password. If fails, mark
         * the password with a well known label, so that object validation can
         * report the proper error. Only if password is a valid one, hash it.
         */
        this.password = (validatePassword(password).isSuccess()) ? hash(password) : BAD_PASSWORD;

        this.roles = (roles == null) ? new ArrayList<String>() : new ArrayList<String>(roles);
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
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        UserConfig other = (UserConfig) obj;
        if (password == null) {
            if (other.password != null) {
                return false;
            }
        } else if (!password.equals(other.password)) {
            return false;
        }
        if (roles == null) {
            if (other.roles != null) {
                return false;
            }
        } else if (!roles.equals(other.roles)) {
            return false;
        }
        if (user == null) {
            if (other.user != null) {
                return false;
            }
        } else if (!user.equals(other.user)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "UserConfig[user=" + user + ", password=" + password + ", roles=" + roles +"]";
    }

    public Status validate() {
        Status validCheck = validateUsername();
        if (validCheck.isSuccess()) {
            validCheck = (!password.equals(BAD_PASSWORD)) ? new Status(StatusCode.SUCCESS) : new Status(
                    StatusCode.BADREQUEST,
                    "Password should be 8 to 256 characters long, contain both upper and lower case letters, "
                            + "at least one number and at least one non alphanumeric character");
        }
        if (validCheck.isSuccess()) {
            validCheck = validateRoles();
        }
        return validCheck;
    }

    protected Status validateUsername() {
        if (user == null || user.isEmpty()) {
            return new Status(StatusCode.BADREQUEST, "Username cannot be empty");
        }

        Matcher mUser = UserConfig.INVALID_USERNAME_CHARACTERS.matcher(user);
        if (user.length() > UserConfig.USERNAME_MAXLENGTH || mUser.find() == true) {
            return new Status(StatusCode.BADREQUEST,
                    "Username can have 1-32 non-whitespace "
                            + "alphanumeric characters and any special "
                            + "characters except ./#%;?\\");
        }

        return new Status(StatusCode.SUCCESS);
    }

    private Status validatePassword(String password) {
        if (password == null || password.isEmpty()) {
            return new Status(StatusCode.BADREQUEST, "Password cannot be empty");
        }

        if (strongPasswordCheck && !password.matches(UserConfig.PASSWORD_REGEX)) {
            return new Status(StatusCode.BADREQUEST, "Password should be 8 to 256 characters long, "
                    + "contain both upper and lower case letters, at least one number "
                    + "and at least one non alphanumeric character");
        }
        return new Status(StatusCode.SUCCESS);
    }

    protected Status validateRoles() {
        if (roles == null || roles.isEmpty()) {
            return new Status(StatusCode.BADREQUEST, "No role specified");
        }
        return new Status(StatusCode.SUCCESS);
    }

    public Status update(String currentPassword, String newPassword, List<String> newRoles) {

        // To make any changes to a user configured profile, current password
        // must always be provided
        if (!this.password.equals(hash(currentPassword))) {
            return new Status(StatusCode.BADREQUEST, "Current password is incorrect");
        }

        // Create a new object with the proposed modifications
        UserConfig proposed = new UserConfig();
        proposed.user = this.user;
        proposed.password = (newPassword == null)? this.password : hash(newPassword);
        proposed.roles = (newRoles == null)? this.roles : newRoles;

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
        if (password.equals(hash(clearTextPass))) {
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

    public static String hash(String message) {
        if (message == null) {
            return message;
        }
        UserConfig.oneWayFunction.reset();
        return HexEncode.bytesToHexString(UserConfig.oneWayFunction.digest(message.getBytes(Charset.defaultCharset())));
    }

    /**
     * Returns UserConfig instance populated with the passed parameters. It does
     * not run any checks on the passed parameters.
     *
     * @param userName
     *            the user name
     * @param password
     *            the plain text password
     * @param roles
     *            the list of roles
     * @return the UserConfig object populated with the passed parameters. No
     *         validity check is run on the input parameters.
     */
    public static UserConfig getUncheckedUserConfig(String userName, String password, List<String> roles) {
        UserConfig config = new UserConfig();
        config.user = userName;
        config.password = hash(password);
        config.roles = roles;
        return config;
    }
}
