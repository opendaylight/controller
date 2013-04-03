
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.protocol_plugin.openflow.internal;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.opendaylight.controller.protocol_plugin.openflow.IDataPacketListen;
import org.opendaylight.controller.protocol_plugin.openflow.IDataPacketMux;
import org.opendaylight.controller.protocol_plugin.openflow.IInventoryShimExternalListener;
import org.opendaylight.controller.protocol_plugin.openflow.core.IController;
import org.opendaylight.controller.protocol_plugin.openflow.core.IMessageListener;
import org.opendaylight.controller.protocol_plugin.openflow.core.ISwitch;
import org.openflow.protocol.OFMessage;
import org.openflow.protocol.OFPacketIn;
import org.openflow.protocol.OFPacketOut;
import org.openflow.protocol.OFPort;
import org.openflow.protocol.OFType;
import org.openflow.protocol.action.OFAction;
import org.openflow.protocol.action.OFActionOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.opendaylight.controller.sal.core.ConstructionException;
import org.opendaylight.controller.sal.core.ContainerFlow;
import org.opendaylight.controller.sal.core.IContainerListener;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.core.Property;
import org.opendaylight.controller.sal.core.UpdateType;
import org.opendaylight.controller.sal.packet.IPluginOutDataPacketService;
import org.opendaylight.controller.sal.packet.PacketResult;
import org.opendaylight.controller.sal.packet.RawPacket;
import org.opendaylight.controller.sal.utils.GlobalConstants;

public class DataPacketMuxDemux implements IContainerListener,
        IMessageListener, IDataPacketMux, IInventoryShimExternalListener {
    protected static final Logger logger = LoggerFactory
            .getLogger(DataPacketMuxDemux.class);
    private IController controller = null;
    private ConcurrentMap<Long, ISwitch> swID2ISwitch = new ConcurrentHashMap<Long, ISwitch>();
    // Gives a map between a Container and all the DataPacket listeners on SAL
    private ConcurrentMap<String, IPluginOutDataPacketService> pluginOutDataPacketServices = new ConcurrentHashMap<String, IPluginOutDataPacketService>();
    // Gives a map between a NodeConnector and the containers to which it belongs
    private ConcurrentMap<NodeConnector, List<String>> nc2Container = new ConcurrentHashMap<NodeConnector, List<String>>();
    // Gives a map between a Container and the FlowSpecs on it
    private ConcurrentMap<String, List<ContainerFlow>> container2FlowSpecs = new ConcurrentHashMap<String, List<ContainerFlow>>();
    // Track local data packet listener
    private List<IDataPacketListen> iDataPacketListen = new CopyOnWriteArrayList<IDataPacketListen>();

    void setIDataPacketListen(IDataPacketListen s) {
        if (this.iDataPacketListen != null) {
            if (!this.iDataPacketListen.contains(s)) {
                logger.debug("Added new IDataPacketListen");
                this.iDataPacketListen.add(s);
            }
        }
    }

    void unsetIDataPacketListen(IDataPacketListen s) {
        if (this.iDataPacketListen != null) {
            if (this.iDataPacketListen.contains(s)) {
                logger.debug("Removed IDataPacketListen");
                this.iDataPacketListen.remove(s);
            }
        }
    }

    void setPluginOutDataPacketService(Map<String, Object> props,
            IPluginOutDataPacketService s) {
        if (props == null) {
            logger.error("Didn't receive the service properties");
            return;
        }
        String containerName = (String) props.get("containerName");
        if (containerName == null) {
            logger.error("containerName not supplied");
            return;
        }
        if (this.pluginOutDataPacketServices != null) {
            // It's expected only one SAL per container as long as the
            // replication is done in the SAL implementation toward
            // the different APPS
            this.pluginOutDataPacketServices.put(containerName, s);
            logger.debug("New outService for container: {}", containerName);
        }
    }

    void unsetPluginOutDataPacketService(Map<String, Object> props,
            IPluginOutDataPacketService s) {
        if (props == null) {
            logger.error("Didn't receive the service properties");
            return;
        }
        String containerName = (String) props.get("containerName");
        if (containerName == null) {
            logger.error("containerName not supplied");
            return;
        }
        if (this.pluginOutDataPacketServices != null) {
            this.pluginOutDataPacketServices.remove(containerName);
            logger.debug("Removed outService for container: {}", containerName);
        }
    }

    void setController(IController s) {
        logger.debug("Controller provider set in DATAPACKET SERVICES");
        this.controller = s;
    }

    void unsetController(IController s) {
        if (this.controller == s) {
            logger.debug("Controller provider UNset in DATAPACKET SERVICES");
            this.controller = null;
        }
    }

    /**
     * Function called by the dependency manager when all the required
     * dependencies are satisfied
     *
     */
    void init() {
        this.controller.addMessageListener(OFType.PACKET_IN, this);
    }

    /**
     * Function called by the dependency manager when at least one
     * dependency become unsatisfied or when the component is shutting
     * down because for example bundle is being stopped.
     *
     */
    void destroy() {
        this.controller.removeMessageListener(OFType.PACKET_IN, this);

        // Clear state that may need to be reused on component restart
        this.pluginOutDataPacketServices.clear();
        this.nc2Container.clear();
        this.container2FlowSpecs.clear();
        this.controller = null;
        this.swID2ISwitch.clear();
    }

    @Override
    public void receive(ISwitch sw, OFMessage msg) {
        if (sw == null || msg == null
                || this.pluginOutDataPacketServices == null) {
            // Something fishy, we cannot do anything
        	logger.debug("sw: {} and/or msg: {} and/or pluginOutDataPacketServices: {} is null!",
        				new Object[]{sw, msg, this.pluginOutDataPacketServices});
            return;
        }
        if (msg instanceof OFPacketIn) {
            OFPacketIn ofPacket = (OFPacketIn) msg;
            Long ofSwitchID = Long.valueOf(sw.getId());
            Short ofPortID = Short.valueOf(ofPacket.getInPort());

            try {
                Node n = new Node(Node.NodeIDType.OPENFLOW, ofSwitchID);
                NodeConnector p = PortConverter.toNodeConnector(ofPortID, n);
                RawPacket dataPacket = new RawPacket(ofPacket.getPacketData());
                dataPacket.setIncomingNodeConnector(p);

                // Try to dispatch the packet locally, in here we will
                // pass the parsed packet simply because once the
                // packet infra is settled all the packets will passed
                // around as parsed and read-only
                for (int i = 0; i < this.iDataPacketListen.size(); i++) {
                    IDataPacketListen s = this.iDataPacketListen.get(i);
                    if (s.receiveDataPacket(dataPacket).equals(
                            PacketResult.CONSUME)) {
                        logger.trace("Consumed locally data packet");
                        return;
                    }
                }

                // Now dispatch the packet toward SAL at least for
                // default container, we need to revisit this in a general
                // slicing architecture refresh
                IPluginOutDataPacketService defaultOutService = this.pluginOutDataPacketServices
                        .get(GlobalConstants.DEFAULT.toString());
                if (defaultOutService != null) {
                    defaultOutService.receiveDataPacket(dataPacket);
                    logger.trace("Dispatched to apps a frame of size: {} on container: {}",
                            ofPacket.getPacketData().length, GlobalConstants.DEFAULT.toString());
                }
                // Now check the mapping between nodeConnector and
                // Container and later on optinally filter based on
                // flowSpec
                List<String> containersRX = this.nc2Container.get(p);
                if (containersRX != null) {
                    for (int i = 0; i < containersRX.size(); i++) {
                        String container = containersRX.get(i);
                        IPluginOutDataPacketService s = this.pluginOutDataPacketServices
                                .get(container);
                        if (s != null) {
                            // TODO add filtering on a per-flowSpec base
                            s.receiveDataPacket(dataPacket);
                            logger.trace("Dispatched to apps a frame of size: {} on container: {}",
                                    ofPacket.getPacketData().length, GlobalConstants.DEFAULT.toString());

                        }
                    }
                }

                // This is supposed to be the catch all for all the
                // DataPacket hence we will assume it has been handled
                return;
            } catch (ConstructionException cex) {
            }

            // If we reach this point something went wrong.
            return;
        } else {
            // We don't care about non-data packets
            return;
        }
    }

    @Override
    public void transmitDataPacket(RawPacket outPkt) {
        // Sanity check area
        if (outPkt == null) {
        	logger.debug("outPkt is null!");
            return;
        }

        NodeConnector outPort = outPkt.getOutgoingNodeConnector();
        if (outPort == null) {
        	logger.debug("outPort is null! outPkt: {}", outPkt);
            return;
        }

        if (!outPort.getType().equals(
                NodeConnector.NodeConnectorIDType.OPENFLOW)) {
            // The output Port is not of type OpenFlow
        	logger.debug("outPort is not OF Type! outPort: {}", outPort);
            return;
        }

        Short port = (Short) outPort.getID();
        Long swID = (Long) outPort.getNode().getID();
        ISwitch sw = this.swID2ISwitch.get(swID);

        if (sw == null) {
            // If we cannot get the controller descriptor we cannot even
            // send out the frame
        	logger.debug("swID: {} - sw is null!", swID);
            return;
        }

        byte[] data = outPkt.getPacketData();
        // build action
        OFActionOutput action = new OFActionOutput().setPort(port);
        // build packet out
        OFPacketOut po = new OFPacketOut().setBufferId(
                OFPacketOut.BUFFER_ID_NONE).setInPort(OFPort.OFPP_NONE)
                .setActions(Collections.singletonList((OFAction) action))
                .setActionsLength((short) OFActionOutput.MINIMUM_LENGTH);

        po.setLengthU(OFPacketOut.MINIMUM_LENGTH + po.getActionsLength()
                + data.length);
        po.setPacketData(data);

        sw.asyncSend(po);
        logger.trace("Transmitted a frame of size: {}", data.length);
    }

    public void addNode(Node node, Set<Property> props) {
        if (node == null) {
        	logger.debug("node is null!");
            return;
        } 

        long sid = (Long) node.getID();
        ISwitch sw = controller.getSwitches().get(sid);
        if (sw == null) {
        	logger.debug("sid: {} - sw is null!", sid);
        	return;
        }
        this.swID2ISwitch.put(sw.getId(), sw);
    }

    public void removeNode(Node node) {
        if (node == null) {
        	logger.debug("node is null!");
            return;
        }

        long sid = (Long) node.getID();
        ISwitch sw = controller.getSwitches().get(sid);
        if (sw == null) {
        	logger.debug("sid: {} - sw is null!", sid);
        	return;
        }
        this.swID2ISwitch.remove(sw.getId());
    }

    @Override
    public void tagUpdated(String containerName, Node n, short oldTag,
            short newTag, UpdateType t) {
        // Do nothing
    }

    @Override
    public void containerFlowUpdated(String containerName,
            ContainerFlow previousFlow, ContainerFlow currentFlow, UpdateType t) {
        if (this.container2FlowSpecs == null) {
            logger.error("container2FlowSpecs is NULL");
            return;
        }
        List<ContainerFlow> fSpecs = this.container2FlowSpecs
                .get(containerName);
        if (fSpecs == null) {
            fSpecs = new CopyOnWriteArrayList<ContainerFlow>();
        }
        switch (t) {
        case ADDED:
            if (!fSpecs.contains(previousFlow)) {
                fSpecs.add(previousFlow);
            }
            break;
        case REMOVED:
            if (fSpecs.contains(previousFlow)) {
                fSpecs.remove(previousFlow);
            }
            break;
        case CHANGED:
            break;
        }
    }

    @Override
    public void nodeConnectorUpdated(String containerName, NodeConnector p,
            UpdateType t) {
        if (this.nc2Container == null) {
            logger.error("nc2Container is NULL");
            return;
        }
        List<String> containers = this.nc2Container.get(p);
        if (containers == null) {
            containers = new CopyOnWriteArrayList<String>();
        }
        boolean updateMap = false;
        switch (t) {
        case ADDED:
            if (!containers.contains(containerName)) {
                containers.add(containerName);
                updateMap = true;
            }
            break;
        case REMOVED:
            if (containers.contains(containerName)) {
                containers.remove(containerName);
                updateMap = true;
            }
            break;
        case CHANGED:
            break;
        }
        if (updateMap) {
            if (containers.isEmpty()) {
                // Do cleanup to reduce memory footprint if no
                // elements to be tracked
                this.nc2Container.remove(p);
            } else {
                this.nc2Container.put(p, containers);
            }
        }
    }

    @Override
    public void containerModeUpdated(UpdateType t) {
        // do nothing
    }

    @Override
    public void updateNode(Node node, UpdateType type, Set<Property> props) {
        switch (type) {
        case ADDED:
            addNode(node, props);
            break;
        case REMOVED:
            removeNode(node);
            break;
        default:
            break;
        }
    }

    @Override
    public void updateNodeConnector(NodeConnector nodeConnector,
            UpdateType type, Set<Property> props) {
        // do nothing
    }
}
