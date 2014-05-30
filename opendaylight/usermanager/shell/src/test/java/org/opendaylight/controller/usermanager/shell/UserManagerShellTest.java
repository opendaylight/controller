package org.opendaylight.controller.usermanager.shell;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.opendaylight.controller.sal.utils.Status;
import org.opendaylight.controller.usermanager.IUserManagerShell;
//import org.opendaylight.controller.usermanager.ServerConfig;

public class UserManagerShellTest {

    private final long COMMAND_TIMEOUT = 1000;
    private IUserManagerShell userManager;

    @Test
    public void testAddAAAServer() throws Exception {
        AddAAAServer addAAAServer = new AddAAAServer();
        userManager = mock(IUserManagerShell.class);
        String commandOutput = "Usage : addAAAServer <server> <secret> <protocol>";
        Status s = new Status(null, commandOutput);
        when(userManager.addAAAServer(null)).thenReturn(s);
        addAAAServer.setUserManager(userManager);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        System.setOut(new PrintStream(baos));
        addAAAServer.doExecute();
        Assert.assertEquals("Usage : addAAAServer <server> <secret> <protocol>\n", baos.toString());
    }

    @Test
    public void testPrintAAAServers() throws Exception {
/*
        PrintAAAServers printAAAServers = new PrintAAAServers();
        userManager = mock(IUserManagerShell.class);
        String commandOutput = "\n";
        //Status s = new Status(null, commandOutput);
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
/*      DumpPendingARPReqList dumpPendTest = new DumpPendingARPReqList();
        userManager = mock(IuserManagerShell.class);
        List<String> failedList = new ArrayList<String>(Arrays.asList("a", "b", "c"));
        when(userManager.dumpPendingArpReqList()).thenReturn(failedList);
        dumpPendTest.setuserManager(userManager);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        System.setOut(new PrintStream(baos));
        dumpPendTest.doExecute();
        //Assert.assertTrue(true);
        Assert.assertEquals("[a, b, c]", baos.toString());
*/
    }
    @Test
    public void testUmAddUser() throws Exception {
/*
        AddAAAServer dumpFailTest = new AddAAAServer();
        userManager = mock(IuserManagerShell.class);
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
    public void testUmGetUsers() throws Exception {
/*
        AddAAAServer dumpFailTest = new AddAAAServer();
        userManager = mock(IuserManagerShell.class);
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
        userManager = mock(IuserManagerShell.class);
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
