package org.opendaylight.controller.hosttracker.shell;

import org.apache.felix.gogo.commands.Command;
import org.apache.karaf.shell.console.OsgiCommandSupport;

@Command(scope = "connectionmanager", name = "printNodes", description="Display the connection nodes")
public class Scheme extends OsgiCommandSupport {

    @Override
    protected Object doExecute() throws Exception {
        System.out.println("Executing hostracker dumpPendingARPReqList");
        return null;
    }

}