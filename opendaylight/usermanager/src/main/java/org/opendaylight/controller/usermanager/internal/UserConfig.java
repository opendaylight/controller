/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.usermanager.internal;

import java.io.Serializable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.opendaylight.controller.sal.authorization.AuthResultEnum;
import org.opendaylight.controller.sal.utils.Status;
import org.opendaylight.controller.sal.utils.StatusCode;
import org.opendaylight.controller.usermanager.AuthResponse;

/**
 * Configuration Java Object which represents a Local AAA user
 * configuration information for User Manager. 
 */
public class UserConfig implements Serializable {
	private static final long serialVersionUID = 1L;

	/*
	 * Clear text password as we are moving to some MD5 digest
	 * for when saving configurations
	 */
	protected String user;
	protected String role;
	private String password;
	private static final int USERNAME_MAXLENGTH = 32;
	private static final int PASSWORD_MINLENGTH = 5;
	private static final int PASSWORD_MAXLENGTH = 256;
	private static final Pattern INVALID_USERNAME_CHARACTERS = 
						 Pattern.compile("([/\\s\\.\\?#%;\\\\]+)");
	
	public UserConfig() {
	}

	public UserConfig(String user, String password, String role) {
		this.user = user;
		this.password = password;
		this.role = role;
	}

	public String getUser() {
		return user;
	}

	public String getPassword() {
		return password;
	}

	public String getRole() {
		return role;
	}

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    @Override
    public boolean equals(Object obj) {
        return EqualsBuilder.reflectionEquals(this, obj);
    }
    
    @Override
    public String toString() {
    	return "UserConfig[user="+ user + ", password=" + password + "]";
    }

	public Status validate() {
		Status validCheck = new Status(StatusCode.SUCCESS, null);
		validCheck = isRoleValid();
		
		if (validCheck.isSuccess())
			validCheck = isUsernameValid();
		if (validCheck.isSuccess()) 
			validCheck = isPasswordValid();
		
		return validCheck;
	}
	
	protected Status isUsernameValid() {
		if (user == null || user.isEmpty()) {
			return new Status(StatusCode.BADREQUEST,
							  "Username cannot be empty");
		}
		
		Matcher mUser = UserConfig.INVALID_USERNAME_CHARACTERS.matcher(user);
		if (user.length() > UserConfig.USERNAME_MAXLENGTH 
								|| mUser.find() == true) {
			return new Status(StatusCode.BADREQUEST, 
							 "Username can have 1-32 non-whitespace " +
							 "alphanumeric characters and any special " +
							 "characters except ./#%;?\\");
		}
			
		return new Status(StatusCode.SUCCESS, null);
	}
	
	private Status isPasswordValid() {
		if (password == null || password.isEmpty()) {
			return new Status(StatusCode.BADREQUEST,
							 "Password cannot be empty");
		}

		if (password.length() < UserConfig.PASSWORD_MINLENGTH || 
			password.length() > UserConfig.PASSWORD_MAXLENGTH) {
			return new Status(StatusCode.BADREQUEST, 
							  "Password should have 5-256 characters");
		}
		return new Status(StatusCode.SUCCESS, null);
	}
	
	protected Status isRoleValid() {
		if (role == null || role.isEmpty()) {
			return new Status(StatusCode.BADREQUEST, 
							 "Role name cannot be empty");
		}
		return new Status(StatusCode.SUCCESS, null);
	}

	public boolean update(String currentPassword, String newPassword,
			String newRole) {
		// To make any changes to a user configured profile, current password
		// must always be provided
		if (!this.password.equals(currentPassword)) {
			return false;
		}
		if (newPassword != null) {
			this.password = newPassword;
		}
		if (newRole != null) {
			this.role = newRole;
		}
		return true;
	}

	public AuthResponse authenticate(String clearTextPass) {
		AuthResponse locResponse = new AuthResponse();
		if (password.equals(clearTextPass)) {
			locResponse.setStatus(AuthResultEnum.AUTH_ACCEPT_LOC);
			locResponse.addData(role.replace(",", " "));
		} else {
			locResponse.setStatus(AuthResultEnum.AUTH_REJECT_LOC);
		}
		return locResponse;
	}
}
