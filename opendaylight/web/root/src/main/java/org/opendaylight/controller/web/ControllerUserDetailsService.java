/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */


package org.opendaylight.controller.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import org.opendaylight.controller.sal.utils.ServiceHelper;
import org.opendaylight.controller.usermanager.IUserManager;


public class ControllerUserDetailsService implements UserDetailsService {

    private static final Logger logger = LoggerFactory
            .getLogger(ControllerUserDetailsService.class);

    ControllerUserDetailsService() {
    }

    @Override
    public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException {
        return getUserManagerRef().loadUserByUsername(username);
    }

    private IUserManager getUserManagerRef() {
        IUserManager userManager = (IUserManager) ServiceHelper
                .getGlobalInstance(IUserManager.class, this);
        if (userManager != null) {
            return userManager;
        } else {
            logger.error("UserManager Ref is null. ");
            throw new RuntimeException("UserManager Ref is null. ");
        }
    }

}
