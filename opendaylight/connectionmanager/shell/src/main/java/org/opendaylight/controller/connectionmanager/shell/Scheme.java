package org.opendaylight.controller.connectionmanager.shell;
/**
* Copyright (c) 2014 Inocybe Technologies, and others. All rights reserved.
*
* This program and the accompanying materials are made available under the
* terms of the Eclipse Public License v1.0 which accompanies this distribution,
* and is available at http://www.eclipse.org/legal/epl-v10.html
*/

import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Argument;
import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.opendaylight.controller.connectionmanager.IConnectionManagerShell;

@Command(scope = "connectionmanager", name = "scheme", description="Affect a scheme passed in parameter")
public class Scheme extends OsgiCommandSupport{

    private IConnectionManagerShell connectionManager;
    @Argument(index = 0, name = "argument", description = "The argument passed to the scheme command", required = false, multiValued = false)
    String name = null;

    @Override
    protected Object doExecute() throws Exception {

        String scheme = connectionManager.setScheme(name);

        if (name == null){
            System.out.println("Please enter valid Scheme name");
            if (scheme != null){
                System.out.println("Current Scheme : " + scheme);
            }
        }
        return null;
    }

    public void setConnectionManager(IConnectionManagerShell connectionManager){
        this.connectionManager = connectionManager;
    }
}