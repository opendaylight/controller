package org.opendaylight.controller.hosttracker.shell;
/**
* Copyright (c) 2014 Inocybe Technologies, and others. All rights reserved.
*
* This program and the accompanying materials are made available under the
* terms of the Eclipse Public License v1.0 which accompanies this distribution,
* and is available at http://www.eclipse.org/legal/epl-v10.html
*/

import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.opendaylight.controller.hosttracker.IHostTrackerShell;

@Command(scope = "hosttracker", name = "dumpFailedARPReqList", description="Display the dump failed ARPReqList")
public class DumpFailedARPReqList extends OsgiCommandSupport{

    private IHostTrackerShell hostTracker;

    @Override
    protected Object doExecute() throws Exception {
        System.out.print(hostTracker.dumpFailedArpReqList());
        return null;
    }

    public void setHostTracker(IHostTrackerShell hostTracker){
        this.hostTracker = hostTracker;
    }
}
