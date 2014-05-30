package org.opendaylight.controller.usermanager.shell;

import org.apache.felix.gogo.commands.Command;
import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.opendaylight.controller.usermanager.IUserManagerShell;
import org.opendaylight.controller.usermanager.ServerConfig;
import org.apache.felix.gogo.commands.Argument;

@Command(scope = "usermanager", name = "addAAAServer", description="Add a AAA server to the usermanager")
public class AddAAAServer extends OsgiCommandSupport{

    private IUserManagerShell userManager;

    @Argument(index = 0, name = "arg0", description = "The first argument passed to the addAAAServer command", required = false, multiValued = false)
    String arg0 = null;

    @Argument(index = 0, name = "arg1", description = "The second argument passed to the addAAAServer command", required = false, multiValued = false)
    String arg1 = null;

    @Argument(index = 0, name = "arg2", description = "The third argument passed to the addAAAServer command", required = false, multiValued = false)
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
        userManager.addAAAServer(s);
        return null;
    }

    public void setUserManager(IUserManagerShell userManager){
        this.userManager = userManager;
    }
}