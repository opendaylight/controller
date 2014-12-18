package org.opendaylight.controller.containermanager.shell;

import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.opendaylight.controller.containermanager.IContainerManagerShell;

@Command(scope = "containermanager", name = "containermgrGetAuthorizedResources", description="Get authorized resources")
public class ContainermgrGetAuthorizedResources extends OsgiCommandSupport{
    private IContainerManagerShell containerManager;

    @Argument(index=0, name="roleName", description="role name", required=true, multiValued=false)
    String roleName = null;

    @Override
    protected Object doExecute() throws Exception {
        for(String p : containerManager.containermgrGetAuthorizedResources(roleName)) {
            System.out.println(p);
        }
        return null;
    }

    public void setContainerManager(IContainerManagerShell containerManager){
        this.containerManager = containerManager;
    }
}