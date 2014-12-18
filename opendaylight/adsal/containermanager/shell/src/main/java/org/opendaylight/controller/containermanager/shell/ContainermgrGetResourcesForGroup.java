package org.opendaylight.controller.containermanager.shell;

import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.opendaylight.controller.containermanager.IContainerManagerShell;

@Command(scope = "containermanager", name = "containermgrGetResourcesForGroup", description="Get resources for group")
public class ContainermgrGetResourcesForGroup extends OsgiCommandSupport{
    private IContainerManagerShell containerManager;

    @Argument(index=0, name="groupName", description="group name", required=true, multiValued=false)
    String groupName = null;

    @Override
    protected Object doExecute() throws Exception {
        for(String p : containerManager.containermgrGetResourcesForGroup(groupName)) {
            System.out.println(p);
        }
        return null;
    }

    public void setContainerManager(IContainerManagerShell containerManager){
        this.containerManager = containerManager;
    }
}