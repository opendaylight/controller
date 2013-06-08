
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.protocol_plugin.openflow.internal;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.opendaylight.controller.sal.core.Bandwidth;
import org.opendaylight.controller.sal.core.AdvertisedBandwidth;
import org.opendaylight.controller.sal.core.SupportedBandwidth;
import org.opendaylight.controller.sal.core.PeerBandwidth;
import org.opendaylight.controller.sal.core.Config;
import org.opendaylight.controller.sal.core.Name;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.core.Property;
import org.opendaylight.controller.sal.core.State;

import org.opendaylight.controller.sal.utils.NodeCreator;

import org.opendaylight.controller.protocol_plugin.openflow.core.ISwitch;
import org.openflow.protocol.OFPhysicalPort;
import org.openflow.protocol.OFPhysicalPort.OFPortConfig;
import org.openflow.protocol.OFPhysicalPort.OFPortFeatures;
import org.openflow.protocol.OFPhysicalPort.OFPortState;

/**
 * The class provides helper functions to retrieve inventory properties from
 * OpenFlow messages
 */
public class InventoryServiceHelper {
    /*
     * Returns BandWidth property from OpenFlow OFPhysicalPort features
     */
    public static Bandwidth OFPortToBandWidth(int portFeatures) {
        Bandwidth bw = null;
        int value = portFeatures
                & (OFPortFeatures.OFPPF_10MB_FD.getValue()
                        | OFPortFeatures.OFPPF_10MB_HD.getValue()
                        | OFPortFeatures.OFPPF_100MB_FD.getValue()
                        | OFPortFeatures.OFPPF_100MB_HD.getValue()
                        | OFPortFeatures.OFPPF_1GB_FD.getValue()
                        | OFPortFeatures.OFPPF_1GB_HD.getValue() | OFPortFeatures.OFPPF_10GB_FD
                        .getValue());

        switch (value) {
        case 1:
        case 2:
            bw = new Bandwidth(Bandwidth.BW10Mbps);
            break;
        case 4:
        case 8:
            bw = new Bandwidth(Bandwidth.BW100Mbps);
            break;
        case 16:
        case 32:
            bw = new Bandwidth(Bandwidth.BW1Gbps);
            break;
        case 64:
            bw = new Bandwidth(Bandwidth.BW10Gbps);
            break;
        default:
            break;
        }
        return bw;
    }

    /*
     * Returns Config property from OpenFLow OFPhysicalPort config
     */
    public static Config OFPortToConfig(int portConfig) {
        Config config;
        if ((OFPortConfig.OFPPC_PORT_DOWN.getValue() & portConfig) != 0)
            config = new Config(Config.ADMIN_DOWN);
        else
            config = new Config(Config.ADMIN_UP);
        return config;
    }

    /*
     * Returns State property from OpenFLow OFPhysicalPort state
     */
    public static State OFPortToState(int portState) {
        State state;
        if ((OFPortState.OFPPS_LINK_DOWN.getValue() & portState) != 0)
            state = new State(State.EDGE_DOWN);
        else
            state = new State(State.EDGE_UP);
        return state;
    }

    /*
     * Returns set of properties from OpenFLow OFPhysicalPort
     */
    public static Set<Property> OFPortToProps(OFPhysicalPort port) {
        Set<Property> props = new HashSet<Property>();
        Bandwidth bw = InventoryServiceHelper.OFPortToBandWidth(port
                .getCurrentFeatures());
        if (bw != null) {
            props.add(bw);
        }
        
        Bandwidth abw = InventoryServiceHelper.OFPortToBandWidth(port.getAdvertisedFeatures());
        if (abw != null) {
        	AdvertisedBandwidth a = new AdvertisedBandwidth(abw.getValue());
        	if (a != null) {
        		props.add(a);
        	}
        }
        Bandwidth sbw = InventoryServiceHelper.OFPortToBandWidth(port.getSupportedFeatures());
        if (sbw != null) {
        	SupportedBandwidth s = new SupportedBandwidth(sbw.getValue());
        	if (s != null) {
        		props.add(s);
        	}
        }
        Bandwidth pbw = InventoryServiceHelper.OFPortToBandWidth(port.getPeerFeatures());
        if (pbw != null) {
        	PeerBandwidth p = new PeerBandwidth(pbw.getValue());
        	if (p != null) {
        		props.add(p);
        	}
        }
        props.add(new Name(port.getName()));
        props.add(InventoryServiceHelper.OFPortToConfig(port.getConfig()));
        props.add(InventoryServiceHelper.OFPortToState(port.getState()));
        return props;
    }

    /*
     * Returns set of properties for each nodeConnector in an OpenFLow switch
     */
    public static Map<NodeConnector, Set<Property>> OFSwitchToProps(ISwitch sw) {
        Map<NodeConnector, Set<Property>> ncProps = new HashMap<NodeConnector, Set<Property>>();

        if (sw == null) {
            return ncProps;
        }

        Node node = NodeCreator.createOFNode(sw.getId());
        if (node == null) {
            return ncProps;
        }

        Set<Property> props;
        NodeConnector nodeConnector;
        OFPhysicalPort port;
        Map<Short, OFPhysicalPort> ports = sw.getPhysicalPorts();
        for (Map.Entry<Short, OFPhysicalPort> entry : ports.entrySet()) {
            nodeConnector = PortConverter.toNodeConnector(entry.getKey(), node);
            port = entry.getValue();
            props = InventoryServiceHelper.OFPortToProps(port);
            ncProps.put(nodeConnector, props);
        }

        return ncProps;
    }
}
