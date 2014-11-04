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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.controller.sal.authorization.AuthResultEnum;
import org.opendaylight.controller.sal.authorization.UserLevel;
import org.opendaylight.controller.sal.utils.ServiceHelper;
import org.opendaylight.controller.usermanager.AuthResponse;
import org.opendaylight.controller.usermanager.AuthenticatedUser;
import org.opendaylight.controller.usermanager.IAAAProvider;
import org.opendaylight.controller.usermanager.IUserManager;
import org.opendaylight.controller.usermanager.ServerConfig;
import org.opendaylight.controller.usermanager.UserConfig;
import org.opendaylight.controller.usermanager.AuthorizationConfig;

/**
 * Unit Tests for UserManager
 */
public class UserManagerImplTest {

    private static UserManager um;

    /**
     * @throws java.lang.Exception
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {

        IUserManager userManager = (IUserManager) ServiceHelper
                .getGlobalInstance(IUserManager.class, new Object());
        if (userManager instanceof UserManager) {
            um = (UserManager) userManager;
        } else {
            um = new UserManager();
            um.setAuthProviders(new ConcurrentHashMap<String, IAAAProvider>());

            // mock up a remote server list with a dummy server
            um.setRemoteServerConfigList(new ConcurrentHashMap<String, ServerConfig>() {
                static final long serialVersionUID = 1L;
                {
                    put("dummyServerConfig", new ServerConfig() {
                        // Server config can't be empty
                        static final long serialVersionUID = 8645L;

                        @Override
                        public String getAddress() {
                            return "1.1.1.1";
                        }

                        @Override
                        public String getSecret() {
                            return "secret";
                        }

                        @Override
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
                    List<String> roles = new ArrayList<String>(1);
                    roles.add(UserLevel.SYSTEMADMIN.toString());
                    put("admin", new UserConfig("admin",
                            "7029,7455,8165,7029,7881", roles));
                }
            });

            um.setAuthorizationConfList(new ConcurrentHashMap<String, AuthorizationConfig>() {
                static final long serialVersionUID = 2L;
                {
                    List<String> roles = new ArrayList<String>(3);
                    roles.add(UserLevel.NETWORKOPERATOR.toString());
                    roles.add("Container1-Admin");
                    roles.add("Application2-User");

                    put("Andrew", new AuthorizationConfig("Andrew", roles));
                }
            });
            // instantiate an empty activeUser collection
            um.setActiveUsers(new ConcurrentHashMap<String, AuthenticatedUser>());
        }
    }

    private IAAAProvider getAnonymousAAAProvider(final String providerName) {
        // instantiate an anonymous AAAProvider
        return new IAAAProvider() {

            @Override
            public AuthResponse authService(String userName, String password,
                    String server, String secretKey) {
                return new AuthResponse();
            };

            @Override
            public String getName() {
                return providerName;
            }
        };
    }

    /**
     * Test method for
     * {@link org.opendaylight.controller.usermanager.internal.UserManager#addAAAProvider(org.opendaylight.controller.usermanager.IAAAProvider)}
     * and for
     * {@link org.opendaylight.controller.usermanager.internal.UserManager#removeAAAProvider(org.opendaylight.controller.usermanager.IAAAProvider)}
     * and
     * {@link org.opendaylight.controller.usermanager.internal.UserManager#getAAAProvider(java.lang.String)}
     */
    @Test
    public void testAddGetRemoveAAAProvider() {
        final String providerName = "dummyAAAProvider";
        IAAAProvider a3p = getAnonymousAAAProvider(providerName);
        um.addAAAProvider(a3p);
        assertEquals(a3p, um.getAAAProvider(providerName));
        um.removeAAAProvider(um.getAAAProvider(providerName));
        assertTrue(um.getAAAProviderNames().isEmpty());
    }

    /**
     * Test method for
     * {@link org.opendaylight.controller.usermanager.internal.UserManager#authenticate(java.lang.String, java.lang.String)}
     * .
     */
    @Test
    public void testAuthenticateStringString() {
        List<String> roles = new ArrayList<String>(1);
        roles.add(UserLevel.SYSTEMADMIN.toString());
        UserConfig uc = new UserConfig("administrator", "admin", roles);
        um.addLocalUser(uc);
        AuthResultEnum authResult = um.authenticate("administrator", "admin");
        assertEquals(authResult, AuthResultEnum.AUTH_ACCEPT_LOC);
    }

    /**
     * Test method for
     * {@link org.opendaylight.controller.usermanager.internal.UserManager#addRemoveLocalUser(org.opendaylight.controller.usermanager.org.opendaylight.controller.usermanager.internal.UserConfig, boolean)}
     * .
     */
    @Test
    public void testAddRemoveLocalUser() {
        List<String> roles = new ArrayList<String>(1);
        roles.add(UserLevel.SYSTEMADMIN.toString());
        UserConfig uc = new UserConfig("sysadmin", "7029,7455,8165,7029,7881",
                roles);
        um.addLocalUser(uc);
        assertTrue(um.getLocalUserList().contains(uc));
        um.removeLocalUser(uc);
        assertFalse(um.getLocalUserList().contains(uc));
    }

    /**
     * Test method for
     * {@link org.opendaylight.controller.usermanager.internal.UserManager#changeLocalUserPassword(java.lang.String, java.lang.String, java.lang.String)}
     * .
     */
    @Test
    public void testChangeLocalUserPassword() {
        // fail("Not yet implemented");
    }

    /**
     * Test method for
     * {@link org.opendaylight.controller.usermanager.internal.UserManager#userLogout(java.lang.String)}
     * .
     */
    @Test
    public void testUserLogout() {
        // fail("Not yet implemented");
    }

    /**
     * Test method for
     * {@link org.opendaylight.controller.usermanager.internal.UserManager#userTimedOut(java.lang.String)}
     * .
     */
    @Test
    public void testUserTimedOut() {
        // fail("Not yet implemented");
    }

    /**
     * Test method for
     * {@link org.opendaylight.controller.usermanager.internal.UserManager#authenticate(org.springframework.security.core.Authentication)}
     * .
     */
    @Test
    public void testAuthenticateAuthentication() {
        // fail("Not yet implemented");
    }

    /**
     * Test method for
     * {@link org.opendaylight.controller.usermanager.internal.UserManager#saveLocalUserList()}
     * .
     */
    @Test
    public void testSaveLocalUserList() {
        // fail("Not yet implemented");
    }

    /**
     * Test method for
     * {@link org.opendaylight.controller.usermanager.internal.UserManager#saveAAAServerList()}
     * .
     */
    @Test
    public void testSaveAAAServerList() {
        // fail("Not yet implemented");
    }

    /**
     * Test method for
     * {@link org.opendaylight.controller.usermanager.internal.UserManager#saveAuthorizationList()}
     * .
     */
    @Test
    public void testSaveAuthorizationList() {
        // fail("Not yet implemented");
    }

    /**
     * Test method for
     * {@link org.opendaylight.controller.usermanager.internal.UserManager#readObject(java.io.ObjectInputStream)}
     * .
     */
    @Test
    public void testReadObject() {
        // fail("Not yet implemented");
    }

    @Test
    public void testGetUserLevel() {
        List<String> roles = new ArrayList<String>(2);
        roles.add(UserLevel.SYSTEMADMIN.toString());
        roles.add("App1_supervisor");
        um.addLocalUser(new UserConfig("Jack", "password", roles));
        um.authenticate("Jack", "password");

        roles.clear();
        roles.add("App2Admin");
        roles.add(UserLevel.NETWORKOPERATOR.toString());
        um.addLocalUser(new UserConfig("John", "password", roles));

        // Run the check on authenticated user
        Assert.assertTrue(um.getUserLevel("Jack") == UserLevel.SYSTEMADMIN);
        // Run the check on configured users
        Assert.assertTrue(um.getUserLevel("John") == UserLevel.NETWORKOPERATOR);
        // Run the check on local authorized users
        Assert.assertTrue(um.getUserLevel("Andrew") == UserLevel.NETWORKOPERATOR);
        // Non locally known user
        Assert.assertTrue(um.getUserLevel("Tom") == UserLevel.NOUSER);
    }
}
