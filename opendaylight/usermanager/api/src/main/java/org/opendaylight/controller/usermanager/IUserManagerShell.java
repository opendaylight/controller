package org.opendaylight.controller.usermanager;

import java.util.concurrent.ConcurrentMap;

public interface IUserManagerShell extends IUserManager{
    public ConcurrentMap<String, ServerConfig> getRemoveServerConfigList();
    public void removeServer(ServerConfig AAAconf);
    public ConcurrentMap<String, UserConfig> getLocalUserConfigList();
}
