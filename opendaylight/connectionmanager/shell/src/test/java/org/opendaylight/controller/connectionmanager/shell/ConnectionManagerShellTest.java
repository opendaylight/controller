package org.opendaylight.controller.connectionmanager.shell;

import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.net.InetAddress;
import java.util.Set;

import org.junit.Assert;
//import org.opendaylight.controller.connectionmanager.shell.PrintNodes;
//import org.opendaylight.controller.connectionmanager.shell.Scheme;
import org.opendaylight.controller.connectionmanager.IConnectionManagerShell;
import org.opendaylight.controller.sal.core.Node;

public class ConnectionManagerShellTest {

    private final long COMMAND_TIMEOUT = 1000;
    private IConnectionManagerShell connectionManager;


    @Test
    public void testPrintNodes() throws Exception {
        PrintNodes printN = new PrintNodes();
        connectionManager = mock(IConnectionManagerShell.class);
        String arg = "";
        Set<Node> n = null;
        printN.setConnectionManager(connectionManager);
        InetAddress address = InetAddress.getByName(arg);
        when(connectionManager.getNodes(address)).thenReturn(n);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        System.setOut(new PrintStream(baos));
        printN.setName(arg);
        printN.doExecute();
        //Assert.assertTrue(true);
        Assert.assertEquals("Nodes connected to this controller : \nNone\n", baos.toString());
    }

    @Test
    public void testScheme() throws Exception {
        Scheme sch = new Scheme();
        connectionManager = mock(IConnectionManagerShell.class);
        String arg = "a";
        String s = "b";
        when(connectionManager.setScheme(arg)).thenReturn(s);
        sch.setConnectionManager(connectionManager);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        System.setOut(new PrintStream(baos));
        sch.doExecute();
        //Assert.assertTrue(true);
        Assert.assertEquals("Please enter valid Scheme name\n", baos.toString());
    }
}
