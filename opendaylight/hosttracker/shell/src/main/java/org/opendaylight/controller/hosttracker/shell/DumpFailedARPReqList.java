package org.opendaylight.controller.hosttracker.shell;

import org.apache.felix.gogo.commands.Command;
import org.apache.karaf.shell.console.OsgiCommandSupport;

@Command(scope = "hosttracker", name = "dumpFailedARPReqList", description="Dump the ARP failed list")
public class DumpFailedARPReqList extends OsgiCommandSupport{
    
    @Override
    protected Object doExecute() throws Exception {
        System.out.println("Executing hostracker dumpFailedARPReqList");
        
        return null;
    }
}