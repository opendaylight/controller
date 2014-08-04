package org.opendaylight.controller.usermanager.shell;
/**
* Copyright (c) 2014 Inocybe Technologies, and others. All rights reserved.
*
* This program and the accompanying materials are made available under the
* terms of the Eclipse Public License v1.0 which accompanies this distribution,
* and is available at http://www.eclipse.org/legal/epl-v10.html
*/
import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.opendaylight.controller.usermanager.IUserManagerShell;
import org.opendaylight.controller.usermanager.ServerConfig;

@Command(scope = "usermanager", name = "removeAAAServer", description="Removes a AAA server from the usermanager")
public class RemoveAAAServer extends OsgiCommandSupport{

    private IUserManagerShell userManager;

    @Argument(index = 0, name = "arg0", description = "server", required = true, multiValued = false)
    String arg0 = null;

    @Argument(index = 1, name = "arg1", description = "server secret", required = true, multiValued = false)
    String arg1 = null;

    @Argument(index = 2, name = "arg2", description = "protocol", required = true, multiValued = false)
    String arg2 = null;

    @Override
    protected Object doExecute() throws Exception {
        String server = arg0;
        String secret = arg1;
        String protocol = arg2;

        if (server == null || secret == null || protocol == null) {
            System.out.println("Usage : removeAAAServer <server> <secret> <protocol>");
            return null;
        }
        ServerConfig s = new ServerConfig(server, secret, protocol);
        userManager.removeServer(s);
        return null;
    }

    public void setUserManager(IUserManagerShell userManager){
        this.userManager = userManager;
    }
}