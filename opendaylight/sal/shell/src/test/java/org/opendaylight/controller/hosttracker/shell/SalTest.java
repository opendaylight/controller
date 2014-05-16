package org.opendaylight.controller.hosttracker.shell;
/*
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.opendaylight.controller.sal.shell.AddFlow;
import org.opendaylight.controller.sal.shell.ModifyFlow;
import org.junit.Assert;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.opendaylight.controller.sal.flowprogrammer.IFlowProgrammerServiceShell;
import org.opendaylight.controller.sal.reader.IReadServiceShell;
*/
import org.junit.Test;

// Unit Test for SAL shell
public class SalTest {

    //private final long COMMAND_TIMEOUT = 1000;
    //private IFlowProgrammerServiceShell flow;
    //private IReadServiceShell read;

    @Test
    public void testDumpPendingARPReqList() throws Exception {
/*
        ModifyFlow dumpPendTest = new ModifyFlow();
        hostTracker = mock(IHostTrackerShell.class);
        List<String> failedList = new ArrayList<String>(Arrays.asList("a", "b", "c"));
        when(hostTracker.dumpPendingArpReqList()).thenReturn(failedList);
        dumpPendTest.setHostTracker(hostTracker);
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
        AddFlow dumpFailTest = new AddFlow();
        hostTracker = mock(IHostTrackerShell.class);
        List<String> failedList = new ArrayList<String>(Arrays.asList("a", "b", "c"));
        when(hostTracker.dumpFailedArpReqList()).thenReturn(failedList);
        dumpFailTest.setHostTracker(hostTracker);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        System.setOut(new PrintStream(baos));
        dumpFailTest.doExecute();
        //Assert.assertTrue(true);
        Assert.assertEquals("[a, b, c]", baos.toString());
*/
    }
}
