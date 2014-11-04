/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.web;

import java.util.List;

import org.opendaylight.controller.usermanager.UserConfig;

public class UserBean {
    private String user;
    private List<String> roles;

    public UserBean(String user, List<String> roles) {
        this.user = user;
        this.roles = roles;
    }

    public UserBean(UserConfig config) {
        this(config.getUser(), config.getRoles());
    }

    public String getUser() {
        return user;
    }

    public List<String> getRoles() {
        return roles;
    }
}
