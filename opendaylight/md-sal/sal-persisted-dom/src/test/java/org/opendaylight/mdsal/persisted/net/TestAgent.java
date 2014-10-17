package org.opendaylight.mdsal.persisted.net;

import org.junit.Assert;
import org.opendaylight.persisted.autoagents.AutonomousAgent;
import org.opendaylight.persisted.autoagents.AutonomousAgentManager;
import org.opendaylight.persisted.net.NetworkID;
import org.opendaylight.persisted.net.Packet;

public class TestAgent extends AutonomousAgent {

    private TestObject testObject = NetworkTest.createTestObject();

    public TestAgent(NetworkID localHost, AutonomousAgentManager m) {
        super(19, m);
        TestObject o = new TestObject();
        m.getTypeDescriptorsContainer().getTypeDescriptorByObject(o);
    }

    @Override
    public void processNext(Packet frame, Object obj) {
        if (obj == null) {
            System.out.println("Received a currapted frame");
        } else {
            System.out.println("Recieved Object, comparing...");
            Assert.assertEquals(testObject, obj);
        }
    }

    @Override
    public void start() {
        // TODO Auto-generated method stub

    }

    @Override
    public String getName() {
        return "Test Agent";
    }

}
