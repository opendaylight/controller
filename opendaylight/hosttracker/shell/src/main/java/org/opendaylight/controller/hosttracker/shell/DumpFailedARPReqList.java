package org.opendaylight.controller.hosttracker.shell;

import org.apache.felix.gogo.commands.Command;
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