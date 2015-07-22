package org.opendaylight.controller.cluster.raft;

import static org.junit.Assert.assertEquals;
import org.junit.Test;
import org.opendaylight.controller.cluster.raft.policy.DefaultRaftPolicy;
import org.opendaylight.controller.cluster.raft.policy.RaftPolicy;
import org.opendaylight.controller.cluster.raft.utils.TestCustomRaftBehavior;

public class DefaultConfigParamsImplTest {

    @Test
    public void testGetCustomRaftBehavior(){
        DefaultConfigParamsImpl params = new DefaultConfigParamsImpl();

        assertEquals("Default instance", DefaultRaftPolicy.INSTANCE, params.getCustomRaftPolicy());
    }

    @Test
    public void testGetCustomRaftBehaviorInvalidClassName(){
        DefaultConfigParamsImpl params = new DefaultConfigParamsImpl();
        params.setCustomRaftActorImplementation("foobar");

        assertEquals("Default instance", DefaultRaftPolicy.INSTANCE, params.getCustomRaftPolicy());
    }

    @Test
    public void testGetCustomRaftBehaviorValidClassNameButInvalidType(){
        DefaultConfigParamsImpl params = new DefaultConfigParamsImpl();
        params.setCustomRaftActorImplementation("java.lang.String");

        assertEquals("Default instance", DefaultRaftPolicy.INSTANCE, params.getCustomRaftPolicy());
    }

    @Test
    public void testGetCustomRaftBehaviorValidClass(){
        DefaultConfigParamsImpl params1 = new DefaultConfigParamsImpl();
        params1.setCustomRaftActorImplementation("org.opendaylight.controller.cluster.raft.utils.TestCustomRaftBehavior");
        RaftPolicy behavior1 = params1.getCustomRaftPolicy();

        assertEquals("TestCustomBehavior", TestCustomRaftBehavior.class, behavior1.getClass());
        assertEquals("Same instance returned", behavior1, params1.getCustomRaftPolicy());

        DefaultConfigParamsImpl params2 = new DefaultConfigParamsImpl();
        RaftPolicy behavior2 = params2.getCustomRaftPolicy();
        params1.setCustomRaftActorImplementation("org.opendaylight.controller.cluster.raft.utils.TestCustomRaftBehavior");

        assertEquals("Default instance", DefaultRaftPolicy.INSTANCE, behavior2);
        assertEquals("Default instance", DefaultRaftPolicy.INSTANCE, params2.getCustomRaftPolicy());

    }


}