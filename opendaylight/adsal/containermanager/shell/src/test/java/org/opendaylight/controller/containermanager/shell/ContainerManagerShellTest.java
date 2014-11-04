package org.opendaylight.controller.containermanager.shell;

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
import org.opendaylight.controller.containermanager.IContainerManagerShell;


public class ContainerManagerShellTest {
    private IContainerManagerShell containerManager;

    @Test
    public void testAddContainer() throws Exception {
        String containerName = "test", staticVlan = "1234";
        AddContainer addConTest = new AddContainer();
        containerManager = mock(IContainerManagerShell.class);
        List<String> result = new ArrayList<String>(Arrays.asList("status"));
        List<String> result2 = new ArrayList<String>(Arrays.asList("Container Name not specified"));
        when(containerManager.addContainer(containerName, staticVlan)).thenReturn(result);
        when(containerManager.addContainer(null, null)).thenReturn(result2);

        Field cNField = addConTest.getClass().getDeclaredField("containerName");
        cNField.setAccessible(true);
        Field sVField = addConTest.getClass().getDeclaredField("staticVlan");
        sVField.setAccessible(true);

        cNField.set(addConTest, "test");
        sVField.set(addConTest, "1234");

        addConTest.setContainerManager(containerManager);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        System.setOut(new PrintStream(baos));
        addConTest.doExecute();
        Assert.assertEquals("status\n", baos.toString());
        baos.reset();

        cNField.set(addConTest, null);
        sVField.set(addConTest, null);
        addConTest.doExecute();
        Assert.assertEquals("Container Name not specified\n", baos.toString());
    }

    @Test
    public void testAddContainerEntry() throws Exception {
        String containerName = "test", nodeId = "1234", portId = "5678";
        AddContainerEntry addConEntTest = new AddContainerEntry();
        containerManager = mock(IContainerManagerShell.class);
        List<String> result = new ArrayList<String>(Arrays.asList("status"));
        when(containerManager.addContainerEntry(containerName, nodeId, portId)).thenReturn(result);

        Field cNField = addConEntTest.getClass().getDeclaredField("containerName");
        cNField.setAccessible(true);
        Field nIField = addConEntTest.getClass().getDeclaredField("nodeId");
        nIField.setAccessible(true);
        Field pIField = addConEntTest.getClass().getDeclaredField("portId");
        pIField.setAccessible(true);

        cNField.set(addConEntTest, "test");
        nIField.set(addConEntTest, "1234");
        pIField.set(addConEntTest, "5678");

        addConEntTest.setContainerManager(containerManager);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        System.setOut(new PrintStream(baos));
        addConEntTest.doExecute();
        Assert.assertEquals("status\n", baos.toString());
    }

    @Test
    public void testAddContainerFlow() throws Exception {
        String containerName = "test", cflowName = "1234", unidirectional = "5678";
        AddContainerFlow addConFlowTest = new AddContainerFlow();
        containerManager = mock(IContainerManagerShell.class);
        List<String> result = new ArrayList<String>(Arrays.asList("status"));
        when(containerManager.addContainerFlow(containerName, cflowName, unidirectional)).thenReturn(result);

        Field cNField = addConFlowTest.getClass().getDeclaredField("containerName");
        cNField.setAccessible(true);
        Field cfField = addConFlowTest.getClass().getDeclaredField("cflowName");
        cfField.setAccessible(true);
        Field unField = addConFlowTest.getClass().getDeclaredField("unidirectional");
        unField.setAccessible(true);

        cNField.set(addConFlowTest, "test");
        cfField.set(addConFlowTest, "1234");
        unField.set(addConFlowTest, "5678");

        addConFlowTest.setContainerManager(containerManager);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        System.setOut(new PrintStream(baos));
        addConFlowTest.doExecute();
        Assert.assertEquals("status\n", baos.toString());
    }

    @Test
    public void testContainermgrGetAuthorizedGroups() throws Exception {
        String roleName = "test";
        ContainermgrGetAuthorizedGroups contmgrGTest = new ContainermgrGetAuthorizedGroups();
        containerManager = mock(IContainerManagerShell.class);
        List<String> result = new ArrayList<String>(Arrays.asList("status"));
        when(containerManager.containermgrGetAuthorizedGroups(roleName)).thenReturn(result);

        Field rNField = contmgrGTest.getClass().getDeclaredField("roleName");
        rNField.setAccessible(true);

        rNField.set(contmgrGTest, "test");

        contmgrGTest.setContainerManager(containerManager);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        System.setOut(new PrintStream(baos));
        contmgrGTest.doExecute();
        Assert.assertEquals("status\n", baos.toString());
    }

    @Test
    public void testContainermgrGetAuthorizedResources() throws Exception {
        String roleName = "test";
        ContainermgrGetAuthorizedResources contmgrRTest = new ContainermgrGetAuthorizedResources();
        containerManager = mock(IContainerManagerShell.class);
        List<String> result = new ArrayList<String>(Arrays.asList("status"));
        when(containerManager.containermgrGetAuthorizedResources(roleName)).thenReturn(result);

        Field rNField = contmgrRTest.getClass().getDeclaredField("roleName");
        rNField.setAccessible(true);

        rNField.set(contmgrRTest, "test");

        contmgrRTest.setContainerManager(containerManager);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        System.setOut(new PrintStream(baos));
        contmgrRTest.doExecute();
        Assert.assertEquals("status\n", baos.toString());
    }

    @Test
    public void testContainermgrGetResourcesForGroup() throws Exception {
        String groupName = "test";
        ContainermgrGetResourcesForGroup contmgrRTest = new ContainermgrGetResourcesForGroup();
        containerManager = mock(IContainerManagerShell.class);
        List<String> result = new ArrayList<String>(Arrays.asList("status"));
        when(containerManager.containermgrGetResourcesForGroup(groupName)).thenReturn(result);

        Field gNField = contmgrRTest.getClass().getDeclaredField("groupName");
        gNField.setAccessible(true);

        gNField.set(contmgrRTest, groupName);

        contmgrRTest.setContainerManager(containerManager);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        System.setOut(new PrintStream(baos));
        contmgrRTest.doExecute();
        Assert.assertEquals("status\n", baos.toString());
    }

    @Test
    public void testContainermgrGetRoles() throws Exception {
        ContainermgrGetRoles contmgrRTest = new ContainermgrGetRoles();
        containerManager = mock(IContainerManagerShell.class);
        List<String> result = new ArrayList<String>(Arrays.asList("status"));
        when(containerManager.containermgrGetRoles()).thenReturn(result);

        contmgrRTest.setContainerManager(containerManager);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        System.setOut(new PrintStream(baos));
        contmgrRTest.doExecute();
        Assert.assertEquals("status\n", baos.toString());
    }

    @Test
    public void testContainermgrGetUserLevel() throws Exception {
        String userName = "test";
        ContainermgrGetUserLevel contmgrUTest = new ContainermgrGetUserLevel();
        containerManager = mock(IContainerManagerShell.class);
        List<String> result = new ArrayList<String>(Arrays.asList("status"));
        when(containerManager.containermgrGetUserLevel(userName)).thenReturn(result);

        Field gNField = contmgrUTest.getClass().getDeclaredField("userName");
        gNField.setAccessible(true);

        gNField.set(contmgrUTest, userName);

        contmgrUTest.setContainerManager(containerManager);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        System.setOut(new PrintStream(baos));
        contmgrUTest.doExecute();
        Assert.assertEquals("status\n", baos.toString());
    }

    @Test
    public void testContainermgrGetUserResources() throws Exception {
        String userName = "test";
        ContainermgrGetUserResources contmgrUTest = new ContainermgrGetUserResources();
        containerManager = mock(IContainerManagerShell.class);
        List<String> result = new ArrayList<String>(Arrays.asList("status"));
        when(containerManager.containermgrGetUserResources(userName)).thenReturn(result);

        Field gNField = contmgrUTest.getClass().getDeclaredField("userName");
        gNField.setAccessible(true);

        gNField.set(contmgrUTest, userName);

        contmgrUTest.setContainerManager(containerManager);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        System.setOut(new PrintStream(baos));
        contmgrUTest.doExecute();
        Assert.assertEquals("status\n", baos.toString());
    }

    @Test
    public void testPfc() throws Exception {
        Pfc pfc = new Pfc();
        containerManager = mock(IContainerManagerShell.class);
        List<String> result = new ArrayList<String>(Arrays.asList("status"));
        when(containerManager.pfc()).thenReturn(result);

        pfc.setContainerManager(containerManager);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        System.setOut(new PrintStream(baos));
        pfc.doExecute();
        Assert.assertEquals("status\n", baos.toString());
    }

    @Test
    public void testPsc() throws Exception {
        Psc psc = new Psc();
        containerManager = mock(IContainerManagerShell.class);
        List<String> result = new ArrayList<String>(Arrays.asList("status"));
        when(containerManager.psc()).thenReturn(result);

        psc.setContainerManager(containerManager);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        System.setOut(new PrintStream(baos));
        psc.doExecute();
        Assert.assertEquals("status\n", baos.toString());
    }

    @Test
    public void testPsd() throws Exception {
        Psd psd = new Psd();
        containerManager = mock(IContainerManagerShell.class);
        List<String> result = new ArrayList<String>(Arrays.asList("status"));
        when(containerManager.psd()).thenReturn(result);

        psd.setContainerManager(containerManager);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        System.setOut(new PrintStream(baos));
        psd.doExecute();
        Assert.assertEquals("status\n", baos.toString());
    }

    @Test
    public void testPsm() throws Exception {
        Psm psm = new Psm();
        containerManager = mock(IContainerManagerShell.class);
        List<String> result = new ArrayList<String>(Arrays.asList("status"));
        when(containerManager.psm()).thenReturn(result);

        psm.setContainerManager(containerManager);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        System.setOut(new PrintStream(baos));
        psm.doExecute();
        Assert.assertEquals("status\n", baos.toString());
    }

    @Test
    public void testPsp() throws Exception {
        Psp psp = new Psp();
        containerManager = mock(IContainerManagerShell.class);
        List<String> result = new ArrayList<String>(Arrays.asList("status"));
        when(containerManager.psp()).thenReturn(result);

        psp.setContainerManager(containerManager);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        System.setOut(new PrintStream(baos));
        psp.doExecute();
        Assert.assertEquals("status\n", baos.toString());
    }

    @Test
    public void testRemoveContainer() throws Exception {
        String containerName = "test";
        RemoveContainer remConTest = new RemoveContainer();
        containerManager = mock(IContainerManagerShell.class);
        List<String> result = new ArrayList<String>(Arrays.asList("status"));
        when(containerManager.removeContainerShell(containerName)).thenReturn(result);

        Field cNField = remConTest.getClass().getDeclaredField("containerName");
        cNField.setAccessible(true);
        cNField.set(remConTest, "test");

        remConTest.setContainerManager(containerManager);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        System.setOut(new PrintStream(baos));
        remConTest.doExecute();
        Assert.assertEquals("status\n", baos.toString());
    }

    @Test
    public void testRemoveContainerEntry() throws Exception {
        String containerName = "test", nodeId = "1234", portId = "5678";
        RemoveContainerEntry remConEntTest = new RemoveContainerEntry();
        containerManager = mock(IContainerManagerShell.class);
        List<String> result = new ArrayList<String>(Arrays.asList("status"));
        when(containerManager.removeContainerEntry(containerName, nodeId, portId)).thenReturn(result);

        Field cNField = remConEntTest.getClass().getDeclaredField("containerName");
        cNField.setAccessible(true);
        Field nIField = remConEntTest.getClass().getDeclaredField("nodeId");
        nIField.setAccessible(true);
        Field pIField = remConEntTest.getClass().getDeclaredField("portId");
        pIField.setAccessible(true);

        cNField.set(remConEntTest, "test");
        nIField.set(remConEntTest, "1234");
        pIField.set(remConEntTest, "5678");

        remConEntTest.setContainerManager(containerManager);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        System.setOut(new PrintStream(baos));
        remConEntTest.doExecute();
        Assert.assertEquals("status\n", baos.toString());
    }

    @Test
    public void testRemoveContainerFlow() throws Exception {
        String containerName = "test", cflowName = "1234";
        RemoveContainerFlow remConFlowTest = new RemoveContainerFlow();
        containerManager = mock(IContainerManagerShell.class);
        List<String> result = new ArrayList<String>(Arrays.asList("status"));
        when(containerManager.removeContainerFlow(containerName, cflowName)).thenReturn(result);

        Field cNField = remConFlowTest.getClass().getDeclaredField("containerName");
        cNField.setAccessible(true);
        Field cfField = remConFlowTest.getClass().getDeclaredField("cflowName");
        cfField.setAccessible(true);

        cNField.set(remConFlowTest, "test");
        cfField.set(remConFlowTest, "1234");

        remConFlowTest.setContainerManager(containerManager);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        System.setOut(new PrintStream(baos));
        remConFlowTest.doExecute();
        Assert.assertEquals("status\n", baos.toString());
    }

    @Test
    public void testSaveConfig() throws Exception {
        SaveConfig saveConfig = new SaveConfig();
        containerManager = mock(IContainerManagerShell.class);
        List<String> result = new ArrayList<String>(Arrays.asList("status"));
        when(containerManager.saveConfig()).thenReturn(result);

        saveConfig.setContainerManager(containerManager);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        System.setOut(new PrintStream(baos));
        saveConfig.doExecute();
        Assert.assertEquals("status\n", baos.toString());
    }
}