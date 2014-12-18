package org.opendaylight.controller.containermanager.shell;

import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.opendaylight.controller.containermanager.IContainerManagerShell;

@Command(scope = "containermanager", name = "addContainerFlow", description="adds container flow")
public class AddContainerFlow extends OsgiCommandSupport{
    private IContainerManagerShell containerManager;

    @Argument(index=0, name="containerName", description="container name", required=true, multiValued=false)
    String containerName = null;

    @Argument(index=1, name="cflowName", description="c Flow name", required=true, multiValued=false)
    String cflowName = null;

    @Argument(index=2, name="unidirectional", description="unidirectional", required=true, multiValued=false)
    String unidirectional = null;

    @Override
    protected Object doExecute() throws Exception {
        for(String p : containerManager.addContainerFlow(containerName, cflowName, unidirectional)) {
            System.out.println(p);
        }
        return null;
    }

    public void setContainerManager(IContainerManagerShell containerManager){
        this.containerManager = containerManager;
    }
}