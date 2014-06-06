package org.opendaylight.controller.usermanager.shell;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.opendaylight.controller.usermanager.IUserManagerShell;
import org.opendaylight.controller.usermanager.ServerConfig;

@Command(scope = "usermanager", name = "removeAAAServer", description="Removes a AAA server from the usermanager")
public class RemoveAAAServer extends OsgiCommandSupport{

    private IUserManagerShell userManager;

    @Argument(index = 0, name = "arg0", description = "The first argument passed to the addAAAServer command", required = false, multiValued = false)
    String arg0 = null;

    @Argument(index = 1, name = "arg1", description = "The second argument passed to the addAAAServer command", required = false, multiValued = false)
    String arg1 = null;

    @Argument(index = 2, name = "arg2", description = "The third argument passed to the addAAAServer command", required = false, multiValued = false)
    String arg2 = null;

    @Override
    protected Object doExecute() throws Exception {
        String server = arg0;
        String secret = arg1;
        String protocol = arg2;

        if (server == null || secret == null || protocol == null) {
            System.out.println("Usage : addAAAServer <server> <secret> <protocol>");
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