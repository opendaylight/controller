package org.opendaylight.controller.security;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

import org.apache.catalina.realm.GenericPrincipal;
import org.apache.catalina.realm.RealmBase;
import org.opendaylight.controller.sal.authorization.AuthResultEnum;
import org.opendaylight.controller.sal.authorization.UserLevel;
import org.opendaylight.controller.sal.utils.ServiceHelper;
import org.opendaylight.controller.usermanager.IUserManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ControllerCustomRealm  extends RealmBase {

    private static final String name = "ControllerCustomRealm";

    private static final Logger logger = LoggerFactory
            .getLogger(ControllerCustomRealm.class);

    @Override
    protected String getName() {
        return name;
    }

    @Override
    protected String getPassword(String username) {
        IUserManager userManager = (IUserManager) ServiceHelper
                .getGlobalInstance(IUserManager.class, this);
        if (userManager != null) {
            return userManager.getPassword(username);
        } else
            throw new RuntimeException("User Manager reference is null");
    }

    @Override
    protected Principal getPrincipal(String username) {
        IUserManager userManager = (IUserManager) ServiceHelper
                .getGlobalInstance(IUserManager.class, this);
        if (userManager != null) {
            final List<String> levels = new ArrayList<String>(); 
            UserLevel level = userManager.getUserLevel(username);
            if (level == null) level = UserLevel.NOUSER;
            levels.add(level.toString());
            return new GenericPrincipal(username, "", levels);
        } else
            throw new RuntimeException("User Manager reference is null");

    }

    @Override
    public Principal authenticate(String username, String credentials) {

        IUserManager userManager = (IUserManager) ServiceHelper
                .getGlobalInstance(IUserManager.class, this);
        if (userManager != null) {
            AuthResultEnum result = userManager.authenticate(username,
                    credentials);
            if (result.equals(AuthResultEnum.AUTHOR_PASS)
                    || result.equals(AuthResultEnum.AUTH_ACCEPT_LOC)
                    || result.equals(AuthResultEnum.AUTH_ACCEPT)) {
                return this.getPrincipal(username);
            } else {
                logger.error("Authentication failed for user " + username);
                return null;
            }
        } else
            throw new RuntimeException("User Manager reference is null");
    }

}
