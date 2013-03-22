/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.usermanager.internal;

import java.io.Serializable;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.opendaylight.controller.sal.authorization.AuthResultEnum;
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

	public boolean isValid() {
		return (user != null && !user.isEmpty() && role != null
				&& !role.isEmpty() && password != null && !password.isEmpty());
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
