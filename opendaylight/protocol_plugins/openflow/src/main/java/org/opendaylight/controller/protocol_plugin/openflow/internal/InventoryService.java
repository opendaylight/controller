
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.protocol_plugin.openflow.internal;

import java.util.Collections;
import java.util.Date;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.felix.dm.Component;
import org.opendaylight.controller.protocol_plugin.openflow.IInventoryShimInternalListener;
import org.opendaylight.controller.protocol_plugin.openflow.IStatisticsListener;
import org.opendaylight.controller.protocol_plugin.openflow.core.IController;
import org.opendaylight.controller.protocol_plugin.openflow.core.ISwitch;
import org.opendaylight.controller.sal.core.Actions;
import org.opendaylight.controller.sal.core.Buffers;
import org.opendaylight.controller.sal.core.Capabilities;
import org.opendaylight.controller.sal.core.ConstructionException;
import org.opendaylight.controller.sal.core.Description;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.Node.NodeIDType;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.core.Property;
import org.opendaylight.controller.sal.core.Tables;
import org.opendaylight.controller.sal.core.TimeStamp;
import org.opendaylight.controller.sal.core.UpdateType;
import org.opendaylight.controller.sal.inventory.IPluginInInventoryService;
import org.opendaylight.controller.sal.inventory.IPluginOutInventoryService;
import org.opendaylight.controller.sal.utils.GlobalConstants;
import org.openflow.protocol.statistics.OFDescriptionStatistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The class describes inventory service protocol plugin. One instance per
 * container of the network. Each instance gets container specific inventory
 * events from InventoryServiceShim. It interacts with SAL to pass inventory
 * data to the upper application.
 *
 *
 */
public class InventoryService implements IInventoryShimInternalListener,
        IPluginInInventoryService, IStatisticsListener {
    protected static final Logger logger = LoggerFactory
            .getLogger(InventoryService.class);
    private Set<IPluginOutInventoryService> pluginOutInventoryServices = Collections
            .synchronizedSet(new HashSet<IPluginOutInventoryService>());
    private IController controller = null;
    private ConcurrentMap<Node, Map<String, Property>> nodeProps; // properties are maintained in default container only
    private ConcurrentMap<NodeConnector, Map<String, Property>> nodeConnectorProps; // properties are maintained in default container only
    private boolean isDefaultContainer = false;

    void setController(IController s) {
        this.controller = s;
    }

    void unsetController(IController s) {
        if (this.controller == s) {
            this.controller = null;
        }
    }

    /**
     * Function called by the dependency manager when all the required
     * dependencies are satisfied
     *
     */
    @SuppressWarnings("rawtypes")
    void init(Component c) {
        logger.trace("INIT called!");

        Dictionary props = c.getServiceProperties();
        if (props != null) {
            String containerName = (String) props.get("containerName");
            isDefaultContainer = containerName.equals(GlobalConstants.DEFAULT
                    .toString());
        }

        nodeProps = new ConcurrentHashMap<Node, Map<String, Property>>();
        nodeConnectorProps = new ConcurrentHashMap<NodeConnector, Map<String, Property>>();
    }

    /**
     * Function called by the dependency manager when at least one dependency
     * become unsatisfied or when the component is shutting down because for
     * example bundle is being stopped.
     *
     */
    void destroy() {
        logger.trace("DESTROY called!");
    }

    /**
     * Function called by dependency manager after "init ()" is called and after
     * the services provided by the class are registered in the service registry
     *
     */
    void start() {
        logger.trace("START called!");
    }

    /**
     * Function called by the dependency manager before the services exported by
     * the component are unregistered, this will be followed by a "destroy ()"
     * calls
     *
     */
    void stop() {
        logger.trace("STOP called!");
    }

    public void setPluginOutInventoryServices(IPluginOutInventoryService service) {
        logger.trace("Got a service set request {}", service);
        if (this.pluginOutInventoryServices != null) {
            this.pluginOutInventoryServices.add(service);
        }
    }

    public void unsetPluginOutInventoryServices(
            IPluginOutInventoryService service) {
        logger.trace("Got a service UNset request");
        if (this.pluginOutInventoryServices != null) {
            this.pluginOutInventoryServices.remove(service);
        }
    }

    protected Node OFSwitchToNode(ISwitch sw) {
        Node node = null;
        Object id = sw.getId();

        try {
            node = new Node(NodeIDType.OPENFLOW, id);
        } catch (ConstructionException e) {
            e.printStackTrace();
        }

        return node;
    }

    /**
     * Retrieve nodes from openflow
     */
    @Override
    public ConcurrentMap<Node, Map<String, Property>> getNodeProps() {
        if (nodeProps == null)
            return null;
        Map<Long, ISwitch> switches = controller.getSwitches();
        for (Map.Entry<Long, ISwitch> entry : switches.entrySet()) {
        	ISwitch sw = entry.getValue();
            Node node = OFSwitchToNode(sw);
            Map<String, Property> propMap = null;
            if (isDefaultContainer) {
                propMap = new HashMap<String, Property>();
                byte tables = sw.getTables();
                Tables t = new Tables(tables);
                if (t != null) {
                	propMap.put(Tables.TablesPropName,t);
                }
                int cap = sw.getCapabilities();
                Capabilities c = new Capabilities(cap);
                if (c != null) {
                	propMap.put(Capabilities.CapabilitiesPropName, c);
                }
                int act = sw.getActions();
                Actions a = new Actions(act);
                if (a != null) {
                	propMap.put(Actions.ActionsPropName,a);
                }
                int buffers = sw.getBuffers();
                Buffers b = new Buffers(buffers);
                if (b != null) {
                	propMap.put(Buffers.BuffersPropName,b);
                }
                Date connectedSince = sw.getConnectedDate();
                Long connectedSinceTime = (connectedSince == null) ? 0
                        : connectedSince.getTime();
                TimeStamp timeStamp = new TimeStamp(connectedSinceTime,
                        "connectedSince");
                propMap.put(TimeStamp.TimeStampPropName, timeStamp);
                nodeProps.put(node, propMap);
            }
        }
        return nodeProps;
    }

    @Override
    public ConcurrentMap<NodeConnector, Map<String, Property>> getNodeConnectorProps(
            Boolean refresh) {
        if (nodeConnectorProps == null)
            return null;

        if (isDefaultContainer && refresh) {
            Map<Long, ISwitch> switches = controller.getSwitches();
            for (ISwitch sw : switches.values()) {
                Map<NodeConnector, Set<Property>> ncProps = InventoryServiceHelper
                        .OFSwitchToProps(sw);
                for (Map.Entry<NodeConnector, Set<Property>> entry : ncProps
                        .entrySet()) {
                    updateNodeConnector(entry.getKey(), UpdateType.ADDED, entry
                            .getValue());
                }
            }
        }

        return nodeConnectorProps;
    }

    @Override
    public void updateNodeConnector(NodeConnector nodeConnector,
            UpdateType type, Set<Property> props) {
        logger.trace("NodeConnector id " + nodeConnector.getID()
                + " type " + nodeConnector.getType() + " "
                + type.getName() + " for Node id "
                + nodeConnector.getNode().getID());

        if (nodeConnectorProps == null) {
            return;
        }


        Map<String, Property> propMap = nodeConnectorProps
                .get(nodeConnector);
        switch (type) {
        case ADDED:
        case CHANGED:
            if (propMap == null)
                propMap = new HashMap<String, Property>();

            if (props != null) {
                for (Property prop : props) {
                    propMap.put(prop.getName(), prop);
                }
            }
            nodeConnectorProps.put(nodeConnector, propMap);
            break;
        case REMOVED:
            nodeConnectorProps.remove(nodeConnector);
            break;
        default:
            return;
        }

        // update sal and discovery
        synchronized (pluginOutInventoryServices) {
            for (IPluginOutInventoryService service : pluginOutInventoryServices) {
                service.updateNodeConnector(nodeConnector, type, props);
            }
        }
    }

    private void addNode(Node node, Set<Property> props) {
        logger.trace("{} added", node);
        if (nodeProps == null)
            return;

        // update local cache
        Map<String, Property> propMap = new HashMap<String, Property>();
        for (Property prop : props) {
            propMap.put(prop.getName(), prop);
        }
        nodeProps.put(node, propMap);

        // update sal
        synchronized (pluginOutInventoryServices) {
            for (IPluginOutInventoryService service : pluginOutInventoryServices) {
                service.updateNode(node, UpdateType.ADDED, props);
            }
        }
    }

    private void removeNode(Node node) {
        logger.trace("{} removed", node);
        if (nodeProps == null)
            return;

        // update local cache
        nodeProps.remove(node);

        Set<NodeConnector> removeSet = new HashSet<NodeConnector>();
        for (NodeConnector nodeConnector : nodeConnectorProps.keySet()) {
            if (nodeConnector.getNode().equals(node)) {
                removeSet.add(nodeConnector);
            }
        }
        for (NodeConnector nodeConnector : removeSet) {
            nodeConnectorProps.remove(nodeConnector);
        }

        // update sal
        synchronized (pluginOutInventoryServices) {
            for (IPluginOutInventoryService service : pluginOutInventoryServices) {
                service.updateNode(node, UpdateType.REMOVED, null);
            }
        }
    }


    private void updateSwitchProperty(Long switchId, Set<Property> propSet) {
        // update local cache
        Node node = OFSwitchToNode(controller.getSwitch(switchId));
        Map<String, Property> propMap = nodeProps.get(node);
        if (propMap == null) {
            propMap = new HashMap<String, Property>();
        }
        
        boolean change = false;
        for (Property prop : propSet) {
        	String propertyName = prop.getName();
        	Property currentProp = propMap.get(propertyName);
        	if (!prop.equals(currentProp)) {
        		change = true;
        		propMap.put(propertyName, prop);
        	}
        }
        nodeProps.put(node, propMap);

        // Update sal if any of the properties has changed
        if (change) {
	        synchronized (pluginOutInventoryServices) {
	            for (IPluginOutInventoryService service : pluginOutInventoryServices) {
	                service.updateNode(node, UpdateType.CHANGED, propSet);
	            }
	        }
        }
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
	public void descriptionRefreshed(Long switchId,
			OFDescriptionStatistics descriptionStats) {
		
		Set<Property> propSet = new HashSet<Property>(1);
  		Description desc = 
				new Description(descriptionStats.getDatapathDescription());
       propSet.add(desc);
       this.updateSwitchProperty(switchId, propSet);
	}
}
