package org.opendaylight.controller.containermanager.shell;

import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.opendaylight.controller.containermanager.IContainerManagerShell;

@Command(scope = "containermanager", name = "removecontainer", description="remove container")
public class RemoveContainer extends OsgiCommandSupport{
    private IContainerManagerShell containerManager;

    @Argument(index=0, name="containerName", description="container name", required=true, multiValued=false)
    String containerName = null;

    @Override
    protected Object doExecute() throws Exception {
        for(String p : containerManager.removeContainerShell(containerName)) {
            System.out.println(p);
        }
        return null;
    }

    public void setContainerManager(IContainerManagerShell containerManager){
        this.containerManager = containerManager;
    }
}