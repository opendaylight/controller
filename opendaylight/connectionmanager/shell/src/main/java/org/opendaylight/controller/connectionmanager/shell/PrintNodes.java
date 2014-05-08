package org.opendaylight.controller.hosttracker.shell;

import org.apache.felix.gogo.commands.Command;
import org.apache.karaf.shell.console.OsgiCommandSupport;

@Command(scope = "connectionmanager", name = "scheme", description="Display the scheme")
public class PrintNodes extends OsgiCommandSupport{

    @Override
    protected Object doExecute() throws Exception {
        System.out.println("Executing hostracker dumpFailedARPReqList");

        return null;
    }
}