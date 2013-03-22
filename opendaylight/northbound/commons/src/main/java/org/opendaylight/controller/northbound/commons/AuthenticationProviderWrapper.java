/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.northbound.commons;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

import org.opendaylight.controller.sal.utils.ServiceHelper;
import org.opendaylight.controller.usermanager.IUserManager;

import org.opendaylight.controller.northbound.commons.exception.InternalServerErrorException;

public class AuthenticationProviderWrapper implements AuthenticationProvider {

    private static final Logger logger = LoggerFactory
            .getLogger(AuthenticationProviderWrapper.class);

    @Override
    public Authentication authenticate(Authentication authentication)
            throws AuthenticationException {
        return ((AuthenticationProvider) getUserManagerRef())
                .authenticate(authentication);
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return ((AuthenticationProvider) getUserManagerRef())
                .supports(authentication);
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

}
