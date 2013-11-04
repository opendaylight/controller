package org.opendaylight.controller.sal.compability;

import org.opendaylight.controller.sal.core.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.port.rev130925.PortFeatures;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;

public class ToSalPropertyClassUtils {
    public static Bandwidth salAdvertisedBandwidthFrom(NodeConnector nodeConnector) {
        FlowCapableNodeConnector flowCapNodeConn = nodeConnector.getAugmentation(FlowCapableNodeConnector.class);        
        PortFeatures portFeatures = flowCapNodeConn.getAdvertisedFeatures();
        return new AdvertisedBandwidth(resolveBandwidth(portFeatures));
    }

    public static Bandwidth salPeerBandwidthFrom(NodeConnector nodeConnector) {
        FlowCapableNodeConnector flowCapNodeConn = nodeConnector.getAugmentation(FlowCapableNodeConnector.class);        
        PortFeatures portFeatures = flowCapNodeConn.getPeerFeatures();
        return new PeerBandwidth(resolveBandwidth(portFeatures));
    }

    public static Bandwidth salSupportedBandwidthFrom(NodeConnector nodeConnector) {
        FlowCapableNodeConnector flowCapNodeConn = nodeConnector.getAugmentation(FlowCapableNodeConnector.class);        
        PortFeatures portFeatures = flowCapNodeConn.getSupported();
        return new SupportedBandwidth(resolveBandwidth(portFeatures));
    }

    public static MacAddress salMacAddressFrom(NodeConnector nodeConnector) {
        FlowCapableNodeConnector flowCapNodeConn = nodeConnector.getAugmentation(FlowCapableNodeConnector.class);        
        String hwAddress = flowCapNodeConn.getHardwareAddress().getValue();
        return new MacAddress(bytesFrom(hwAddress));        
    }
    
    
    public static Name salNameFrom(NodeConnector nodeConnector) {
        FlowCapableNodeConnector flowCapNodeConn = nodeConnector.getAugmentation(FlowCapableNodeConnector.class);        
        return new Name(flowCapNodeConn.getName());
    }
    
    

    private static byte[] bytesFrom(String hwAddress) {
        String[] mac = hwAddress.split(":");
        byte[] macAddress = new byte[6]; // mac.length == 6 bytes
        for (int i = 0; i < mac.length; i++) {
            macAddress[i] = Integer.decode("0x" + mac[i]).byteValue();
        }
        return macAddress;
    }

    private static long resolveBandwidth(PortFeatures portFeatures) {
        if (portFeatures.is_1tbFd()) {
            return Bandwidth.BW1Tbps;
        } else if (portFeatures.is_100gbFd()) {
            return Bandwidth.BW100Gbps;
        } else if (portFeatures.is_40gbFd()) {
            return Bandwidth.BW40Gbps;
        } else if (portFeatures.is_10gbFd()) {
            return Bandwidth.BW10Gbps;
        } else if (portFeatures.is_1gbHd() || portFeatures.is_1gbFd()) {
            return Bandwidth.BW1Gbps;
        } else if (portFeatures.is_100mbHd() || portFeatures.is_100mbFd()) {
            return Bandwidth.BW100Mbps;
        } else if (portFeatures.is_10mbHd() || portFeatures.is_10mbFd()) {
            return Bandwidth.BW10Mbps;
        } else {
            return Bandwidth.BWUNK;
        }
    }

}
