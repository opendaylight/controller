/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */


package org.opendaylight.controller.web;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutHandler;

import org.opendaylight.controller.sal.utils.ServiceHelper;
import org.opendaylight.controller.usermanager.IUserManager;

public class ControllerLogoutHandler implements LogoutHandler {

    private static final Logger logger = LoggerFactory
            .getLogger(ControllerLogoutHandler.class);

    @Override
    public void logout(HttpServletRequest request,
            HttpServletResponse response, Authentication authentication) {
        if (authentication != null) {
            String userName = authentication.getName();
            if (userName != null) {
                IUserManager userManager = (IUserManager) ServiceHelper
                        .getGlobalInstance(IUserManager.class, this);
                if (userManager != null) {
                    userManager.userLogout(userName);
                    HttpSession session = request.getSession();
                    userManager.getSessionManager().invalidateSessions(userName, session.getId());
                    
                } else
                    logger
                            .error("UserMgr ref is null. Logout is not done cleanly");

            } else
                logger
                        .error("User name is null in authentication. Logout is not done cleanly");
        }

    }

}
