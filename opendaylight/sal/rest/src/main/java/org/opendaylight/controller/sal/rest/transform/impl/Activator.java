package org.opendaylight.controller.sal.rest.transform.impl;

import org.apache.felix.dm.Component;
import org.opendaylight.controller.sal.core.ComponentActivatorAbstractBase;
import org.opendaylight.controller.sal.rest.transform.ActionToRestTransformer;
import org.opendaylight.controller.sal.rest.transform.FlowOnNodeToRestTransformer;
import org.opendaylight.controller.sal.rest.transform.FlowToRestTransformer;

public class Activator extends ComponentActivatorAbstractBase {

    @Override
    protected void configureGlobalInstance(Component c, Object imp) {
        if (imp.equals(SALActionToRestTransformerImpl.class)) {
            c.setInterface(ActionToRestTransformer.class.getName(), null);
        } else if (imp.equals(FlowOnNodeToRestTransformerImpl.class)) {
            c.setInterface(FlowOnNodeToRestTransformer.class.getName(), null);
            c.add(createServiceDependency().setService(
                    FlowToRestTransformer.class).setCallbacks(
                    "setFlowTransformer", "unsetFlowTransformer"));
        } else if (imp.equals(FlowToRestTransformerImpl.class)) {
            c.setInterface(FlowToRestTransformer.class.getName(), null);
            c.add(createServiceDependency().setService(
                    ActionToRestTransformer.class).setCallbacks(
                    "setActionTransformer", "unsetActionTransformer"));
        }
    }

    @Override
    protected Object[] getGlobalImplementations() {
        Object[] ret = { SALActionToRestTransformerImpl.class,
                FlowOnNodeToRestTransformerImpl.class,FlowToRestTransformerImpl.class
        };
        return ret;
    }

    @Override
    protected void init() {
    }

    @Override
    protected void destroy() {
    }

}
