package org.opendaylight.controller.sal.rest.transform.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.opendaylight.controller.sal.flowprogrammer.Flow;
import org.opendaylight.controller.sal.rest.action.ActionRTO;
import org.opendaylight.controller.sal.rest.flow.FlowRTO;
import org.opendaylight.controller.sal.rest.transform.ActionToRestTransformer;
import org.opendaylight.controller.sal.rest.transform.FlowToRestTransformer;

public class FlowToRestTransformerImpl implements FlowToRestTransformer {

    private ActionToRestTransformer actionTransformer;

    @Override
    public Collection<FlowRTO> transformAll(Collection<? extends Flow> inputs) {
        List<FlowRTO> ret = new ArrayList<FlowRTO>();
        for (Flow flow : inputs) {
            ret.add(transform(flow));
        }
        return ret;
    }

    @Override
    public FlowRTO transform(Flow input) {

        List<ActionRTO> actions = new ArrayList<ActionRTO>(
                actionTransformer.transformAll(input.getActions()));
        FlowRTO ret = new FlowRTO(input.getMatch(), actions);
        ret.setHardTimeout(input.getHardTimeout());
        ret.setIdleTimeout(input.getIdleTimeout());
        ret.setId(input.getId());
        ret.setPriority(input.getPriority());
        
        return ret;
    }

    public ActionToRestTransformer getActionTransformer() {
        return actionTransformer;
    }

    public void setActionTransformer(ActionToRestTransformer actionTransformer) {
        this.actionTransformer = actionTransformer;
    }
    
    public void unsetActionTransformer() {
        this.actionTransformer = null;
    }

}
