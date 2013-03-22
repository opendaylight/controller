/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.usermanager.internal;


/**
 * Configuration Java Object which represents a Local configured Authorization
 * for a remote authenticated user for User Manager.
 */
public class AuthorizationConfig extends UserConfig {
	private static final long serialVersionUID = 1L;

	public AuthorizationConfig() {
		super();
	}

	// Constructor may be needed for autocontainer logic
	public AuthorizationConfig(String user, String role) {
		super();
		this.user = user;
		this.role = role;
	}

	@Override
	public boolean isValid() {
		return (user != null && !user.isEmpty() && role != null && !role
				.isEmpty());
	}

	public String getRolesData() {
		return (role.replace(",", " "));
	}

	public String toString() {
		return "AuthorizationConfig=[user: " + user + ", role: " + role + "]";
	}
}
