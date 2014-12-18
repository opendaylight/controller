/**
* Copyright (c) 2014 Inocybe Technologies, and others. All rights reserved.
*
* This program and the accompanying materials are made available under the
* terms of the Eclipse Public License v1.0 which accompanies this distribution,
* and is available at http://www.eclipse.org/legal/epl-v10.html
*/
package org.opendaylight.controller.topologymanager.shell;

import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.opendaylight.controller.topologymanager.ITopologyManagerShell;

@Command(scope = "topologymanager", name = "deleteUserLink", description="deletes user link")
public class DeleteUserLink extends OsgiCommandSupport{
    private ITopologyManagerShell topologyManager;

    @Argument(index=0, name="name", description="name", required=true, multiValued=false)
    String name = null;

    @Override
    protected Object doExecute() throws Exception {
        for(String p : topologyManager.deleteUserLinkShell(name)) {
            System.out.println(p);
        }
        return null;
    }

    public void setTopologyManager(ITopologyManagerShell topologyManager){
        this.topologyManager = topologyManager;
    }
}
