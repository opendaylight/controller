package org.opendaylight.controller.protocol_plugin.openflow.mapping.impl;

import static org.opendaylight.controller.protocol_plugin.openflow.mapping.impl.SALActionMappings.*;

import org.opendaylight.controller.concepts.transform.CompositeClassBasedTransformer;
import org.opendaylight.controller.protocol_plugin.openflow.mapping.api.SALToOFActionTransformer;
import org.opendaylight.controller.sal.action.Action;
import org.openflow.protocol.action.OFAction;

public class CompositeSALActionToOFTransformer extends
        CompositeClassBasedTransformer<Action, OFAction> implements
        SALToOFActionTransformer<Action> {
    
    public CompositeSALActionToOFTransformer() {
        
        // Init base transformers
        addTransformer(CONTROLLER);
        addTransformer(FLOOD);
        addTransformer(FLOOD_ALL);
        addTransformer(HW_PATH);
        addTransformer(LOOPBACK);
        addTransformer(OUTPUT);
        addTransformer(POP_VLAN);
        addTransformer(SET_DL_DST);
        addTransformer(SET_DL_SRC);
        addTransformer(SET_NW_DST);
        addTransformer(SET_NW_SRC);
        addTransformer(SET_NW_TOS);
        addTransformer(SET_TP_DST);
        addTransformer(SET_TP_SRC);
        addTransformer(SET_VLAN_ID);
        addTransformer(SET_VLAN_PCP);
        addTransformer(SW_PATH);
    }
    
    @Override
    public Class<? extends Action> getInputClass() {
        return Action.class;
    }

}
