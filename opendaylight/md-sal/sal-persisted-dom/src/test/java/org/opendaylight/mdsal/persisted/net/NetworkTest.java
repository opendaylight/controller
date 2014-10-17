package org.opendaylight.mdsal.persisted.net;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.persisted.autoagents.AutonomousAgent;
import org.opendaylight.persisted.autoagents.AutonomousAgentManager;
import org.opendaylight.persisted.autoagents.ClusterMap;
import org.opendaylight.persisted.codec.TypeDescriptorsContainer;
import org.opendaylight.persisted.net.NetworkID;
import org.opendaylight.persisted.net.NetworkNode;
import org.opendaylight.persisted.net.NetworkNodeConnection;

public class NetworkTest {

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
        TypeDescriptorsContainer container = new TypeDescriptorsContainer("./");
        AutonomousAgentManager nodes[] = new AutonomousAgentManager[10];
        AutonomousAgent agent[] = new AutonomousAgent[nodes.length];
        for (int i = 0; i < nodes.length; i++) {
            nodes[i] = new AutonomousAgentManager(container);
            agent[i] = new TestAgent(nodes[i].getNetworkNode().getLocalHost(),
                    nodes[i]);
        }

        try {
            Thread.sleep(1000);
        } catch (Exception err) {
        }

        System.out.println("Ready!");
        agent[4].send(createTestObject(),
                NetworkNodeConnection.PROTOCOL_ID_BROADCAST);

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
        TypeDescriptorsContainer container = new TypeDescriptorsContainer("./");
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
        agent[2].send(createTestObject(), multiCast);

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
        TypeDescriptorsContainer container = new TypeDescriptorsContainer("./");
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
        agent[2].send(createTestObject(), destination);

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
    public void testClusterMapString() {
        TypeDescriptorsContainer container = new TypeDescriptorsContainer("./");
        AutonomousAgentManager m1 = new AutonomousAgentManager(container);
        AutonomousAgentManager m2 = new AutonomousAgentManager(container);
        ClusterMap<String, String> map1 = new ClusterMap<>(125, m1
                .getNetworkNode().getLocalHost(), m1);
        ClusterMap<String, String> map2 = new ClusterMap<>(125, m2
                .getNetworkNode().getLocalHost(), m2);
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
    public void testClusterMapTestObject() {
        setOrigOut();
        TypeDescriptorsContainer container = new TypeDescriptorsContainer("./");
        AutonomousAgentManager m1 = new AutonomousAgentManager(container);
        AutonomousAgentManager m2 = new AutonomousAgentManager(container);
        ClusterMap<String, TestObject> map1 = new ClusterMap<>(125, m1
                .getNetworkNode().getLocalHost(), m1);
        ClusterMap<String, TestObject> map2 = new ClusterMap<>(125, m2
                .getNetworkNode().getLocalHost(), m2);
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

    public static void main(String args[]){
        TypeDescriptorsContainer container = new TypeDescriptorsContainer("./");
        AutonomousAgentManager m1 = new AutonomousAgentManager(container);
        AutonomousAgentManager m2 = new AutonomousAgentManager(container);
        ClusterMap<String, TestObject> map1 = new ClusterMap<>(125, m1.getNetworkNode().getLocalHost(), m1);
        ClusterMap<String, TestObject> map2 = new ClusterMap<>(125, m2.getNetworkNode().getLocalHost(), m2);
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
}
