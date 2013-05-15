package org.opendaylight.controller.protocol_plugin.openflow.mapping.impl;

import java.util.HashSet;
import java.util.Set;

import org.opendaylight.controller.concepts.transform.SimpleConditionalTransformer;
import org.opendaylight.controller.sal.action.Action;

public class BitMaskToActionClassTransformer implements SimpleConditionalTransformer<Integer,Set<Class<? extends Action>>>{
    
    
    /**
     * Extracts a set of supported {@link Action} from input bit field.
     * 
     */
    @Override
    public Set<Class<? extends Action>> transform(Integer input) {
        Set<Class<? extends Action>> ret = new HashSet<Class<? extends Action>>();
        
        
        
        
        return ret;
    }

    @Override
    public boolean isAcceptable(Integer input) {
        if(input != null) return true;
        throw new IllegalArgumentException("Input must not be null.");
    }


    
}
