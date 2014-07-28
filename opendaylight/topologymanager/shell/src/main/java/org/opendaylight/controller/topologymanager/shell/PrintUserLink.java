package org.opendaylight.controller.topologymanager.shell;

import org.apache.felix.gogo.commands.Command;
import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.opendaylight.controller.topologymanager.ITopologyManagerShell;

@Command(scope = "topologymanager", name = "printUserLink", description="Prints user link")
public class PrintUserLink extends OsgiCommandSupport{
    private ITopologyManagerShell topologyManager;

    @Override
    protected Object doExecute() throws Exception {
        for(String p : topologyManager.printUserLink()) {
            System.out.println(p);
        }
        return null;
    }

    public void setTopologyManager(ITopologyManagerShell topologyManager){
        this.topologyManager = topologyManager;
    }
}