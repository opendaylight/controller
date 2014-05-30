package org.opendaylight.controller.usermanager.shell;

import org.apache.felix.gogo.commands.Command;
import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.opendaylight.controller.usermanager.IUserManagerShell;
import org.opendaylight.controller.usermanager.UserConfig;

@Command(scope = "usermanager", name = "getUsers", description="Gets the users within the usermanager")
public class UmGetUsers extends OsgiCommandSupport{

    private IUserManagerShell userManager;

    @Override
    protected Object doExecute() throws Exception {
        for (UserConfig conf : userManager.getLocalUserList()) {
            System.out.println(conf.getUser() + " " + conf.getRoles());
        }
        return null;
    }

    public void setUserManager(IUserManagerShell userManager){
        this.userManager = userManager;
    }
}