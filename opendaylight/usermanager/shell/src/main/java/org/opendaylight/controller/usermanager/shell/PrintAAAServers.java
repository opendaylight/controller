package org.opendaylight.controller.usermanager.shell;

import org.apache.felix.gogo.commands.Command;
import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.opendaylight.controller.usermanager.IUserManagerShell;
import org.opendaylight.controller.usermanager.ServerConfig;

@Command(scope = "usermanager", name = "printAAAServers", description="Shows the list of AAA servers in the usermanager")
public class PrintAAAServers extends OsgiCommandSupport{

    private IUserManagerShell userManager;

    @Override
    protected Object doExecute() throws Exception {
        for (ServerConfig aaaServer : userManager.getRemoveServerConfigList().values()) {
            System.out.println(aaaServer.getAddress() + "-" + aaaServer.getProtocol());
        }
        return null;
    }

    public void setUserManager(IUserManagerShell userManager){
        this.userManager = userManager;
    }
}