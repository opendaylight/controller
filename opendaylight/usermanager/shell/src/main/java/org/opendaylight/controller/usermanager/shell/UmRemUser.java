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
import org.opendaylight.controller.usermanager.UserConfig;

@Command(scope = "usermanager", name = "remUser", description="Removes a user from the usermanager")
public class UmRemUser extends OsgiCommandSupport{

    private IUserManagerShell userManager;

    @Argument(index = 0, name = "arg0", description = "The first argument passed to the addUser command", required = false, multiValued = false)
    String arg0 = null;

    @Override
    protected Object doExecute() throws Exception {
        String userName = arg0;

        if (userName == null || userName.trim().isEmpty()) {
            System.out.println("Invalid Arguments");
            System.out.println("umRemUser <user_name>");
            return null;
        }
        UserConfig target = userManager.getLocalUserConfigList().get(userName);
        if (target == null) {
            System.out.println("User not found");
            return null;
        }
        System.out.println(userManager.removeLocalUser(target));
        return null;
    }

    public void setUserManager(IUserManagerShell userManager){
        this.userManager = userManager;
    }
}