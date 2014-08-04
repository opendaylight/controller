package org.opendaylight.controller.usermanager.shell;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import org.junit.Assert;
import org.junit.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.opendaylight.controller.sal.utils.Status;
import org.opendaylight.controller.usermanager.IUserManagerShell;

public class UserManagerShellTest {

    private final long COMMAND_TIMEOUT = 1000;
    private IUserManagerShell userManager;

    @Test
    public void testAddAAAServer() throws Exception {
        AddAAAServer addAAAServer = new AddAAAServer();
        userManager = mock(IUserManagerShell.class);
        String commandOutput = "Usage : addAAAServer <server> <secret> <protocol>\n";
        Status s = new Status(null, commandOutput);
        when(userManager.addAAAServer(null)).thenReturn(s);
        addAAAServer.setUserManager(userManager);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        System.setOut(new PrintStream(baos));
        addAAAServer.doExecute();
        Assert.assertEquals(commandOutput, baos.toString());
    }

    @Test
    public void testPrintAAAServers() throws Exception {
/*
        PrintAAAServers printAAAServers = new PrintAAAServers();
        userManager = mock(IUserManagerShell.class);
        String commandOutput = "\n";
        //Status s = new Status(null, commandOutput);
        new ServerConfig(null, "bogus", "bogus");
        List<ServerConfig> conf = new ArrayList<ServerConfig>(Arrays.asList(new ServerConfig()));
        when(userManager.getAAAServerList()).thenReturn(conf);
        printAAAServers.setUserManager(userManager);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        System.setOut(new PrintStream(baos));
        printAAAServers.doExecute();
        Assert.assertEquals(commandOutput, baos.toString());
*/
    }

    @Test
    public void testRemoveAAAServer() throws Exception {
        RemoveAAAServer removeAAAServer = new RemoveAAAServer();
        userManager = mock(IUserManagerShell.class);
        String commandOutput = "Usage : removeAAAServer <server> <secret> <protocol>\n";
        Status s = new Status(null, commandOutput);
        when(userManager.addAAAServer(null)).thenReturn(s);
        removeAAAServer.setUserManager(userManager);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        System.setOut(new PrintStream(baos));
        removeAAAServer.doExecute();
        Assert.assertEquals(commandOutput, baos.toString());
    }
    @Test
    public void testUmAddUser() throws Exception {
/*
        UmAddUser umAddUser = new UmAddUser();
        userManager = mock(IUserManagerShell.class);
        //List<String> failedList = new ArrayList<String>(Arrays.asList("a", "b", "c"));
        when(userManager.).thenReturn(failedList);
        //dumpFailTest.setuserManager(userManager);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        System.setOut(new PrintStream(baos));
        //dumpFailTest.doExecute();
        //Assert.assertTrue(true);
        Assert.assertEquals("[a, b, c]", baos.toString());
*/
    }
    @Test
    public void testUmGetUsers() throws Exception {
/*
        AddAAAServer dumpFailTest = new AddAAAServer();
        userManager = mock(IUserManagerShell.class);
        List<String> failedList = new ArrayList<String>(Arrays.asList("a", "b", "c"));
        when(userManager.dumpFailedArpReqList()).thenReturn(failedList);
        dumpFailTest.setuserManager(userManager);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        System.setOut(new PrintStream(baos));
        dumpFailTest.doExecute();
        //Assert.assertTrue(true);
        Assert.assertEquals("[a, b, c]", baos.toString());
*/
    }
    @Test
    public void testUmRemUser() throws Exception {
/*
        AddAAAServer dumpFailTest = new AddAAAServer();
        userManager = mock(IUserManagerShell.class);
        List<String> failedList = new ArrayList<String>(Arrays.asList("a", "b", "c"));
        when(userManager.dumpFailedArpReqList()).thenReturn(failedList);
        dumpFailTest.setuserManager(userManager);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        System.setOut(new PrintStream(baos));
        dumpFailTest.doExecute();
        //Assert.assertTrue(true);
        Assert.assertEquals("[a, b, c]", baos.toString());
*/
    }
}
