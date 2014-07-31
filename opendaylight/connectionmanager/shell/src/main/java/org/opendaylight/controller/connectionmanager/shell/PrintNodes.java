package org.opendaylight.controller.connectionmanager.shell;
/**
* Copyright (c) 2014 Inocybe Technologies, and others. All rights reserved.
*
* This program and the accompanying materials are made available under the
* terms of the Eclipse Public License v1.0 which accompanies this distribution,
* and is available at http://www.eclipse.org/legal/epl-v10.html
*/

import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Argument;
import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.opendaylight.controller.connectionmanager.IConnectionManagerShell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.net.InetAddress;
import java.net.UnknownHostException;

@Command(scope = "connectionmanager", name = "printNodes", description="Display the nodes within the connection manager")
public class PrintNodes extends OsgiCommandSupport{

    private IConnectionManagerShell connectionManager;
    @Argument(index = 0, name = "argument", description = "The argument passed to the printNode command", required = false, multiValued = false)
    String name = null;
    private static final Logger logger = LoggerFactory.getLogger(PrintNodes.class);

    @Override
    protected Object doExecute() throws Exception {
        String controller = name;
        if (controller == null) {
            System.out.println("Nodes connected to this controller : ");
            if (connectionManager.getLocalNodes() == null) {
                System.out.println("None");
            } else {
                System.out.println(connectionManager.getLocalNodes().toString());
            }
            return null;
        }
        try {
            InetAddress address = InetAddress.getByName(controller);
            System.out.println("Nodes connected to this controller : " + controller);
            if (connectionManager.getNodes(address) == null) {
                System.out.println("None");
            } else {
                System.out.println(connectionManager.getNodes(address).toString());
            }
        } catch (UnknownHostException e) {
            logger.error("An error occured", e);
        }
        return null;
    }

    public void setConnectionManager(IConnectionManagerShell connectionManager){
        this.connectionManager = connectionManager;
    }

    public void setName(String name){
        this.name = name;
    }
}