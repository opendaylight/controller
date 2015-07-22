package org.opendaylight.controller.cluster.raft;

import static org.junit.Assert.assertEquals;
import org.junit.Test;
import org.opendaylight.controller.cluster.raft.policy.DefaultRaftPolicy;
import org.opendaylight.controller.cluster.raft.policy.RaftPolicy;
import org.opendaylight.controller.cluster.raft.policy.TestRaftPolicy;

public class DefaultConfigParamsImplTest {

    @Test
    public void testGetRaftPolicyWithDefault(){
        DefaultConfigParamsImpl params = new DefaultConfigParamsImpl();

        assertEquals("Default instance", DefaultRaftPolicy.INSTANCE, params.getRaftPolicy());
    }

    @Test
    public void testGetRaftPolicyInvalidClassName(){
        DefaultConfigParamsImpl params = new DefaultConfigParamsImpl();
        params.setCustomRaftPolicyImplementationClass("foobar");

        assertEquals("Default instance", DefaultRaftPolicy.INSTANCE, params.getRaftPolicy());
    }

    @Test
    public void testGetRaftPolicyValidClassNameButInvalidType(){
        DefaultConfigParamsImpl params = new DefaultConfigParamsImpl();
        params.setCustomRaftPolicyImplementationClass("java.lang.String");

        assertEquals("Default instance", DefaultRaftPolicy.INSTANCE, params.getRaftPolicy());
    }

    @Test
    public void testGetRaftPolicyValidClass(){
        DefaultConfigParamsImpl params1 = new DefaultConfigParamsImpl();
        params1.setCustomRaftPolicyImplementationClass("org.opendaylight.controller.cluster.raft.policy.TestRaftPolicy");
        RaftPolicy behavior1 = params1.getRaftPolicy();

        assertEquals("TestCustomBehavior", TestRaftPolicy.class, behavior1.getClass());
        assertEquals("Same instance returned", behavior1, params1.getRaftPolicy());

        DefaultConfigParamsImpl params2 = new DefaultConfigParamsImpl();
        RaftPolicy behavior2 = params2.getRaftPolicy();
        params1.setCustomRaftPolicyImplementationClass("org.opendaylight.controller.cluster.raft.policy.TestRaftPolicy");

        assertEquals("Default instance", DefaultRaftPolicy.INSTANCE, behavior2);
        assertEquals("Default instance", DefaultRaftPolicy.INSTANCE, params2.getRaftPolicy());

    }


}