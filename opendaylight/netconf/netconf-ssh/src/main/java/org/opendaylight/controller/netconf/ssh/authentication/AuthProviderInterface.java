
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.ssh.authentication;

import org.opendaylight.controller.usermanager.IUserManager;

public interface AuthProviderInterface {

    public boolean authenticated(String username, String password) throws Exception;
    public char[] getPEMAsCharArray() throws Exception;
    public void removeUserManagerService();
    public void addUserManagerService(IUserManager userManagerService);
}
