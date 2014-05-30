package org.opendaylight.controller.usermanager.shell;
/*
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
*/
import org.junit.Test;

//import static org.mockito.Mockito.mock;
//import static org.mockito.Mockito.when;

//import org.opendaylight.controller.usermanager.IUserManagerShell;
//import org.opendaylight.controller.usermanager.shell.AddAAAServer;

public class UserManagerShellTest {

//    private final long COMMAND_TIMEOUT = 1000;
// private IUserManagerShell userManager;

    @Test
    public void testDumpPendingARPReqList() throws Exception {
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
    public void testDumpFailedARPReqList() throws Exception {
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
