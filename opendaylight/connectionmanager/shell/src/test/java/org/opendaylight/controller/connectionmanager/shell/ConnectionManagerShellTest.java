package org.opendaylight.controller.connectionmanager.shell;

import org.junit.Test;

/*import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import org.junit.Assert;*/
//import org.opendaylight.controller.connectionmanager.shell.PrintNodes;
//import org.opendaylight.controller.connectionmanager.shell.Scheme;
import org.opendaylight.controller.connectionmanager.IConnectionManagerShell;

public class ConnectionManagerShellTest {

    private final long COMMAND_TIMEOUT = 1000;
    private IConnectionManagerShell connectionManager;

    @Test
    public void testPrintNodes() throws Exception {
        /*PrintNodes printN = new PrintNodes();
        connectionManager = mock(IConnectionManagerShell.class);
        String arg = "a";
        //ArrayList<String> n = new ArrayList<String>(Arrays.asList("a", "b", "c"));
        String n = "Please enter valid Scheme name";
        when(connectionManager.setScheme(arg)).thenReturn(n);
        printN.setConnectionManager(connectionManager);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        System.setOut(new PrintStream(baos));
        printN.setName(arg);
        printN.doExecute();
        //Assert.assertTrue(true);
        Assert.assertEquals("Nodes connected to this controller : \na\nb\nc\n", baos.toString());*/
    }

    @Test
    public void testScheme() throws Exception {
        /*Scheme sch = new Scheme();
        connectionManager = mock(IConnectionManagerShell.class);
        String arg = "a";
        String s = "b";
        when(connectionManager.setScheme(arg)).thenReturn(s);
        sch.setHostTracker(connectionManager);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        System.setOut(new PrintStream(baos));
        sch.doExecute();
        //Assert.assertTrue(true);
        Assert.assertEquals("Please enter valid Scheme name\n", baos.toString());*/
    }
}
