package org.opendaylight.controller.connectionmanager.shell;

import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Argument;
import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.opendaylight.controller.connectionmanager.IConnectionManagerShell;


@Command(scope = "connectionmanager", name = "scheme", description="Affect a scheme passed in parameter")
public class Scheme extends OsgiCommandSupport{

    private IConnectionManagerShell connectionManager;
    @Argument(index = 0, name = "argument", description = "The argument passed to the scheme command", required = false, multiValued = false)
    String name = null;

    @Override
    protected Object doExecute() throws Exception {

        String scheme = connectionManager.scheme(name);

        if (name == null){
            System.out.println("Please enter valid Scheme name");
            if (scheme != null){
                System.out.println("Current Scheme : " + scheme);
            }
        }
        return null;
    }

    public void setHostTracker(IConnectionManagerShell connectionManager){
        this.connectionManager = connectionManager;
    }
}