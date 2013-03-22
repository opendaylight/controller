
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.usermanager;

import org.opendaylight.controller.sal.authorization.UserLevel;
import org.springframework.security.core.GrantedAuthority;

public class ODLUserLevel implements GrantedAuthority {
	private static final long serialVersionUID = 1L;
	UserLevel userLevel;

    public ODLUserLevel(UserLevel userLevel) {
        this.userLevel = userLevel;
    }

    @Override
    public String getAuthority() {
        return "ROLE_" + this.userLevel.toString().toUpperCase();
    }

}
