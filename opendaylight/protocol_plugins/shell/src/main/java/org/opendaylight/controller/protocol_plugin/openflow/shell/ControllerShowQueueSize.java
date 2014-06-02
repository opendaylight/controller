package org.opendaylight.controller.protocol_plugin.openflow.shell;

import org.apache.felix.gogo.commands.Command;
import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.opendaylight.controller.protocol_plugin.openflow.core.IControllerShell;

@Command(scope = "controller", name = "showQueueSize", description="show controller queue size")
public class ControllerShowQueueSize extends OsgiCommandSupport{
    private IControllerShell controller;

    @Override
    protected Object doExecute() throws Exception {
        for(String p : controller.controllerShowQueueSize()) {
            System.out.println(p);
        }
        return null;
    }

    public void setController(IControllerShell controller){
        this.controller = controller;
    }
}
