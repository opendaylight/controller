
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

import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.controller.sal.authorization.AuthResultEnum;
import org.opendaylight.controller.sal.authorization.UserLevel;
import org.opendaylight.controller.sal.utils.ServiceHelper;
import org.opendaylight.controller.usermanager.AuthResponse;
import org.opendaylight.controller.usermanager.IAAAProvider;
import org.opendaylight.controller.usermanager.IUserManager;

/**
 * Unit Tests for UserManagerImpl
 */
public class UserManagerImplTest {

	private static UserManagerImpl um;

	/**
	 * @throws java.lang.Exception
	 */
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {

		IUserManager userManager = (IUserManager) ServiceHelper
				.getGlobalInstance(IUserManager.class, new Object());
		if (userManager instanceof UserManagerImpl) {
			um = (UserManagerImpl) userManager;
		} else {
			um = new UserManagerImpl();
			um.setAuthProviders(new ConcurrentHashMap<String, IAAAProvider>());

			// mock up a remote server list with a dummy server
			um.setRemoteServerConfigList(new ConcurrentHashMap<String, ServerConfig>() {
				static final long serialVersionUID = 1L;
				{
					put("dummyServerConfig", new ServerConfig() { // Server config can't be empty
								static final long serialVersionUID = 8645L;

								public String getAddress() {
									return "1.1.1.1";
								}

								public String getSecret() {
									return "secret";
								}

								public String getProtocol() {
									return "IPv4";
								}
							});
				}
			});

			// mock up a localUserConfigList with an admin user
			um.setLocalUserConfigList(new ConcurrentHashMap<String, UserConfig>() {
				static final long serialVersionUID = 2L;
				{
					put("admin", new UserConfig("admin", "7029,7455,8165,7029,7881",
									UserLevel.SYSTEMADMIN.toString()));
				}
			});
			// instantiate an empty activeUser collection
			um.setActiveUsers(new ConcurrentHashMap<String, AuthenticatedUser>());

		}

	}

	/**
	 * Test method for
	 * {@link org.opendaylight.controller.usermanager.internal.UserManagerImpl#addAAAProvider(org.opendaylight.controller.usermanager.IAAAProvider)}
	 * .
	 */
	@Test
	public void testAddAAAProvider() {
		// instantiate an anonymous AAAProvider
		IAAAProvider a3p = new IAAAProvider() {

			public AuthResponse authService(String userName, String password,
					String server, String secretKey) {
				return new AuthResponse();
			};

			public String getName() {
				return "dummyAAAProvider";
			}
		};

		um.addAAAProvider(a3p);
		assertEquals(a3p, um.getAAAProvider("dummyAAAProvider"));

	}

	/**
	 * Test method for
	 * {@link org.opendaylight.controller.usermanager.internal.UserManagerImpl#removeAAAProvider(org.opendaylight.controller.usermanager.IAAAProvider)}
	 * and for for
	 * {@link org.opendaylight.controller.usermanager.internal.UserManagerImpl#getAAAProvider(java.lang.String)}
	 * .
	 */
	@Test
	public void testRemoveAAAProvider() {
		um.removeAAAProvider(um.getAAAProvider("dummyAAAProvider"));
		assertTrue(um.getAAAProviderNames().isEmpty());
	}

	/**
	 * Test method for
	 * {@link org.opendaylight.controller.usermanager.internal.UserManagerImpl#authenticate(java.lang.String, java.lang.String)}
	 * .
	 */
	@Test
	public void testAuthenticateStringString() {
		UserConfig uc = new UserConfig("administrator", "admin",
				UserLevel.SYSTEMADMIN.toString());
		um.addLocalUser(uc);
		AuthResultEnum authResult = um.authenticate("administrator", "admin");
		assertEquals(authResult, AuthResultEnum.AUTH_ACCEPT_LOC);
	}

	/**
	 * Test method for
	 * {@link org.opendaylight.controller.usermanager.internal.UserManagerImpl#addRemoveLocalUser(org.opendaylight.controller.usermanager.internal.UserConfig, boolean)}
	 * .
	 */
	@Test
	public void testAddRemoveLocalUser() {
		UserConfig uc = new UserConfig("sysadmin", "7029,7455,8165,7029,7881",
				UserLevel.SYSTEMADMIN.toString());
		um.addLocalUser(uc);
		assertTrue(um.getLocalUserList().contains(uc));
		um.removeLocalUser(uc);
		assertFalse(um.getLocalUserList().contains(uc));
	}

	/**
	 * Test method for
	 * {@link org.opendaylight.controller.usermanager.internal.UserManagerImpl#changeLocalUserPassword(java.lang.String, java.lang.String, java.lang.String)}
	 * .
	 */
	@Test
	public void testChangeLocalUserPassword() {
		// fail("Not yet implemented");
	}

	/**
	 * Test method for
	 * {@link org.opendaylight.controller.usermanager.internal.UserManagerImpl#userLogout(java.lang.String)}
	 * .
	 */
	@Test
	public void testUserLogout() {
		// fail("Not yet implemented");
	}

	/**
	 * Test method for
	 * {@link org.opendaylight.controller.usermanager.internal.UserManagerImpl#userTimedOut(java.lang.String)}
	 * .
	 */
	@Test
	public void testUserTimedOut() {
		// fail("Not yet implemented");
	}

	/**
	 * Test method for
	 * {@link org.opendaylight.controller.usermanager.internal.UserManagerImpl#authenticate(org.springframework.security.core.Authentication)}
	 * .
	 */
	@Test
	public void testAuthenticateAuthentication() {
		// fail("Not yet implemented");
	}

	/**
	 * Test method for
	 * {@link org.opendaylight.controller.usermanager.internal.UserManagerImpl#saveLocalUserList()}
	 * .
	 */
	@Test
	public void testSaveLocalUserList() {
		// fail("Not yet implemented");
	}

	/**
	 * Test method for
	 * {@link org.opendaylight.controller.usermanager.internal.UserManagerImpl#saveAAAServerList()}
	 * .
	 */
	@Test
	public void testSaveAAAServerList() {
		// fail("Not yet implemented");
	}

	/**
	 * Test method for
	 * {@link org.opendaylight.controller.usermanager.internal.UserManagerImpl#saveAuthorizationList()}
	 * .
	 */
	@Test
	public void testSaveAuthorizationList() {
		// fail("Not yet implemented");
	}

	/**
	 * Test method for
	 * {@link org.opendaylight.controller.usermanager.internal.UserManagerImpl#readObject(java.io.ObjectInputStream)}
	 * .
	 */
	@Test
	public void testReadObject() {
		// fail("Not yet implemented");
	}
	
	@Test
	public void testGetUserLevel() {
		um.addLocalUser(new UserConfig("Jack", "password",
				UserLevel.SYSTEMADMIN.toString()));
		um.authenticate("Jack", "password");
		
		um.addLocalUser(new UserConfig("John", "password",
				UserLevel.NETWORKOPERATOR.toString()));
		// Run the check on authenticated user
		Assert.assertTrue(um.getUserLevel("Jack") == UserLevel.SYSTEMADMIN);
		// Run the check on configured users
		Assert.assertTrue(um.getUserLevel("John") == UserLevel.NETWORKOPERATOR);
		Assert.assertTrue(um.getUserLevel("Andrew") == UserLevel.NOUSER);
	}
}
