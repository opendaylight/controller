package org.opendaylight.controller.sal.rest.transform.test;
import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.sal.action.Action;
import org.opendaylight.controller.sal.action.SetVlanId;
import org.opendaylight.controller.sal.rest.action.ActionRTO;
import org.opendaylight.controller.sal.rest.action.Drop;
import org.opendaylight.controller.sal.rest.transform.impl.SALActionToRestTransformerImpl;
import org.opendaylight.controller.concepts.transform.CompositeClassBasedTransformer;
import org.opendaylight.controller.concepts.transform.InputClassBasedTransformer;


public class SALActionsToRestTest {

    
    
    private CompositeClassBasedTransformer<Action, ActionRTO> transformService;
    private InputClassBasedTransformer<Action, Action, ActionRTO> transformer;
    
    @Before
    public void init(){
        transformService = new SALActionToRestTransformerImpl();
        transformer = transformService;
        
    }
    
    @Test
    public void test() {
        ActionRTO drop = transformer.transform(new org.opendaylight.controller.sal.action.Drop());
        assertTrue( drop instanceof Drop);
        
        ActionRTO setVlan = transformer.transform(new SetVlanId(56));
        assertTrue(setVlan instanceof org.opendaylight.controller.sal.rest.action.SetVlanId);
        
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void testUnknownInput() {
        transformer.transform(new Action(){
        });
    }

}
