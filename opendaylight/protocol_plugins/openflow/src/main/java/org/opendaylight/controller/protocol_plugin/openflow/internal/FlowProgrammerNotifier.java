package org.opendaylight.controller.protocol_plugin.openflow.internal;

import org.apache.felix.dm.Component;
import org.opendaylight.controller.protocol_plugin.openflow.IFlowProgrammerNotifier;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.flowprogrammer.Flow;
import org.opendaylight.controller.sal.flowprogrammer.IPluginOutFlowProgrammerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Flow Programmer Notifier class for relaying asynchronous messages received
 * from the network node to the listeners on the proper container
 */
public class FlowProgrammerNotifier implements IFlowProgrammerNotifier {
    protected static final Logger logger = LoggerFactory
            .getLogger(FlowProgrammerNotifier.class);
    private IPluginOutFlowProgrammerService salNotifier;

    public FlowProgrammerNotifier() {
        salNotifier = null;
    }

    void init(Component c) {
        logger.debug("INIT called!");
    }

    /**
     * Function called by the dependency manager when at least one dependency
     * become unsatisfied or when the component is shutting down because for
     * example bundle is being stopped.
     * 
     */
    void destroy() {
        logger.debug("DESTROY called!");
    }

    /**
     * Function called by dependency manager after "init ()" is called and after
     * the services provided by the class are registered in the service registry
     * 
     */
    void start() {
        logger.debug("START called!");
    }

    /**
     * Function called by the dependency manager before the services exported by
     * the component are unregistered, this will be followed by a "destroy ()"
     * calls
     * 
     */
    void stop() {
        logger.debug("STOP called!");
    }

    public void setPluginOutFlowProgrammerService(
            IPluginOutFlowProgrammerService s) {
        this.salNotifier = s;
    }

    public void unsetPluginOutFlowProgrammerService(
            IPluginOutFlowProgrammerService s) {
        if (this.salNotifier == s) {
            this.salNotifier = null;
        }
    }

    @Override
    public void flowRemoved(Node node, Flow flow) {
        if (salNotifier != null) {
            salNotifier.flowRemoved(node, flow);
        } else {
            logger.warn("Unable to relay switch message to upper layer");
        }
    }

}
