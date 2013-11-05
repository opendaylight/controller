/*
 * Copyright (c) 2013 IBM and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.samples.simpleforwarding.internal;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.opendaylight.controller.sal.packet.Ethernet;
import org.opendaylight.controller.sal.packet.IDataPacketService;
import org.opendaylight.controller.sal.packet.IListenDataPacket;
import org.opendaylight.controller.sal.core.ConstructionException;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.packet.Packet;
import org.opendaylight.controller.sal.packet.PacketResult;
import org.opendaylight.controller.sal.packet.RawPacket;
import org.opendaylight.controller.sal.utils.EtherTypes;
import org.opendaylight.controller.samples.simpleforwarding.IBroadcastHandler;
import org.opendaylight.controller.samples.simpleforwarding.IBroadcastPortSelector;
import org.opendaylight.controller.switchmanager.ISwitchManager;
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
 */
public class SimpleBroadcastHandlerImpl implements IBroadcastHandler, IListenDataPacket {

    private static Logger log = LoggerFactory.getLogger(SimpleBroadcastHandlerImpl.class);
    protected IDataPacketService dataPacketService = null;
    protected ITopologyManager topoManager = null;
    protected ISwitchManager swMgr = null;
    protected IBroadcastPortSelector bcastPorts = null;

    protected ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    BroadcastMode mode = BroadcastMode.BROADCAST_TO_NONINTERNAL;

    @Override
    public PacketResult receiveDataPacket(RawPacket inPkt) {
        /*
         * note that this assumes that the protocol plugin will do appropriate
         * filtering to ensure that this only receives packets for it's
         * container.
         */

        if (mode == BroadcastMode.DISABLED) {
            return PacketResult.IGNORED;
        }

        Packet decodedPkt = this.dataPacketService.decodeDataPacket(inPkt);
        if (decodedPkt instanceof Ethernet) {
            Ethernet eth = (Ethernet) decodedPkt;

            // TODO: should we offer an option to not handle ARP since the
            // ARPHandler already does that

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

        // make sure that topoManager/datPacketService aren't pulled out from
        // under us
        lock.readLock().lock();
        if (topoManager == null || dataPacketService == null
            || swMgr == null) {
            return false;
        }

        // find all non-internal NodeConnectors
        switch (mode) {
            case DISABLED:
                // intentionally blank; don't send the packet anywhere
                break;

            case BROADCAST_TO_HOSTS:
                toPacketOut.addAll(topoManager.getNodeConnectorWithHost());
                break;

            case BROADCAST_TO_NONINTERNAL:
                for (Node n : swMgr.getNodes()) {
                    // getUpNodeConnectors will filter out NodeConnectors of type "SW"
                    for (NodeConnector nc : swMgr.getUpNodeConnectors(n)) {
                        if (!topoManager.isInternal(nc)) {
                            toPacketOut.add(nc);
                        }
                    }
                }
                break;

            case EXTERNAL_QUERY:
                if (bcastPorts != null) {
                    toPacketOut.addAll(bcastPorts.getBroadcastPorts());
                } else {
                    log.error("Mode set to "
                              + BroadcastMode.EXTERNAL_QUERY
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
        for (NodeConnector nc : toPacketOut) {
            try {
                RawPacket toSend = new RawPacket(pkt);
                toSend.setOutgoingNodeConnector(nc);
                dataPacketService.transmitDataPacket(toSend);
            } catch (ConstructionException e) {
                log.error("Could create packet: {}", e);
            }
        }

        lock.readLock().unlock();

        return true;
    }

    public void setDataPacketService(IDataPacketService s) {
        // make sure dataPacketService doesn't change while we're in the middle
        // of stuff
        lock.writeLock().lock();
        this.dataPacketService = s;
        lock.writeLock().unlock();
    }

    public void unsetDataPacketService(IDataPacketService s) {
        // make sure dataPacketService doesn't change while we're in the middle
        // of stuff
        lock.writeLock().lock();
        if (this.dataPacketService == s) {
            this.dataPacketService = null;
        }
        lock.writeLock().unlock();
    }

    public void setTopologyManager(ITopologyManager t) {
        // make sure topoManager doesn't change while we're in the middle of
        // stuff
        lock.writeLock().lock();
        this.topoManager = t;
        lock.writeLock().unlock();
    }

    public void unsetTopologyManager(ITopologyManager t) {
        // make sure topoManager doesn't change while we're in the middle of
        // stuff
        lock.writeLock().lock();
        if (this.topoManager == t) {
            this.topoManager = null;
        }
        lock.writeLock().unlock();
    }

    public void setSwitchManager(ISwitchManager i) {
        lock.writeLock().lock();
        this.swMgr = i;
        lock.writeLock().unlock();
    }

    public void unsetSwitchManager(ISwitchManager i) {
        lock.writeLock().lock();
        if (this.swMgr == i) {
            this.swMgr = null;
        }
        lock.writeLock().unlock();
    }

    public void setBroadcastPortSelector(IBroadcastPortSelector bps) {
        lock.writeLock().lock();
        bcastPorts = bps;
        lock.writeLock().unlock();
    }

    public void unsetBroadcastPortSelector(IBroadcastPortSelector bps) {
        lock.writeLock().lock();
        if (bcastPorts == bps) {
            this.bcastPorts = null;
        }
        lock.writeLock().unlock();
    }

    public void setMode(BroadcastMode m) {
        lock.writeLock().lock();
        mode = m;
        lock.writeLock().unlock();
    }

}
