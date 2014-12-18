package org.opendaylight.controller.containermanager.shell;

import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.opendaylight.controller.containermanager.IContainerManagerShell;

@Command(scope = "containermanager", name = "removeContainerEntry", description="remove container entry")
public class RemoveContainerEntry extends OsgiCommandSupport{
    private IContainerManagerShell containerManager;

    @Argument(index=0, name="containerName", description="container name", required=true, multiValued=false)
    String containerName = null;

    @Argument(index=1, name="nodeId", description="node ID", required=true, multiValued=false)
    String nodeId = null;

    @Argument(index=2, name="portId", description="portId", required=true, multiValued=false)
    String portId = null;

    @Override
    protected Object doExecute() throws Exception {
        for(String p : containerManager.removeContainerEntry(containerName, nodeId, portId)) {
            System.out.println(p);
        }
        return null;
    }

    public void setContainerManager(IContainerManagerShell containerManager){
        this.containerManager = containerManager;
    }
}