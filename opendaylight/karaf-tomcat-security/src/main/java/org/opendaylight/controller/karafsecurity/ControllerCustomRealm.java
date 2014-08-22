/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.karafsecurity;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

import org.apache.catalina.realm.GenericPrincipal;
import org.apache.catalina.realm.RealmBase;

public class ControllerCustomRealm  extends RealmBase {

    private static final String name = "ControllerCustomRealm";

    @Override
    protected String getName() {
        return name;
    }

    @Override
    protected String getPassword(String username) {
        return "admin";
    }

    @Override
    protected Principal getPrincipal(String username) {
        List<String> controllerRoles = new ArrayList<String>();
        controllerRoles.add("System-Admin");
        return new GenericPrincipal(username, "", controllerRoles);
    }

    @Override
    public Principal authenticate(String username, String credentials) {
        return this.getPrincipal(username);
    }
}
