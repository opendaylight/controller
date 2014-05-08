package org.opendaylight.controller.hosttracker.shell;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.opendaylight.controller.hosttracker.IHostTrackerShell;

public class HostTrackerShellTest {

    private final long COMMAND_TIMEOUT = 1000;
    private IHostTrackerShell hostTracker;

    @Test
    public void testDumpPendingARPReqList() throws Exception {
        DumpPendingARPReqList dumpPendTest = new DumpPendingARPReqList();
        hostTracker = mock(IHostTrackerShell.class);
        List<String> failedList = new ArrayList<String>(Arrays.asList("a", "b", "c"));
        when(hostTracker.dumpPendingArpReqList()).thenReturn(failedList);
        dumpPendTest.setHostTracker(hostTracker);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        System.setOut(new PrintStream(baos));
        dumpPendTest.doExecute();
        //Assert.assertTrue(true);
        Assert.assertEquals("[a, b, c]", baos.toString());
    }

    @Test
    public void testDumpFailedARPReqList() throws Exception {
        DumpFailedARPReqList dumpFailTest = new DumpFailedARPReqList();
        hostTracker = mock(IHostTrackerShell.class);
        List<String> failedList = new ArrayList<String>(Arrays.asList("a", "b", "c"));
        when(hostTracker.dumpFailedArpReqList()).thenReturn(failedList);
        dumpFailTest.setHostTracker(hostTracker);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        System.setOut(new PrintStream(baos));
        dumpFailTest.doExecute();
        //Assert.assertTrue(true);
        Assert.assertEquals("[a, b, c]", baos.toString());
    }
}
