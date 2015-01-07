package org.opendaylight.datasand.network.tests;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.datasand.network.NetworkID;
import org.opendaylight.datasand.network.NetworkNode;
import org.opendaylight.datasand.network.NetworkNodeConnection;

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
