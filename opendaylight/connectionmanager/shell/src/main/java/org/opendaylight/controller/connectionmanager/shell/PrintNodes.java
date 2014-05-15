package org.opendaylight.controller.connectionmanager.shell;

import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Argument;
import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.opendaylight.controller.connectionmanager.IConnectionManagerShell;
import java.util.ArrayList;

@Command(scope = "connectionmanager", name = "printNodes", description="Display the nodes within the connection manager")
public class PrintNodes extends OsgiCommandSupport{

    private IConnectionManagerShell connectionManager;
    @Argument(index = 0, name = "argument", description = "The argument passed to the printNode command", required = false, multiValued = false)
    String name = null;

    @Override
    protected Object doExecute() throws Exception {

        System.out.println("Nodes connected to this controller : ");
        ArrayList<String> nodes = connectionManager.printNodes(name);
        if (nodes == null){
            System.out.println("None");
        }
        else{
            for (String s : nodes){
                System.out.println(s.toString());
            }
        }
        return null;
    }

    public void setHostTracker(IConnectionManagerShell connectionManager){
        this.connectionManager = connectionManager;
    }

    public void setName(String name){
        this.name = name;
    }
}