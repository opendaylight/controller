/**
* Copyright (c) 2014 Inocybe Technologies, and others. All rights reserved.
*
* This program and the accompanying materials are made available under the
* terms of the Eclipse Public License v1.0 which accompanies this distribution,
* and is available at http://www.eclipse.org/legal/epl-v10.html
*/
package org.opendaylight.controller.switchmanager.shell;

import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Argument;
import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.opendaylight.controller.switchmanager.ISwitchManagerShell;

@Command(scope = "switchmanager", name = "pencs", description="Display pencs")
public class Pencs extends OsgiCommandSupport{
    private ISwitchManagerShell switchManager;

    @Argument(index=0, name="st", description="st", required=true, multiValued=false)
    String st = null;


    @Override
    protected Object doExecute() throws Exception {
        for(String p : switchManager.pencs(st)) {
            System.out.println(p);
        }
        return null;
    }

    public void setSwitchManager(ISwitchManagerShell switchManager){
        this.switchManager = switchManager;
    }
}
