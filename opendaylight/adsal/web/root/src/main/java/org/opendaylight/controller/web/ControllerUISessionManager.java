/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.web;

import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import org.opendaylight.controller.sal.utils.ServiceHelper;
import org.opendaylight.controller.usermanager.IUserManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ControllerUISessionManager implements HttpSessionListener {

    private static final Logger logger = LoggerFactory.getLogger(ControllerUISessionManager.class);

    @Override
    public void sessionCreated(HttpSessionEvent se) {
        IUserManager userManager = (IUserManager) ServiceHelper.getGlobalInstance(IUserManager.class, this);
        if (userManager != null) {
            ((HttpSessionListener) userManager.getSessionManager()).sessionCreated(se);
        } else {
            logger.warn("User Manager is currently unavailable. Unable to register UI session.");
        }
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent se) {
        IUserManager userManager = (IUserManager) ServiceHelper.getGlobalInstance(IUserManager.class, this);
        if (userManager != null) {
            ((HttpSessionListener) userManager.getSessionManager()).sessionDestroyed(se);
        } else {
            logger.warn("User Manager is currently unavailable. Unable to destroy UI session.");
        }
    }

}
