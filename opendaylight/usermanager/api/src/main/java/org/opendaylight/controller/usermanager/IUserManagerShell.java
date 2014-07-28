package org.opendaylight.controller.usermanager;
/**
* Copyright (c) 2014 Inocybe Technologies, and others. All rights reserved.
*
* This program and the accompanying materials are made available under the
* terms of the Eclipse Public License v1.0 which accompanies this distribution,
* and is available at http://www.eclipse.org/legal/epl-v10.html
*/
import java.util.concurrent.ConcurrentMap;

public interface IUserManagerShell extends IUserManager{
    public ConcurrentMap<String, ServerConfig> getRemoveServerConfigList();
    public void removeServer(ServerConfig AAAconf);
    public ConcurrentMap<String, UserConfig> getLocalUserConfigList();
}
