package org.opendaylight.controller.containermanager.shell;

import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.opendaylight.controller.containermanager.IContainerManagerShell;

@Command(scope = "containermanager", name = "removeContainerFlow", description="removes container flow")
public class RemoveContainerFlow extends OsgiCommandSupport{
    private IContainerManagerShell containerManager;

    @Argument(index=0, name="containerName", description="container name", required=true, multiValued=false)
    String containerName = null;

    @Argument(index=1, name="cflowName", description="c Flow name", required=true, multiValued=false)
    String cflowName = null;

    @Override
    protected Object doExecute() throws Exception {
        for(String p : containerManager.removeContainerFlow(containerName, cflowName)) {
            System.out.println(p);
        }
        return null;
    }

    public void setContainerManager(IContainerManagerShell containerManager){
        this.containerManager = containerManager;
    }
}