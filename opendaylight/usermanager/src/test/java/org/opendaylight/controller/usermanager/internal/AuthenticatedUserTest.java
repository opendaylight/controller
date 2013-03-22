/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.usermanager.internal;

import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import org.opendaylight.controller.sal.authorization.UserLevel;
import org.springframework.security.core.GrantedAuthority;

public class AuthenticatedUserTest {

	static String[] roleArray;
	static AuthenticatedUser user;

	@BeforeClass
	public static void testSetup() {
		roleArray = new String[] { UserLevel.NETWORKOPERATOR.toString(),
				UserLevel.APPUSER.toString() };
	}

	@Test
	public void testAuthenticatedUser() {
		user = new AuthenticatedUser("auser");

		Assert.assertFalse(user.getAccessDate().isEmpty());
		Assert.assertNull(user.getUserRoles());

	}

	@Test
	public void testSetUserRoleList() {
		List<String> retrievedRoleList = null;
		List<String> roleList = Arrays.asList(roleArray);

		// list arg
		user = new AuthenticatedUser("auser");
		user.setRoleList(roleList);
		retrievedRoleList = user.getUserRoles();
		Assert.assertTrue(roleList.equals(retrievedRoleList));

		// array arg
		user = new AuthenticatedUser("auser");
		user.setRoleList(roleArray);
		retrievedRoleList = user.getUserRoles();
		for (int i = 0; i < roleArray.length; i++)
			Assert.assertTrue(roleArray[i].equals(retrievedRoleList.get(i)));

		// test addUserRole
		user.addUserRole("AnotherRole");
		Assert.assertTrue(user.getUserRoles().lastIndexOf("AnotherRole") != -1);

	}

	@Test
	public void testGetGrantedAuthorities() {
		List<GrantedAuthority> gaList = user
				.getGrantedAuthorities(UserLevel.NETWORKOPERATOR);
		Assert.assertTrue(gaList.get(0).getAuthority()
				.equals("ROLE_NETWORK-OPERATOR"));
	}

}
