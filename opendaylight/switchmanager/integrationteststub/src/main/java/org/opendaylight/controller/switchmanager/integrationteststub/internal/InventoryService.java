package org.opendaylight.controller.switchmanager.integrationteststub.internal;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.felix.dm.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.opendaylight.controller.sal.core.Actions;
import org.opendaylight.controller.sal.core.Bandwidth;
import org.opendaylight.controller.sal.core.Buffers;
import org.opendaylight.controller.sal.core.Capabilities;
import org.opendaylight.controller.sal.core.Capabilities.CapabilitiesType;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.core.Property;
import org.opendaylight.controller.sal.core.State;
import org.opendaylight.controller.sal.core.Tables;
import org.opendaylight.controller.sal.core.TimeStamp;
import org.opendaylight.controller.sal.inventory.IPluginInInventoryService;
import org.opendaylight.controller.sal.utils.NodeCreator;
import org.opendaylight.controller.sal.utils.NodeConnectorCreator;

/**
 * Stub Implementation for IPluginInReadService used by SAL
 * 
 * 
 */
public class InventoryService implements IPluginInInventoryService {
    private static final Logger logger = LoggerFactory
            .getLogger(InventoryService.class);

    private ConcurrentMap<Node, Map<String, Property>> nodeProps; // properties are maintained in global container only
    private ConcurrentMap<NodeConnector, Map<String, Property>> nodeConnectorProps; // properties are maintained in global container only

    
    /**
     * Function called by the dependency manager when all the required
     * dependencies are satisfied
     * 
     */
    void init() {
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
    }

    /**
     * Function called by dependency manager after "init ()" is called and after
     * the services provided by the class are registered in the service registry
     * 
     */
    void start() {
    }

    /**
     * Function called by the dependency manager before the services exported by
     * the component are unregistered, this will be followed by a "destroy ()"
     * calls
     * 
     */
    void stop() {
    }

    /**
     * Retrieve nodes from openflow
     */
    @Override
    public ConcurrentMap<Node, Map<String, Property>> getNodeProps() {
        
        //  setup nodeProps
        Map<String, Property> propMap = new HashMap<String, Property>();

        Tables t = new Tables((byte)1);
        propMap.put(Tables.TablesPropName, t);
        Capabilities c = new Capabilities((int)3);
        propMap.put(Capabilities.CapabilitiesPropName, c);
        Actions a = new Actions((int)2);
        propMap.put(Actions.ActionsPropName, a);
        Buffers b = new Buffers((int)1);
        propMap.put(Buffers.BuffersPropName, b);
        Long connectedSinceTime = 100000L;
        TimeStamp timeStamp = new TimeStamp(connectedSinceTime,
                "connectedSince");
        propMap.put(TimeStamp.TimeStampPropName, timeStamp);
        
        // setup property map for all nodes
        Node node;
        node = NodeCreator.createOFNode(2L);
        nodeProps.put(node, propMap);
                
        return nodeProps;
    }

    /**
     * Retrieve nodeConnectors from openflow
     */
    @Override
    public ConcurrentMap<NodeConnector, Map<String, Property>> getNodeConnectorProps(
            Boolean refresh) {

        //  setup nodeConnectorProps
        Map<String, Property> ncPropMap = new HashMap<String, Property>();
        Capabilities cap = new Capabilities 
        		(CapabilitiesType.FLOW_STATS_CAPABILITY.getValue());
        ncPropMap.put(Capabilities.CapabilitiesPropName, cap);
        Bandwidth bw = new Bandwidth (Bandwidth.BW1Gbps);
        ncPropMap.put(Bandwidth.BandwidthPropName, bw);
        State st = new State (State.EDGE_UP);
        ncPropMap.put(State.StatePropName, st);
        
        // setup property map for all node connectors
        NodeConnector nc = NodeConnectorCreator.createOFNodeConnector
                ((short)2, NodeCreator.createOFNode(3L));
        nodeConnectorProps.put(nc, ncPropMap);
       
        return nodeConnectorProps;
    }


}
