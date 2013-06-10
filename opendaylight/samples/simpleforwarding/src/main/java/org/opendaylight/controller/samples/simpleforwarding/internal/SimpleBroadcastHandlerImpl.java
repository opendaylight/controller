package org.opendaylight.controller.samples.simpleforwarding.internal;

import java.util.HashSet;
import java.util.Set;

import org.opendaylight.controller.sal.packet.Ethernet;
import org.opendaylight.controller.sal.packet.IDataPacketService;
import org.opendaylight.controller.sal.packet.IListenDataPacket;
import org.opendaylight.controller.sal.core.ConstructionException;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.inventory.IInventoryService;
import org.opendaylight.controller.sal.packet.Packet;
import org.opendaylight.controller.sal.packet.PacketResult;
import org.opendaylight.controller.sal.packet.RawPacket;
import org.opendaylight.controller.sal.utils.EtherTypes;
import org.opendaylight.controller.samples.simpleforwarding.IBroadcastHandler;
import org.opendaylight.controller.samples.simpleforwarding.IBroadcastPortSelector;
import org.opendaylight.controller.topologymanager.ITopologyManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The simple broadcast handler simply sends broadcast packets out all ports
 * that are not known to belong to an internal, i.e., switch-switch, link. Note
 * that this is *not* safe in the general case when an OpenDaylight-controlled
 * network has L2 peering with normal a normal L2 network. It is entirely
 * possible for a packet to be flooded to a legacy/non-controlled switch and
 * then be reflected back into the OpenDaylight-controlled region resulting in a
 * loop.
 *
 * @author Colin Dixon <ckd@us.ibm.com>
 *
 */
public class SimpleBroadcastHandlerImpl implements IBroadcastHandler, IListenDataPacket {

    private static Logger log = LoggerFactory.getLogger(SimpleBroadcastHandlerImpl.class);
    protected IDataPacketService dataPacketService = null;
    protected ITopologyManager topoManager = null;
    protected IInventoryService invSvc = null;
    protected IBroadcastPortSelector bcastPorts = null;

    Mode mode = Mode.DISABLED;

    @Override
    public PacketResult receiveDataPacket(RawPacket inPkt) {
        if (mode == Mode.DISABLED) {
            return PacketResult.IGNORED;
        }

        Packet decodedPkt = this.dataPacketService.decodeDataPacket(inPkt);
        if (decodedPkt instanceof Ethernet) {
            Ethernet eth = (Ethernet) decodedPkt;

            // ignore LLDP
            if (eth.getEtherType() != EtherTypes.LLDP.shortValue()) {

                if (eth.isBroadcast()) {
                    broadcastPacket(inPkt);
                } else if (eth.isMulticast()) {
                    // TODO: for now just treat multicast as broadcast
                    broadcastPacket(inPkt);
                }
            }
        }

        return PacketResult.KEEP_PROCESSING;
    }

    @Override
    public boolean broadcastPacket(RawPacket pkt) {
        Set<NodeConnector> toPacketOut = new HashSet<NodeConnector>();

        // make sure that topoManager/datPacketService aren't pulled out from under us
        synchronized (this) {
            if (topoManager == null) {
                return false;
            }

            if (dataPacketService == null) {
                return false;
            }

            if (invSvc == null) {
                return false;
            }

            // find all non-internal NodeConnectors
            switch(mode){
                case DISABLED:
                    // intentionally blank; don't send the packet anywhere
                    break;

                case BROADCAST_TO_HOSTS:
                    toPacketOut.addAll(topoManager.getNodeConnectorWithHost());
                    break;

                case BROADCAST_TO_NONINTERNAL:
                    for (NodeConnector nc : invSvc.getNodeConnectorProps().keySet()) {
                        if (!topoManager.isInternal(nc) && !nc.getType().equals("SW")) {
                            toPacketOut.add(nc);
                        }
                    }
                    break;

                case EXTERNAL_QUERY:
                    if(bcastPorts != null){
                        toPacketOut.addAll(bcastPorts.getBroadcastPorts());
                    }else{
                        log.error("Mode set to "
                                  + Mode.EXTERNAL_QUERY
                                  + ", but no external source of broadcast ports was provided.");
                        return false;
                    }
                    break;

                default:
                    log.error("Mode " + mode + " is not supported.");
                    break;
            }

            // remove the NodeConnector it came in on
            toPacketOut.remove(pkt.getIncomingNodeConnector());

            // send it out all the node connectors
            for(NodeConnector nc : toPacketOut){
                try {
                    RawPacket toSend = new RawPacket(pkt);
                    toSend.setOutgoingNodeConnector(nc);
                    dataPacketService.transmitDataPacket(toSend);
                } catch (ConstructionException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }

        return true;
    }

    void setDataPacketService(IDataPacketService s) {
        // make sure dataPacketService doesn't change while we're in the middle of stuff
        synchronized (this) {
            this.dataPacketService = s;
        }
    }

    void unsetDataPacketService(IDataPacketService s) {
        // make sure dataPacketService doesn't change while we're in the middle of stuff
        synchronized (this) {
            if (this.dataPacketService == s) {
                this.dataPacketService = null;
            }
        }
    }

    void setTopologyManager(ITopologyManager t) {
        // make sure topoManager doesn't change while we're in the middle of stuff
        synchronized (this) {
            this.topoManager = t;
        }
    }

    void unsetTopologyManager(ITopologyManager t) {
        // make sure topoManager doesn't change while we're in the middle of stuff
        synchronized (this) {
            if (this.topoManager == t) {
                this.topoManager = null;
            }
        }
    }

    void setInventoryService(IInventoryService i) {
        synchronized (this) {
            this.invSvc = i;
        }
    }

    void unsetInventoryService(IInventoryService i) {
        synchronized (this) {
            if (this.invSvc == i) {
                this.invSvc = null;
            }
        }
    }

    void setBroadcastPortSelector(IBroadcastPortSelector bps) {
        synchronized (this) {
            bcastPorts = bps;
        }
    }

    void unsetBroadcastPortSelector(IBroadcastPortSelector bps) {
        synchronized (this) {
            if( bcastPorts == bps ) {
                this.bcastPorts = null;
            }
        }
    }

    public void setMode(Mode m) {
        synchronized (this) {
            mode = m;
        }
    }

}
