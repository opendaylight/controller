package org.opendaylight.controller.sal.rest.transform.impl;

import org.opendaylight.controller.sal.action.Action;
import org.opendaylight.controller.sal.rest.action.ActionRTO;
import org.opendaylight.controller.sal.rest.transform.ActionToRestTransformer;
import org.opendaylight.controller.concepts.transform.CompositeClassBasedTransformer;

public final class SALActionToRestTransformerImpl extends
        CompositeClassBasedTransformer<Action, ActionRTO> implements
        ActionToRestTransformer {

    public SALActionToRestTransformerImpl() {
        ActionsToRest.registerBaseActionTransformers(this);
    }

    @Override
    public Class<? extends Action> getInputClass() {
        return Action.class;
    }

}
