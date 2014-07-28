package org.opendaylight.controller.switchmanager.shell;

import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Argument;
import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.opendaylight.controller.switchmanager.ISwitchManagerShell;

@Command(scope = "switchmanager", name = "pdm", description="Display pdm")
public class Pdm extends OsgiCommandSupport{
    private ISwitchManagerShell switchManager;

    @Argument(index=0, name="st", description="st", required=true, multiValued=false)
    String st = null;


    @Override
    protected Object doExecute() throws Exception {
        for(String p : switchManager.pdm(st)) {
            System.out.println(p);
        }
        return null;
    }

    public void setSwitchManager(ISwitchManagerShell switchManager){
        this.switchManager = switchManager;
    }
}