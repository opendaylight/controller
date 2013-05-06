package org.opendaylight.controller.protocol_plugins.stub.internal;

import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.felix.dm.Component;

import org.opendaylight.controller.sal.core.ComponentActivatorAbstractBase;
import org.opendaylight.controller.sal.core.IContainerListener;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.discovery.IDiscoveryService;
import org.opendaylight.controller.sal.flowprogrammer.IPluginInFlowProgrammerService;
import org.opendaylight.controller.sal.inventory.IPluginInInventoryService;
import org.opendaylight.controller.sal.inventory.IPluginOutInventoryService;
import org.opendaylight.controller.sal.packet.IPluginInDataPacketService;
import org.opendaylight.controller.sal.packet.IPluginOutDataPacketService;
import org.opendaylight.controller.sal.reader.IPluginInReadService;
import org.opendaylight.controller.sal.reader.IReadService;
import org.opendaylight.controller.sal.topology.IPluginInTopologyService;
import org.opendaylight.controller.sal.topology.IPluginOutTopologyService;
import org.opendaylight.controller.sal.utils.GlobalConstants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * stub protocol plugin Activator
 * 
 * 
 */
public class Activator extends ComponentActivatorAbstractBase {
    protected static final Logger logger = LoggerFactory
            .getLogger(Activator.class);

    /**
     * Function called when the activator starts just after some initializations
     * are done by the ComponentActivatorAbstractBase.
     * 
     */
    public void init() {
        Node.NodeIDType.registerIDType("STUB", Integer.class);
        NodeConnector.NodeConnectorIDType.registerIDType("STUB", Integer.class, "STUB");
    }

    /**
     * Function called when the activator stops just before the cleanup done by
     * ComponentActivatorAbstractBase
     * 
     */
    public void destroy() {
        Node.NodeIDType.unRegisterIDType("STUB");
        NodeConnector.NodeConnectorIDType.unRegisterIDType("STUB");
    }

    /**
     * Function that is used to communicate to dependency manager the list of
     * known implementations for services inside a container
     * 
     * 
     * @return An array containing all the CLASS objects that will be
     *         instantiated in order to get an fully working implementation
     *         Object
     */
    public Object[] getImplementations() {
        Object[] res = { ReadService.class, InventoryService.class };
        return res;
    }

    /**
     * Function that is called when configuration of the dependencies is
     * required.
     * 
     * @param c
     *            dependency manager Component object, used for configuring the
     *            dependencies exported and imported
     * @param imp
     *            Implementation class that is being configured, needed as long
     *            as the same routine can configure multiple implementations
     * @param containerName
     *            The containerName being configured, this allow also optional
     *            per-container different behavior if needed, usually should not
     *            be the case though.
     */
    public void configureInstance(Component c, Object imp, String containerName) {
        if (imp.equals(ReadService.class)) {
            // export the service to be used by SAL
            Dictionary<String, Object> props = new Hashtable<String, Object>();
            // Set the protocolPluginType property which will be used
            // by SAL
            props.put("protocolPluginType", "STUB");
            c.setInterface(IPluginInReadService.class.getName(), props);
        }

        if (imp.equals(InventoryService.class)) {
            // export the service to be used by SAL
            Dictionary<String, Object> props = new Hashtable<String, Object>();
            // Set the protocolPluginType property which will be used
            // by SAL
            props.put("protocolPluginType", "STUB");
            c.setInterface(IPluginInInventoryService.class.getName(), props);
        }
    }
}
