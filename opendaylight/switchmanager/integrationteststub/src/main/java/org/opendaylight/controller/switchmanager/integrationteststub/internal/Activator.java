package org.opendaylight.controller.switchmanager.integrationteststub.internal;

import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.felix.dm.Component;

import org.opendaylight.controller.clustering.services.IClusterContainerServices;
//import org.opendaylight.controller.protocol_plugin.openflow.IInventoryShimInternalListener;
//import org.opendaylight.controller.protocol_plugin.openflow.IStatisticsListener;
//import org.opendaylight.controller.protocol_plugin.openflow.core.IController;
//import org.opendaylight.controller.protocol_plugin.openflow.internal.InventoryService;
//import org.opendaylight.controller.hosttracker.IfHostListener;
//import org.opendaylight.controller.hosttracker.IfIptoHost;
//import org.opendaylight.controller.hosttracker.IfNewHostNotify;
//import org.opendaylight.controller.hosttracker.hostAware.IHostFinder;
//import org.opendaylight.controller.hosttracker.internal.HostTracker;
import org.opendaylight.controller.sal.core.ComponentActivatorAbstractBase;
import org.opendaylight.controller.sal.core.IContainerListener;
import org.opendaylight.controller.sal.core.Node;
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
import org.opendaylight.controller.switchmanager.IInventoryListener;
import org.opendaylight.controller.switchmanager.ISwitchManager;
import org.opendaylight.controller.switchmanager.ISwitchManagerAware;
import org.opendaylight.controller.topologymanager.ITopologyManager;
import org.opendaylight.controller.topologymanager.ITopologyManagerAware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * switchmanager integrationtest stub Activator
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
    }

    /**
     * Function called when the activator stops just before the cleanup done by
     * ComponentActivatorAbstractBase
     * 
     */
    public void destroy() {
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
        Object[] res = { InventoryService.class };
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
        if (imp.equals(InventoryService.class)) {
            // export the service to be used by SAL
            Dictionary<String, Object> props = new Hashtable<String, Object>();
            // Set the protocolPluginType property which will be used
            // by SAL
            props.put("protocolPluginType", Node.NodeIDType.OPENFLOW);
            c.setInterface(IPluginInInventoryService.class.getName(), props);

            // Now lets add a service dependency to make sure the
            // provider of service exists
//            c.add(createServiceDependency()
//                    .setService(IController.class, "(name=Controller)")
//                    .setCallbacks("setController", "unsetController")
//                    .setRequired(true));
//            c.add(createContainerServiceDependency(containerName)
//                    .setService(IPluginOutInventoryService.class)
//                    .setCallbacks("setPluginOutInventoryServices",
//                            "unsetPluginOutInventoryServices")
//                    .setRequired(false));
        }
        
//  if (imp.equals(HostTracker.class)) {
//            // export the service
//            c.setInterface(new String[] { ISwitchManagerAware.class.getName(),
//                    IInventoryListener.class.getName(),
//                    ITopologyManagerAware.class.getName() }, null);

//            c.add(createContainerServiceDependency(containerName).setService(
//                    ISwitchManager.class).setCallbacks("setSwitchManager",
//                    "unsetSwitchManager").setRequired(false));
//            c.add(createContainerServiceDependency(containerName).setService(
//                    IClusterContainerServices.class).setCallbacks(
//                    "setClusterContainerService",
//                    "unsetClusterContainerService").setRequired(true));
//            c.add(createContainerServiceDependency(containerName).setService(
//                    IHostFinder.class).setCallbacks("setArpHandler",
//                    "unsetArpHandler").setRequired(false));
//            c.add(createContainerServiceDependency(containerName).setService(
//                    ITopologyManager.class).setCallbacks("setTopologyManager",
//                    "unsetTopologyManager").setRequired(false));
//            c.add(createContainerServiceDependency(containerName).setService(
//                    IfNewHostNotify.class).setCallbacks("setnewHostNotify",
//                    "unsetnewHostNotify").setRequired(false));
//        }
    }
}
