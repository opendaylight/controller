package org.opendaylight.controller.usermanager.shell;
/**
* Copyright (c) 2014 Inocybe Technologies, and others. All rights reserved.
*
* This program and the accompanying materials are made available under the
* terms of the Eclipse Public License v1.0 which accompanies this distribution,
* and is available at http://www.eclipse.org/legal/epl-v10.html
*/
import java.util.ArrayList;
import java.util.List;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.opendaylight.controller.usermanager.IUserManagerShell;
import org.opendaylight.controller.usermanager.UserConfig;

@Command(scope = "usermanager", name = "addUser", description="Adds a user to the usermanager")
public class UmAddUser extends OsgiCommandSupport{

    private IUserManagerShell userManager;

    @Argument(index = 0, name = "arg0", description = "user_name", required = true, multiValued = false)
    String arg0 = null;

    @Argument(index = 1, name = "arg1", description = "password", required = true, multiValued = false)
    String arg1 = null;

    @Argument(index = 2, name = "arg2", description = "user_role", required = true, multiValued = false)
    String arg2 = null;

    @Override
    protected Object doExecute() throws Exception {
        String userName = arg0;
        String password = arg1;
        String role = arg2;

        List<String> roles = new ArrayList<String>();
        while (role != null) {
            if (!role.trim().isEmpty()) {
                roles.add(role);
            }
            role = arg2;
        }

        if (userName == null || userName.trim().isEmpty() || password == null || password.trim().isEmpty()
                || roles.isEmpty()) {
            System.out.println("Invalid Arguments");
            System.out.println("umAddUser <user_name> <password> <user_role>");
            return null;
        }
        System.out.print(userManager.addLocalUser(new UserConfig(userName, password, roles)));
        return null;
    }

    public void setUserManager(IUserManagerShell userManager){
        this.userManager = userManager;
    }
}