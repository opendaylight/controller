/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.usermanager.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.opendaylight.controller.sal.authorization.AuthResultEnum;
import org.opendaylight.controller.sal.authorization.UserLevel;
import org.opendaylight.controller.usermanager.AuthResponse;

/*
 * This test case includes tests for UserConfig and the extending class AuthorizationConfig
 */
public class AuthorizationUserConfigTest {

	@Test
	public void AuthorizationConfigTest() {
		AuthorizationConfig authConfig;

		// test isValid
		authConfig = new AuthorizationConfig(null,
				UserLevel.SYSTEMADMIN.toString());
		assertFalse(authConfig.isValid());
		authConfig = new AuthorizationConfig("admin", "");
		assertFalse(authConfig.isValid());
		authConfig = new AuthorizationConfig("admin",
				UserLevel.SYSTEMADMIN.toString());
		assertTrue(authConfig.isValid());		
	}

	@Test
	public void UserConfigTest() {
		UserConfig userConfig;

		userConfig = new UserConfig(null, "cisco",
				UserLevel.NETWORKOPERATOR.toString());
		assertFalse(userConfig.isValid());

		userConfig = new UserConfig("uname", "", "cisco");
		assertFalse(userConfig.isValid());

		userConfig = new UserConfig("uname", "ciscocisco",
				UserLevel.NETWORKOPERATOR.toString());
		assertTrue(userConfig.isValid());

		/* currentPassword mismatch */
		assertFalse(userConfig.update("Cisco", "cisco123",
				UserLevel.NETWORKOPERATOR.toString()));

		assertTrue(userConfig.update("ciscocisco", null,
				UserLevel.NETWORKOPERATOR.toString()));
		/* New Password = null, No change in password */
		assertTrue(userConfig.getPassword().equals("ciscocisco"));

		/* Password changed successfully, no change in user role */
		assertTrue(userConfig.update("ciscocisco", "cisco123",
				UserLevel.NETWORKOPERATOR.toString()));
		assertTrue(userConfig.getPassword().equals("cisco123"));
		assertTrue(userConfig.getRole().equals(
				UserLevel.NETWORKOPERATOR.toString()));

		/* Password not changed, role changed successfully */
		assertTrue(userConfig.update("cisco123", "cisco123",
				UserLevel.SYSTEMADMIN.toString()));
		assertTrue(userConfig.getPassword().equals("cisco123"));
		assertTrue(userConfig.getRole()
				.equals(UserLevel.SYSTEMADMIN.toString()));

		/* Password and role changed successfully */
		assertTrue(userConfig.update("cisco123", "ciscocisco",
				UserLevel.SYSTEMADMIN.toString()));
		assertTrue(userConfig.getPassword().equals("ciscocisco"));
		assertTrue(userConfig.getRole()
				.equals(UserLevel.SYSTEMADMIN.toString()));

		String username = userConfig.getUser();
		assertTrue(username.equals("uname"));

		// test authenticate
		AuthResponse authresp = userConfig.authenticate("ciscocisco");
		assertTrue(authresp.getStatus().equals(AuthResultEnum.AUTH_ACCEPT_LOC));
		authresp = userConfig.authenticate("wrongPassword");
		assertTrue(authresp.getStatus().equals(AuthResultEnum.AUTH_REJECT_LOC));

		// test equals()
		userConfig = new UserConfig("uname", "ciscocisco",
				UserLevel.NETWORKOPERATOR.toString());
		assertEquals(userConfig, userConfig);
		UserConfig userConfig2 = new UserConfig("uname",
				"ciscocisco",
				UserLevel.NETWORKOPERATOR.toString());
		assertEquals(userConfig, userConfig2);
	}
}
