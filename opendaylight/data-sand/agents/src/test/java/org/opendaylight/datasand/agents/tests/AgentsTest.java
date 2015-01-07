package org.opendaylight.datasand.agents.tests;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.datasand.agents.AutonomousAgent;
import org.opendaylight.datasand.agents.AutonomousAgentManager;
import org.opendaylight.datasand.agents.Message;
import org.opendaylight.datasand.agents.cmap.CMap;
import org.opendaylight.datasand.agents.cmap.TypeDescriptorListener;
import org.opendaylight.datasand.codec.TypeDescriptor;
import org.opendaylight.datasand.codec.TypeDescriptorsContainer;
import org.opendaylight.datasand.network.NetworkID;
import org.opendaylight.datasand.network.NetworkNode;
import org.opendaylight.datasand.network.NetworkNodeConnection;

public class AgentsTest {

    private ByteArrayOutputStream bout = null;
    private PrintStream orig = System.out;

    @Before
    public void before() {
        try {
            bout = new ByteArrayOutputStream();
            System.setOut(new PrintStream(bout));
        } catch (Exception err) {
            err.printStackTrace();
        }
    }

    @After
    public void after() {
        setOrigOut();
        System.out
                .println("Sleeping for 5 seconds to allow proper nodes shutdown");
        try {
            Thread.sleep(5000);
        } catch (Exception err) {
        }
    }

    public void setOrigOut() {
        System.setOut(orig);
    }

    @Test
    public void testBroadcastToNodes() {
        NetworkNode nodes[] = new NetworkNode[10];
        for (int i = 0; i < 10; i++) {
            nodes[i] = new NetworkNode(null);
        }
        try {
            Thread.sleep(1000);
        } catch (Exception err) {
        }
        System.out.println("Ready");
        nodes[3].send(new byte[5], nodes[3].getLocalHost(),
                NetworkNodeConnection.PROTOCOL_ID_BROADCAST);
        NetworkID unreach = new NetworkID(nodes[3].getLocalHost()
                .getIPv4Address(), 56565, 0);
        nodes[3].send(new byte[5], nodes[3].getLocalHost(), unreach);
        try {
            Thread.sleep(1000);
        } catch (Exception err) {
        }
        for (int i = 0; i < nodes.length; i++) {
            nodes[i].shutdown();
        }
        try {
            bout.close();
            String output = new String(bout.toByteArray());
            setOrigOut();
            int count = countSubstring("Dest=0.0.0.0:0:10", output);
            Assert.assertEquals(new Integer(10), new Integer(count));
            Assert.assertEquals(true, output.indexOf("Unreachable") != -1);
        } catch (Exception err) {
            err.printStackTrace();
        }
    }

    @Test
    public void testBroadcastToAgent() {
        TypeDescriptorsContainer container = new TypeDescriptorsContainer("./node1");
        AutonomousAgentManager nodes[] = new AutonomousAgentManager[10];
        AutonomousAgent agent[] = new AutonomousAgent[nodes.length];
        for (int i = 0; i < nodes.length; i++) {
            nodes[i] = new AutonomousAgentManager(container);
            agent[i] = new TestAgent(nodes[i].getNetworkNode().getLocalHost(),nodes[i]);
        }

        try {
            Thread.sleep(1000);
        } catch (Exception err) {
        }

        System.out.println("Ready!");
        agent[4].send(new Message(0,createTestObject()),NetworkNodeConnection.PROTOCOL_ID_BROADCAST);

        try {
            Thread.sleep(1000);
        } catch (Exception err) {
        }

        for (int i = 0; i < nodes.length; i++) {
            nodes[i].shutdown();
        }

        try {
            bout.close();
            String output = new String(bout.toByteArray());
            setOrigOut();
            int count = countSubstring("Recieved Object, comparing..", output);
            Assert.assertEquals(new Integer(10), new Integer(count));
        } catch (Exception err) {
            err.printStackTrace();
        }

    }

    @Test
    public void testMulticast() {
        TypeDescriptorsContainer container = new TypeDescriptorsContainer("./node1");
        AutonomousAgentManager nodes[] = new AutonomousAgentManager[10];
        AutonomousAgent agent[] = new AutonomousAgent[nodes.length];
        // Arbitrary number greater than 10 and not equal to 9999 (which is the
        // destination unreachable code)
        int MULTICAST_GROUP = 27;
        NetworkID multiCast = new NetworkID(
                NetworkNodeConnection.PROTOCOL_ID_BROADCAST.getIPv4Address(),
                MULTICAST_GROUP, MULTICAST_GROUP);
        for (int i = 0; i < nodes.length; i++) {
            nodes[i] = new AutonomousAgentManager(container);
            agent[i] = new TestAgent(nodes[i].getNetworkNode().getLocalHost(),
                    nodes[i]);
            // only 5 agents are registered for this multicast
            if (i % 2 == 0) {
                nodes[i].registerForMulticast(MULTICAST_GROUP, agent[i]);
            }
        }

        System.out.println("Ready!");
        agent[2].send(new Message(0,createTestObject()), multiCast);

        try {
            Thread.sleep(1000);
        } catch (Exception err) {
        }

        for (int i = 0; i < nodes.length; i++) {
            nodes[i].shutdown();
        }

        try {
            bout.close();
            String output = new String(bout.toByteArray());
            setOrigOut();
            int count = countSubstring("Recieved Object, comparing..", output);
            Assert.assertEquals(new Integer(5), new Integer(count));
        } catch (Exception err) {
            err.printStackTrace();
        }
    }

    @Test
    public void testUnicast() {
        TypeDescriptorsContainer container = new TypeDescriptorsContainer("./node1");
        AutonomousAgentManager nodes[] = new AutonomousAgentManager[10];
        AutonomousAgent agent[] = new AutonomousAgent[nodes.length];
        NetworkID destination = null;
        for (int i = 0; i < nodes.length; i++) {
            nodes[i] = new AutonomousAgentManager(container);
            agent[i] = new TestAgent(nodes[i].getNetworkNode().getLocalHost(),
                    nodes[i]);
            if (i == 7)
                destination = agent[i].getAgentID();
        }

        System.out.println("Ready!");
        agent[2].send(new Message(0,createTestObject()), destination);

        try {
            Thread.sleep(1000);
        } catch (Exception err) {
        }

        for (int i = 0; i < nodes.length; i++) {
            nodes[i].shutdown();
        }

        try {
            bout.close();
            String output = new String(bout.toByteArray());
            setOrigOut();
            int count = countSubstring("Recieved Object, comparing..", output);
            Assert.assertEquals(new Integer(1), new Integer(count));
        } catch (Exception err) {
            err.printStackTrace();
        }
    }

    @Test
    public void testCMapString() {
        TypeDescriptorsContainer container = new TypeDescriptorsContainer("./node1");
        AutonomousAgentManager m1 = new AutonomousAgentManager(container);
        AutonomousAgentManager m2 = new AutonomousAgentManager(container);
        CMap<String, String> map1 = new CMap<>(125, m1,252,null);
        CMap<String, String> map2 = new CMap<>(125, m2,252,null);

        map1.put("TestKey1", "Value1");
        map1.put("TestKey2", "Value2");
        try {
            Thread.sleep(1000);
        } catch (Exception err) {
            err.printStackTrace();
        }
        Assert.assertEquals(map1.get("TestKey1"), map2.get("TestKey1"));
        Assert.assertEquals(map1.get("TestKey2"), map2.get("TestKey2"));
        map2.put("TestKey3", "Value3");
        map2.put("TestKey4", "Value4");
        try {
            Thread.sleep(1000);
        } catch (Exception err) {
            err.printStackTrace();
        }
        Assert.assertEquals(map2.get("TestKey3"), map1.get("TestKey3"));
        Assert.assertEquals(map2.get("TestKey4"), map1.get("TestKey4"));
        m1.shutdown();
        m2.shutdown();
    }

    @Test
    public void testCMapTestObject() {
        setOrigOut();
        TypeDescriptorsContainer container = new TypeDescriptorsContainer("./node1");
        AutonomousAgentManager m1 = new AutonomousAgentManager(container);
        AutonomousAgentManager m2 = new AutonomousAgentManager(container);
        CMap<String, TestObject> map1 = new CMap<>(125, m1,252,null);
        CMap<String, TestObject> map2 = new CMap<>(125, m2,252,null);

        map1.put("TestKey1", createTestObject());
        map1.put("TestKey2", createTestObject());
        try {
            Thread.sleep(1000);
        } catch (Exception err) {
            err.printStackTrace();
        }
        Assert.assertEquals(map1.get("TestKey1"), map2.get("TestKey1"));
        Assert.assertEquals(map1.get("TestKey2"), map2.get("TestKey2"));
        map2.put("TestKey3", createTestObject());
        map2.put("TestKey4", createTestObject());
        try {
            Thread.sleep(1000);
        } catch (Exception err) {
            err.printStackTrace();
        }
        Assert.assertEquals(map2.get("TestKey3"), map1.get("TestKey3"));
        Assert.assertEquals(map2.get("TestKey4"), map1.get("TestKey4"));
        m1.shutdown();
        m2.shutdown();
    }

    public static int countSubstring(String substr, String output) {
        System.out.println(output);
        int index = output.indexOf(substr);
        int count = 0;
        while (index != -1) {
            count++;
            index = output.indexOf(substr, index + 1);
        }
        return count;
    }

    public static TestObject createTestObject() {
        TestObject object = new TestObject();
        object.setName("Test me");
        object.setAddress("My Test address");
        object.setZipcode(95014);
        object.setSocial(55366354);
        return object;
    }
    @Test
    public void testUnreachable(){
        setOrigOut();
        TypeDescriptorsContainer container = new TypeDescriptorsContainer("./node1");
        AutonomousAgentManager m1 = new AutonomousAgentManager(container);
        CMap<String, TestObject> map1 = new CMap<>(125, m1,252,null);

        map1.put("TestKey1", createTestObject());
        map1.put("TestKey2", createTestObject());

        AutonomousAgentManager m2 = new AutonomousAgentManager(container);
        CMap<String, TestObject> map2 = new CMap<>(125, m2,252,null);
        map2.put("TestKey7", createTestObject());
        map1.remove("TestKey1");

        AutonomousAgentManager m3 = new AutonomousAgentManager(container);
        CMap<String, TestObject> map3 = new CMap<>(125, m3,252,null);

        map1.put("TestKey1", createTestObject());
        map2.put("TestKey3", createTestObject());
        map3.put("TestKey4", createTestObject());

        System.out.println("Sleeping 5 seconds to allow map1,2 & 3 to sync");
        try {
            Thread.sleep(5000);
        } catch (Exception err) {
            err.printStackTrace();
        }
        Assert.assertEquals(map1.get("TestKey1"), map2.get("TestKey1"));
        Assert.assertEquals(map1.get("TestKey2"), map2.get("TestKey2"));
        Assert.assertEquals(map1.get("TestKey2"), map3.get("TestKey2"));
        Assert.assertEquals(map1.get("TestKey3"), map3.get("TestKey3"));
        Assert.assertEquals(map1.get("TestKey4"), map2.get("TestKey4"));

        AutonomousAgentManager m4 = new AutonomousAgentManager(container);
        CMap<String, TestObject> map4 = new CMap<>(125, m4,252,null);
        map4.put("TestKey11", createTestObject());

        try{Thread.sleep(2000);}catch(Exception err){}

        map2._ForTestOnly_pseudoSendEnabled = true;
        map1._ForTestOnly_pseudoSendEnabled = true;
        map3._ForTestOnly_pseudoSendEnabled = true;

        m4.shutdown(); //Simulate node down/unreachable during synchronization
        System.out.println("Sleeping 6 seconds to allow node 4 shutdown...");
        try{Thread.sleep(6000);}catch(Exception err){}

        map2.put("TestKey3", createTestObject());
        map3.put("TestKey3", createTestObject());
        map2.put("TestKey4", createTestObject());

        map1.put("TestKey5", createTestObject());
        map3.remove("TestKey3");
        map2.remove("TestKeTestKey4y4");

        boolean firstTime = true;

        map2._ForTestOnly_pseudoSendEnabled = false;
        map1._ForTestOnly_pseudoSendEnabled = false;
        map1.sendARPBroadcast();
        map2.sendARPBroadcast();
        map1.put("TestKey6", createTestObject());
        map3._ForTestOnly_pseudoSendEnabled = false;
        map3.sendARPBroadcast();
        map4.sendARPBroadcast();

        m4 = new AutonomousAgentManager(container);
        map4 = new CMap<>(125, m4,252,null);

        try {
            System.out.println("Sleeping 5 seconds to allow node 4 to load and sync");
            Thread.sleep(5000);
        } catch (Exception err) {
            err.printStackTrace();
        }

        Assert.assertEquals(map2.get("TestKey3"), map1.get("TestKey3"));
        Assert.assertEquals(map2.get("TestKey4"), map1.get("TestKey4"));
        Assert.assertEquals(map2.get("TestKey4"), map3.get("TestKey4"));
        Assert.assertEquals(map2.get("TestKey6"), map3.get("TestKey6"));
        Assert.assertEquals(map1.get("TestKey6"), map2.get("TestKey6"));
        Assert.assertEquals(map4.get("TestKey6"), map2.get("TestKey6"));

        Assert.assertEquals(8, map1.size());
        Assert.assertEquals(8, map2.size());
        Assert.assertEquals(8, map3.size());
        Assert.assertEquals(8, map4.size());

        map2._ForTestOnly_pseudoSendEnabled = true;
        map1._ForTestOnly_pseudoSendEnabled = true;
        map3._ForTestOnly_pseudoSendEnabled = true;
        map4._ForTestOnly_pseudoSendEnabled = true; //Simulate node timeout during synchronization

        map2.put("TestKey8", createTestObject());

        map2._ForTestOnly_pseudoSendEnabled = false;
        map1._ForTestOnly_pseudoSendEnabled = false;
        map3._ForTestOnly_pseudoSendEnabled = false;
        map1.sendARPBroadcast();
        map2.sendARPBroadcast();
        map3.sendARPBroadcast();

        try {
            System.out.println("Sleeping 10 seconds to allow nodes to sync after timeout");
            Thread.sleep(10000);
        } catch (Exception err) {
            err.printStackTrace();
        }
        map4._ForTestOnly_pseudoSendEnabled = false;
        map4.sendARPBroadcast();
        map1.sendARPBroadcast();
        try {
            Thread.sleep(20000);
        } catch (Exception err) {
            err.printStackTrace();
        }

        Assert.assertEquals(map3.get("TestKey8"), map2.get("TestKey8"));
        Assert.assertEquals(map4.get("TestKey8"), map1.get("TestKey8"));

        System.out.println("Finish");
        m1.shutdown();
        m2.shutdown();
        m3.shutdown();
        m4.shutdown();
        //m4.shutdown();
    }

    @Test
    public void testClusterTypeDescriptors(){

        File node1 = new File("./node1");
        node1.mkdirs();
        TypeDescriptorsContainer container1 = new TypeDescriptorsContainer("./node1");
        AutonomousAgentManager m1 = new AutonomousAgentManager(container1);
        CMap<String, TypeDescriptor> cm1 = new CMap<>(223, m1,255,new TypeDescriptorListener<String,TypeDescriptor>(container1));
        container1.setClusterMap(cm1);

        File node2 = new File("./node2");
        node2.mkdirs();
        TypeDescriptorsContainer container2 = new TypeDescriptorsContainer("./node2");
        AutonomousAgentManager m2 = new AutonomousAgentManager(container2);
        CMap<String, TypeDescriptor> cm2 = new CMap<>(223, m2,255,new TypeDescriptorListener<String,TypeDescriptor>(container2));
        container2.setClusterMap(cm2);


        TestObject to = createTestObject();
        TypeDescriptor td = container1.getTypeDescriptorByObject(to);
        try{Thread.sleep(5000);}catch(Exception err){}
        TypeDescriptor td2 = container2.checkTypeDescriptorByClass(TestObject.class);
        Assert.assertEquals(true, td2!=null);
        m1.shutdown();
        m2.shutdown();
    }

    @AfterClass
    public static void clean(){
        File f = new File("./node1");
        deleteDirectory(f);
        f = new File("./node2");
        deleteDirectory(f);
    }

    public static void deleteDirectory(File dir) {
        File files[] = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory())
                    deleteDirectory(file);
                else
                    file.delete();
            }
        }
        dir.delete();
    }
}
