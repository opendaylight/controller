/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.northbound.commons;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.web.context.HttpRequestResponseHolder;
import org.springframework.security.web.context.SecurityContextRepository;

import org.opendaylight.controller.sal.utils.ServiceHelper;
import org.opendaylight.controller.usermanager.IUserManager;

import org.opendaylight.controller.northbound.commons.exception.InternalServerErrorException;

public class WebSecurityContextRepository implements SecurityContextRepository {

    private static final Logger logger = LoggerFactory
            .getLogger(WebSecurityContextRepository.class);

    WebSecurityContextRepository() {
    }

    @Override
    public SecurityContext loadContext(
            HttpRequestResponseHolder requestResponseHolder) {

        SecurityContextRepository contextRepo = (SecurityContextRepository) getUserManagerRef()
                .getSecurityContextRepo();
        return contextRepo.loadContext(requestResponseHolder);
    }

    @Override
    public void saveContext(SecurityContext context,
            HttpServletRequest request, HttpServletResponse response) {
        SecurityContextRepository contextRepo = (SecurityContextRepository) getUserManagerRef()
                .getSecurityContextRepo();
        contextRepo.saveContext(context, request, response);
    }

    private IUserManager getUserManagerRef() {
        IUserManager userManager = (IUserManager) ServiceHelper
                .getGlobalInstance(IUserManager.class, this);
        if (userManager != null) {
            return userManager;
        } else {
            logger.error("UserManager Ref is null. ");
            throw new InternalServerErrorException("UserManager Ref is null. ");
        }
    }

    @Override
    public boolean containsContext(HttpServletRequest request) {
        SecurityContextRepository contextRepo = (SecurityContextRepository) getUserManagerRef()
                .getSecurityContextRepo();
        return contextRepo.containsContext(request);
    }

}
