package org.opendaylight.controller.cluster.raft;

import static org.junit.Assert.assertEquals;
import org.junit.Test;
import org.opendaylight.controller.cluster.raft.utils.TestCustomRaftBehavior;

public class DefaultConfigParamsImplTest {

    @Test
    public void testGetCustomRaftBehavior(){
        DefaultConfigParamsImpl params = new DefaultConfigParamsImpl();

        assertEquals("Default instance", DefaultRaftBehavior.INSTANCE, params.getCustomRaftBehavior());
    }

    @Test
    public void testGetCustomRaftBehaviorInvalidClassName(){
        DefaultConfigParamsImpl params = new DefaultConfigParamsImpl();
        params.setCustomRaftActorImplementation("foobar");

        assertEquals("Default instance", DefaultRaftBehavior.INSTANCE, params.getCustomRaftBehavior());
    }

    @Test
    public void testGetCustomRaftBehaviorValidClassNameButInvalidType(){
        DefaultConfigParamsImpl params = new DefaultConfigParamsImpl();
        params.setCustomRaftActorImplementation("java.lang.String");

        assertEquals("Default instance", DefaultRaftBehavior.INSTANCE, params.getCustomRaftBehavior());
    }

    @Test
    public void testGetCustomRaftBehaviorValidClass(){
        DefaultConfigParamsImpl params1 = new DefaultConfigParamsImpl();
        params1.setCustomRaftActorImplementation("org.opendaylight.controller.cluster.raft.utils.TestCustomRaftBehavior");
        CustomizableRaftBehavior behavior1 = params1.getCustomRaftBehavior();

        assertEquals("TestCustomBehavior", TestCustomRaftBehavior.class, behavior1.getClass());
        assertEquals("Same instance returned", behavior1, params1.getCustomRaftBehavior());

        DefaultConfigParamsImpl params2 = new DefaultConfigParamsImpl();
        CustomizableRaftBehavior behavior2 = params2.getCustomRaftBehavior();
        params1.setCustomRaftActorImplementation("org.opendaylight.controller.cluster.raft.utils.TestCustomRaftBehavior");

        assertEquals("Default instance", DefaultRaftBehavior.INSTANCE, behavior2);
        assertEquals("Default instance", DefaultRaftBehavior.INSTANCE, params2.getCustomRaftBehavior());

    }


}