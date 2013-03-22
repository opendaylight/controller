
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.usermanager.security;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.web.context.HttpRequestResponseHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;

public class UserSecurityContextRepository implements
        SecurityContextRepository {

    HttpSessionSecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();

    @Override
    public SecurityContext loadContext(
            HttpRequestResponseHolder requestResponseHolder) {
        return securityContextRepository.loadContext(requestResponseHolder);
    }

    @Override
    public void saveContext(SecurityContext context,
            HttpServletRequest request, HttpServletResponse response) {
        securityContextRepository.saveContext(context, request, response);
    }

    @Override
    public boolean containsContext(HttpServletRequest request) {
        return securityContextRepository.containsContext(request);
    }

}
