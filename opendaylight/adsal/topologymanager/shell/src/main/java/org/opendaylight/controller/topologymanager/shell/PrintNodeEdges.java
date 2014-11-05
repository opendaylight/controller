/**
* Copyright (c) 2014 Inocybe Technologies, and others. All rights reserved.
*
* This program and the accompanying materials are made available under the
* terms of the Eclipse Public License v1.0 which accompanies this distribution,
* and is available at http://www.eclipse.org/legal/epl-v10.html
*/
package org.opendaylight.controller.topologymanager.shell;

import org.apache.felix.gogo.commands.Command;
import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.opendaylight.controller.topologymanager.ITopologyManagerShell;

@Command(scope = "topologymanager", name = "printNodeEdges", description="Prints node edges")
public class PrintNodeEdges extends OsgiCommandSupport{
    private ITopologyManagerShell topologyManager;

    @Override
    protected Object doExecute() throws Exception {
        for(String p : topologyManager.printNodeEdges()) {
            System.out.println(p);
        }
        return null;
    }

    public void setTopologyManager(ITopologyManagerShell topologyManager){
        this.topologyManager = topologyManager;
    }
}
