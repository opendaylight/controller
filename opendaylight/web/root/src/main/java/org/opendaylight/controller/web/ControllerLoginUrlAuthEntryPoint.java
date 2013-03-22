/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */


package org.opendaylight.controller.web;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.RedirectUrlBuilder;

@SuppressWarnings("deprecation")
public class ControllerLoginUrlAuthEntryPoint extends
        LoginUrlAuthenticationEntryPoint {

    private String loginFormUrl = "/login";
    private final RedirectStrategy redirectStrategy = new DefaultRedirectStrategy();

    //This entry point always re-directs to root login page.

   @Override
   public void commence(HttpServletRequest request,
            HttpServletResponse response, AuthenticationException authException)
            throws IOException, ServletException {

        String redirectUrl = request.getRequestURL().toString();
            RedirectUrlBuilder urlBuilder = new RedirectUrlBuilder();
            urlBuilder.setScheme(request.getScheme());
            urlBuilder.setServerName(request.getServerName());
            urlBuilder.setPort(getPortResolver().getServerPort(request));
            // urlBuilder.setContextPath(request.getContextPath());
            urlBuilder.setPathInfo(loginFormUrl);
            redirectUrl = urlBuilder.getUrl();
            redirectStrategy.sendRedirect(request, response, redirectUrl);  

    }

}
