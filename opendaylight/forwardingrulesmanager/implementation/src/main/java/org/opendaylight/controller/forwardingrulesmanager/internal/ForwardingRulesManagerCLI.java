package org.opendaylight.controller.forwardingrulesmanager.internal;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

import org.apache.felix.service.command.Descriptor;
import org.opendaylight.controller.forwardingrulesmanager.FlowEntry;
import org.opendaylight.controller.forwardingrulesmanager.IForwardingRulesManager;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.utils.ServiceHelper;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class provides osgi cli commands for developers to debug
 * ForwardingRulesManager functionality
 */
public class ForwardingRulesManagerCLI {
    protected static final Logger logger = LoggerFactory.getLogger(ForwardingRulesManagerCLI.class);
    @SuppressWarnings("rawtypes")
    private ServiceRegistration sr = null;

    public void init() {
    }

    public void destroy() {
    }

    public void start() {
        final Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put("osgi.command.scope", "odpcontroller");
        props.put("osgi.command.function", new String[] { "showRequestedGroupFlows", "showInstalledGroupFlows",
                "showRequestedNodeFlows", "showInstalledNodeFlows" });
        this.sr = ServiceHelper.registerGlobalServiceWReg(ForwardingRulesManagerCLI.class, this, props);
    }

    public void stop() {
        if (this.sr != null) {
            this.sr.unregister();
            this.sr = null;
        }
    }

    @Descriptor("Displays all the flow entries in a given group")
    public void showRequestedGroupFlows(@Descriptor("Container in which to query FRM") String container,
            @Descriptor("Group name") String group, @Descriptor("True for verbose else false") Boolean verbose) {
        IForwardingRulesManager frm = (IForwardingRulesManager) ServiceHelper.getInstance(
                IForwardingRulesManager.class, container, this);
        if (frm == null) {
            logger.info("Cannot find the FRM instance on container: ", container);
            return;
        }
        List<FlowEntry> groupFlows = frm.getFlowEntriesForGroup(group);
        logger.info("Group ", group);
        for (FlowEntry flowEntry : groupFlows) {
            if (!verbose) {
                logger.error(flowEntry.getNode().toString());
                logger.error(" ", flowEntry.getFlowName());
            } else {
                logger.error(flowEntry.getNode().toString());
                logger.error(" ", flowEntry.toString());
            }
        }
    }

    @Descriptor("Displays all the installed flow entries in a given group")
    public void showInstalledGroupFlows(@Descriptor("Container in which to query FRM") String container,
            @Descriptor("Group name") String group, @Descriptor("True for verbose else false") Boolean verbose) {
        IForwardingRulesManager frm = (IForwardingRulesManager) ServiceHelper.getInstance(
                IForwardingRulesManager.class, container, this);
        if (frm == null) {
            logger.error("Cannot find the FRM instance on container: ", container);
            return;
        }
        List<FlowEntry> groupFlows = frm.getInstalledFlowEntriesForGroup(group);
        logger.info("Group ", group);
        for (FlowEntry flowEntry : groupFlows) {
            if (!verbose) {
                logger.error(flowEntry.getNode().toString());
                logger.error(" ", flowEntry.getFlowName());
            } else {
                logger.error(flowEntry.getNode().toString());
                logger.error(" ", flowEntry.toString());
            }
        }
    }

    @Descriptor("Displays all the flow entries for a network node")
    public void showRequestedNodeFlows(
            @Descriptor("Container in which to query FRM") String container,
            @Descriptor("String representation of the Node, this need to be consumable from Node.fromString()") String nodeId,
            @Descriptor("True for verbose else false") Boolean verbose) {
        IForwardingRulesManager frm = (IForwardingRulesManager) ServiceHelper.getInstance(
                IForwardingRulesManager.class, container, this);
        if (frm == null) {
            logger.error("Cannot find the FRM instance on container: ", container);
            return;
        }
        Node node = Node.fromString(nodeId);
        if (node == null) {
            logger.info("Please enter a valid node id");
            return;
        }
        List<FlowEntry> groupFlows = frm.getFlowEntriesForNode(node);
        logger.info("Node " + nodeId);
        for (FlowEntry flowEntry : groupFlows) {
            if (!verbose) {
                logger.error(flowEntry.getNode().toString());
                logger.error(" ", flowEntry.getFlowName());
            } else {
                logger.error(flowEntry.getNode().toString());
                logger.error(" ", flowEntry.toString());
            }
        }
    }

    @Descriptor("Displays all the flow entries installed in a network node")
    public void showInstalledNodeFlows(
            @Descriptor("Container in which to query FRM") String container,
            @Descriptor("String representation of the Node, this need to be consumable from Node.fromString()") String nodeId,
            @Descriptor("True for verbose else false") Boolean verbose) {
        IForwardingRulesManager frm = (IForwardingRulesManager) ServiceHelper.getInstance(
                IForwardingRulesManager.class, container, this);
        if (frm == null) {
            logger.info("Cannot find the FRM instance on container: ", container);
            return;
        }
        Node node = Node.fromString(nodeId);
        if (node == null) {
            logger.info("Please enter a valid node id");
            return;
        }
        List<FlowEntry> groupFlows = frm.getInstalledFlowEntriesForNode(node);
        logger.info("Node ", nodeId);
        for (FlowEntry flowEntry : groupFlows) {
            if (!verbose) {
                logger.error(flowEntry.getNode().toString());
                logger.error(" ", flowEntry.getFlowName());
            } else {
                logger.error(flowEntry.getNode().toString());
                logger.error(" ", flowEntry.toString());
            }
        }
    }
}
