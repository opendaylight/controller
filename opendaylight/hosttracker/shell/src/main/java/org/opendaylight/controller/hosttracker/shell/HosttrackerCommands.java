package org.opendaylight.controller.hosttracker.shell;

import org.apache.felix.gogo.commands.Command;
import org.apache.karaf.shell.console.OsgiCommandSupport;

@Command(scope = "hosttracker", name = "dumpPendingARPReqList", description="Dump the ARP request list")
public class HosttrackerCommands extends OsgiCommandSupport {

    @Override
    protected Object doExecute() throws Exception {
        System.out.println("Executing hostracker dumpPendingARPReqList");
        return null;
    }

}