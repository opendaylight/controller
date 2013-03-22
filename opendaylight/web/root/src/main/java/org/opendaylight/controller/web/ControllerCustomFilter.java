/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.web;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.opendaylight.controller.sal.utils.ServiceHelper;
import org.opendaylight.controller.usermanager.IUserManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.GenericFilterBean;

public class ControllerCustomFilter extends GenericFilterBean {

    private static final Logger logger = LoggerFactory
            .getLogger(ControllerCustomFilter.class);

 
    @Override
    public void doFilter(ServletRequest req, ServletResponse resp,
            FilterChain chain) throws IOException, ServletException {
        //custom filter to handle logged out users
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) resp;

        String url = request.getRequestURL().toString();

        //skip anonymous auth
        if (!(url.indexOf("login") > -1) && !(url.indexOf("logout") > -1)) {
            if (SecurityContextHolder.getContext().getAuthentication() != null
                    && SecurityContextHolder.getContext().getAuthentication()
                            .isAuthenticated()) {

                IUserManager userManager = (IUserManager) ServiceHelper
                        .getGlobalInstance(IUserManager.class, this);
                if (userManager != null) {
                    Map<String, List<String>> activeUsers = userManager
                            .getUserLoggedIn();
                    if (activeUsers != null && activeUsers.size() > 0) {

                        String username = SecurityContextHolder.getContext()
                                .getAuthentication().getName();
                        if (!activeUsers.containsKey(username)) {
                            throw new AccessDeniedException(
                                    "UserManager activeUserList does not contain user "
                                            + username);
                        }
                    } else {
                        logger.error("UserManager return empty activeusers");
                        throw new AccessDeniedException(
                                "UserManager activeUserList is empty. ");
                    }
                } else {
                    logger.error("UserManager Ref is null. ");
                    throw new RuntimeException("UserManager Ref is null. ");
                }

            } else {
                logger.error("SecurityContextHolder getAuthentication is null");
                throw new AccessDeniedException(
                        "SecurityContextHolder is not populated");
            }
        }

        chain.doFilter(request, response);
    }


}
