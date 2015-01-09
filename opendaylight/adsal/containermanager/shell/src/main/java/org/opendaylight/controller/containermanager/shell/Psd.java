package org.opendaylight.controller.containermanager.shell;

import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.opendaylight.controller.containermanager.IContainerManagerShell;

@Command(scope = "containermanager", name = "psd", description="Display psd")
public class Psd extends OsgiCommandSupport{
    private IContainerManagerShell containerManager;

    @Override
    protected Object doExecute() throws Exception {
        for(String p : containerManager.psd()) {
            System.out.println(p);
        }
        return null;
    }

    public void setContainerManager(IContainerManagerShell containerManager){
        this.containerManager = containerManager;
    }
}