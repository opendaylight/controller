/*
 * Copyright IBM Corporation, 2013.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.samples.loadbalancer;

import org.opendaylight.controller.sal.packet.IPv4;
import org.opendaylight.controller.sal.packet.Packet;
import org.opendaylight.controller.sal.packet.TCP;
import org.opendaylight.controller.sal.packet.UDP;
import org.opendaylight.controller.sal.utils.IPProtocols;
import org.opendaylight.controller.sal.utils.NetUtils;
import org.opendaylight.controller.samples.loadbalancer.entities.Client;
import org.opendaylight.controller.samples.loadbalancer.entities.VIP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class defines utilty methods that will be used by different components 
 * of the load balancer service 
 *
 */
public class LBUtil {
    
    private static final Logger lbuLogger = LoggerFactory.getLogger(LBUtil.class);
    
    public LBUtil(){}
    
    /**
     * Extract the details of the source machine that sent this packet 'inPkt'  
     * @param inPkt	Packet that is received by the controller
     * @return	Details of the source machine in Client object.
     */
    public Client getClientFromPacket(IPv4 inPkt){
        lbuLogger.info("Find client information from packet : {}",inPkt.toString());
        
        String ip = NetUtils.getInetAddress(inPkt.getSourceAddress()).getHostAddress();
        
        String protocol = IPProtocols.getProtocolName(inPkt.getProtocol());
        
        lbuLogger.info("client ip {} and protocl {}",ip,protocol);
        
        Packet tpFrame= inPkt.getPayload();
        
        lbuLogger.info("Get protocol layer {}",tpFrame.toString());
        
        short port = 0;
        
        if(protocol.equals(IPProtocols.TCP.toString())){
            TCP tcpFrame = (TCP)tpFrame;
            port = tcpFrame.getSourcePort();
        }else{
            UDP udpFrame = (UDP)tpFrame;
            port = udpFrame.getSourcePort();
        }
        
        lbuLogger.info("Found port {}",port);
        
        Client source = new Client(ip, protocol,port);
        
        lbuLogger.info("Client information : {}",source.toString());
        
        return source;
    }
    
    /**
     * Extract the details of the destination machine where this packet 'inPkt' need
     * to be delivered
     * @param inPkt Packet that is received by controller for forwarding
     * @return	Details of the destination machine packet in VIP
     */
    public VIP getVIPFromPacket(IPv4 inPkt){
        
        lbuLogger.info("Find VIP information from packet : {}",inPkt.toString());
        
        String ip = NetUtils.getInetAddress(inPkt.getDestinationAddress()).getHostAddress();
        
        String protocol = IPProtocols.getProtocolName(inPkt.getProtocol());
        
        Packet tpFrame= inPkt.getPayload();
        
        short port = 0;
        
        if(protocol.equals(IPProtocols.TCP.toString())){
            TCP tcpFrame = (TCP)tpFrame;
            port = tcpFrame.getDestinationPort();
        }else{
            
            UDP udpFrame = (UDP)tpFrame;
            port = udpFrame.getDestinationPort();
        }
        
        VIP dest = new VIP(null,ip, protocol,port,null);
        
        lbuLogger.info("VIP information : {}",dest.toString());
        
        return dest;
    }
}