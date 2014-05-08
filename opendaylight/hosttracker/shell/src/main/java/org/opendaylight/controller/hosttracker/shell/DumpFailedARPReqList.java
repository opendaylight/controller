package org.opendaylight.controller.hosttracker.shell;

import org.apache.felix.gogo.commands.Command;
import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.opendaylight.controller.hosttracker.IHostTrackerShell;
//import org.opendaylight.controller.hosttracker.internal.HostTracker;

@Command(scope = "hosttracker", name = "dumpFailedARPReqList", description="Display the dump failed ARPReqList")
public class DumpFailedARPReqList extends OsgiCommandSupport{

    private IHostTrackerShell hosttracker;

    @Override
    protected Object doExecute() throws Exception {
        System.out.println("Executing hostracker dumpFailedARPReqList");
        System.out.print(hosttracker.dumpFailedArpReqList());
        //hosttracker.dumpFailedArpReqList();
        return null;
    }
}