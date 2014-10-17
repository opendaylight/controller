package org.opendaylight.mdsal.persisted.net;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.persisted.net.NetworkID;
import org.opendaylight.persisted.net.NetworkNode;
import org.opendaylight.persisted.net.NetworkNodeConnection;

public class NetworkTest {
    
    private ByteArrayOutputStream bout = null;
    private PrintStream orig = System.out;
    
    @Before
    public void before(){
        try{
            bout = new ByteArrayOutputStream();
            System.setOut(new PrintStream(bout));
        }catch(Exception err){
            err.printStackTrace();
        }
    }
    
    @After
    public void after(){
        System.setOut(orig);
    }
    
    @Test
    public void testBroadcast(){
        NetworkNode nodes[] = new NetworkNode[10];
        for(int i=0;i<10;i++){
            nodes[i] = new NetworkNode(null);
        }
        try{Thread.sleep(5000);}catch(Exception err){}      
        System.out.println("Ready");
        nodes[3].send(new byte[5], nodes[3].getLocalHost(), NetworkNodeConnection.PROTOCOL_ID_BROADCAST);
        NetworkID unreach = new NetworkID(nodes[3].getLocalHost().getIPv4Address(), 56565, 0);
        nodes[3].send(new byte[5], nodes[3].getLocalHost(),unreach);
        try{Thread.sleep(5000);}catch(Exception err){}      
        for(int i=0;i<nodes.length;i++){
            nodes[i].shutdown();
        }        
        try{
            bout.close();
            String output = new String(bout.toByteArray());
            after();
            System.out.println(output);
            int index =output.indexOf("Dest=0.0.0.0:0:10");
            int count = 0;
            while(index!=-1){
                count++;
                index = output.indexOf("Dest=0.0.0.0:0:10",index+1);
            }
            Assert.assertEquals(new Integer(10), new Integer(count));
            Assert.assertEquals(true, output.indexOf("Unreachable")!=-1);
        }catch(Exception err){
            err.printStackTrace();
        }
    }
        
}
