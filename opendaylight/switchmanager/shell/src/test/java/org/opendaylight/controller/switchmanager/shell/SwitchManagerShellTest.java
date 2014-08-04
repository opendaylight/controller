package org.opendaylight.controller.switchmanager.shell;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.lang.reflect.Field;

import org.junit.Assert;
import org.junit.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.opendaylight.controller.switchmanager.ISwitchManagerShell;


public class SwitchManagerShellTest {
    private ISwitchManagerShell switchManager;

    @Test
    public void testPencs() throws Exception {
        String st = "test";
        Pencs pencsTest = new Pencs();
        switchManager = mock(ISwitchManagerShell.class);
        List<String> result = new ArrayList<String>(Arrays.asList("status"));
        when(switchManager.pencs(st)).thenReturn(result);

        Field sField = pencsTest.getClass().getDeclaredField("nodeId");
        sField.setAccessible(true);

        sField.set(pencsTest, "test");

        pencsTest.setSwitchManager(switchManager);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        System.setOut(new PrintStream(baos));
        pencsTest.doExecute();
        Assert.assertEquals("status\n", baos.toString());
    }
    @Test
    public void testPmd() throws Exception {
        String st = "test";
        Pdm pmdTest = new Pdm();
        switchManager = mock(ISwitchManagerShell.class);
        List<String> result = new ArrayList<String>(Arrays.asList("status"));
        when(switchManager.pdm(st)).thenReturn(result);

        Field sField = pmdTest.getClass().getDeclaredField("nodeId");
        sField.setAccessible(true);

        sField.set(pmdTest, "test");

        pmdTest.setSwitchManager(switchManager);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        System.setOut(new PrintStream(baos));
        pmdTest.doExecute();
        Assert.assertEquals("status\n", baos.toString());
    }
}