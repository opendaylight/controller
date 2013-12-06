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