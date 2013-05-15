package org.opendaylight.controller.sal.rest.transform;

import org.opendaylight.controller.concepts.transform.AggregateTransformer;
import org.opendaylight.controller.concepts.transform.InputClassBasedTransformer;
import org.opendaylight.controller.sal.action.Action;
import org.opendaylight.controller.sal.rest.action.ActionRTO;

public interface ActionToRestTransformer extends
    InputClassBasedTransformer<Action, Action, ActionRTO>,
    AggregateTransformer<Action, ActionRTO> {
    
    
    void addTransformer(InputClassBasedTransformer<Action, ? extends Action, ActionRTO> transformer);

}
