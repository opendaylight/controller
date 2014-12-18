package org.opendaylight.controller.containermanager.shell;

import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.opendaylight.controller.containermanager.IContainerManagerShell;

@Command(scope = "containermanager", name = "containermgrGetUserResources", description="Get user resources")
public class ContainermgrGetUserResources extends OsgiCommandSupport{
    private IContainerManagerShell containerManager;

    @Argument(index=0, name="userName", description="user name", required=true, multiValued=false)
    String userName = null;

    @Override
    protected Object doExecute() throws Exception {
        for(String p : containerManager.containermgrGetUserResources(userName)) {
            System.out.println(p);
        }
        return null;
    }

    public void setContainerManager(IContainerManagerShell containerManager){
        this.containerManager = containerManager;
    }
}