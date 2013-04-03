
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

/**
 * @file   DataPacketService.java
 *
 * @brief  Implementation of Data Packet services in SAL
 *
 * Implementation of Data Packet services in SAL
 */

package org.opendaylight.controller.sal.implementation.internal;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.opendaylight.controller.sal.core.ConstructionException;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.match.Match;
import org.opendaylight.controller.sal.packet.Ethernet;
import org.opendaylight.controller.sal.packet.IDataPacketService;
import org.opendaylight.controller.sal.packet.IListenDataPacket;
import org.opendaylight.controller.sal.packet.IPluginInDataPacketService;
import org.opendaylight.controller.sal.packet.IPluginOutDataPacketService;
import org.opendaylight.controller.sal.packet.LinkEncap;
import org.opendaylight.controller.sal.packet.Packet;
import org.opendaylight.controller.sal.packet.PacketResult;
import org.opendaylight.controller.sal.packet.RawPacket;
import org.opendaylight.controller.sal.utils.NetUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataPacketService implements IPluginOutDataPacketService,
        IDataPacketService {
    private int RXMAXQUEUESIZE = 1000;
    private int TXMAXQUEUESIZE = 1000;
    protected static final Logger logger = LoggerFactory
            .getLogger(DataPacketService.class);
    /**
     * Database that associates one NodeIDType to the
     * IPluginDataPacketService, in fact we expect that there will be
     * one instance of IPluginDataPacketService for each southbound
     * plugin.
     * Using the ConcurrentHashMap because the threads that will be
     * adding a new service, removing a service, going through all of
     * them maybe different.
     */
    private ConcurrentHashMap<String, IPluginInDataPacketService>
        pluginInDataService =
        new ConcurrentHashMap<String, IPluginInDataPacketService>();
    private Map<String, AtomicInteger> statistics = new HashMap<String, AtomicInteger>();
    /**
     * Queue for packets received from Data Path
     */
    private LinkedBlockingQueue<RawPacket> rxQueue = new LinkedBlockingQueue<RawPacket>(
            RXMAXQUEUESIZE);
    /**
     * Queue for packets that need to be transmitted to Data Path
     */
    private LinkedBlockingQueue<RawPacket> txQueue = new LinkedBlockingQueue<RawPacket>(
            RXMAXQUEUESIZE);
    /**
     * Transmission thread
     */
    private Thread txThread = new Thread(new TxLoop(),
            "DataPacketService TX thread");
    /**
     * Receiving thread
     */
    private Thread rxThread = new Thread(new RxLoop(),
            "DataPacketService RX thread");

    /**
     * Representation of a Data Packet Listener including of its
     * properties
     *
     */
    private class DataPacketListener {
        // Key fields
        private String listenerName;
        // Attribute fields
        private IListenDataPacket listener;
        private String dependency;
        private Match match;

        DataPacketListener(String name, IListenDataPacket s, String dependency,
                Match match) {
            this.listenerName = name;
            this.listener = s;
            this.dependency = dependency;
            this.match = match;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (obj == this) {
                return true;
            }
            if (obj.getClass() != getClass()) {
                return false;
            }
            DataPacketListener rhs = (DataPacketListener) obj;
            return new EqualsBuilder().append(this.listenerName,
                    rhs.listenerName).isEquals();
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder(13, 31).append(listenerName)
                    .toHashCode();
        }
    }

    /**
     * This very expensive version of List is being used because it
     * work well in concurrent situation, as we expect new service
     * addition, service removal and walk of the service will happen
     * from different places
     */
    private List<List<DataPacketListener>> listenDataPacket = new CopyOnWriteArrayList<List<DataPacketListener>>();
    // Quick index to make sure there are no duplicate elements
    private Set<DataPacketListener> indexDataPacket = Collections
            .synchronizedSet(new HashSet<DataPacketListener>());

    /**
     * Loop for processing Received packets
     *
     */
    private class RxLoop implements Runnable {
        public void run() {
            RawPacket pkt;
            try {
                for (pkt = rxQueue.take(); pkt != null; pkt = rxQueue.take()) {
                    for (List<DataPacketListener> serialListeners : listenDataPacket) {
                        int i = 0;
                        for (i = 0; i < serialListeners.size(); i++) {
                            RawPacket copyPkt = null;
                            try {
                                copyPkt = new RawPacket(pkt);
                            } catch (ConstructionException cex) {
                                logger.debug("Error while cloning the packet");
                            }
                            if (copyPkt == null) {
                                increaseStat("RXPacketCopyFailed");
                                continue;
                            }
                            DataPacketListener l = serialListeners.get(i);
                            IListenDataPacket s = (l == null ? null
                                    : l.listener);
                            if (s != null) {
                                try {
                                    // TODO Make sure to filter based
                                    // on the match too, later on
                                    PacketResult res = s
                                            .receiveDataPacket(copyPkt);
                                    increaseStat("RXPacketSuccess");
                                    if (res.equals(PacketResult.CONSUME)) {
                                        increaseStat("RXPacketSerialExit");
                                        break;
                                    }
                                } catch (Exception e) {
                                    increaseStat("RXPacketFailedForException");
                                }
                            }
                        }
                    }
                }
            } catch (InterruptedException e) {
                // Not a big deal
            }
        }
    }

    /**
     * Loop for processing packets to be transmitted
     *
     */
    private class TxLoop implements Runnable {
        public void run() {
            RawPacket pkt;
            try {
                for (pkt = txQueue.take(); pkt != null; pkt = txQueue.take()) {
                    // Retrieve outgoing node connector so to send out
                    // the packet to corresponding node
                    NodeConnector p = pkt.getOutgoingNodeConnector();
                    if (p != null) {
                        String t = p.getNode()
                                .getType();
                        // Now locate the TX dispatcher
                        IPluginInDataPacketService s = pluginInDataService
                                .get(t);
                        if (s != null) {
                            try {
                                s.transmitDataPacket(pkt);
                                increaseStat("TXPacketSuccess");
                            } catch (Exception e) {
                                increaseStat("TXPacketFailedForException");
                            }
                        } else {
                            increaseStat("TXpluginNotFound");
                        }
                    }
                }
            } catch (InterruptedException e) {
                // Not a big deal
            }
        }
    }

    void setPluginInDataService(Map props, IPluginInDataPacketService s) {
        if (this.pluginInDataService == null) {
            logger.error("pluginInDataService store null");
            return;
        }
        String type = null;
        logger.trace("Received setPluginInDataService request");
        for (Object e : props.entrySet()) {
            Map.Entry entry = (Map.Entry) e;
            logger.trace("Prop key:({}) value:({})",entry.getKey(), entry.getValue());
        }

        Object value = props.get("protocolPluginType");
        if (value instanceof String) {
            type = (String) value;
        }
        if (type == null) {
            logger.error("Received a PluginInDataService without any "
                    + "protocolPluginType provided");
        } else {
            this.pluginInDataService.put(type, s);
            logger.debug("Stored the PluginInDataService for type: {}", type);
        }
    }

    void unsetPluginInDataService(Map props, IPluginInDataPacketService s) {
        if (this.pluginInDataService == null) {
            logger.error("pluginInDataService store null");
            return;
        }

        String type = null;
        logger.trace("Received unsetPluginInDataService request");
        for (Object e : props.entrySet()) {
            Map.Entry entry = (Map.Entry) e;
            logger.trace("Prop key:({}) value:({})",entry.getKey(), entry.getValue());
        }

        Object value = props.get("protocoloPluginType");
        if (value instanceof String) {
            type = (String) value;
        }
        if (type == null) {
            logger.error("Received a PluginInDataService without any "
                    + "protocolPluginType provided");
        } else if (this.pluginInDataService.get(type).equals(s)) {
            this.pluginInDataService.remove(type);
            logger.debug("Removed the PluginInDataService for type: {}", type);
        }
    }

    void setListenDataPacket(Map props, IListenDataPacket s) {
        if (this.listenDataPacket == null || this.indexDataPacket == null) {
            logger.error("data structure to store data is NULL");
            return;
        }
        logger.trace("Received setListenDataPacket request");
        for (Object e : props.entrySet()) {
            Map.Entry entry = (Map.Entry) e;
            logger.trace("Prop key:({}) value:({})",entry.getKey(), entry.getValue());
        }

        String listenerName = null;
        String listenerDependency = null;
        Match filter = null;
        Object value;
        // Read the listenerName
        value = props.get("salListenerName");
        if (value instanceof String) {
            listenerName = (String) value;
        }

        if (listenerName == null) {
            logger.error("Trying to set a listener without a Name");
            return;
        }

        //Read the dependency
        value = props.get("salListenerDependency");
        if (value instanceof String) {
            listenerDependency = (String) value;
        }

        //Read match filter if any
        value = props.get("salListenerFilter");
        if (value instanceof Match) {
            filter = (Match) value;
        }

        DataPacketListener l = new DataPacketListener(listenerName, s,
                listenerDependency, filter);

        DataPacketListener lDependency = new DataPacketListener(
                listenerDependency, null, null, null);

        // Now let see if there is any dependency
        if (listenerDependency == null) {
            logger.debug("listener without any dependency");
            if (this.indexDataPacket.contains(l)) {
                logger.error("trying to add an existing element");
            } else {
                logger.debug("adding listener: {}", listenerName);
                CopyOnWriteArrayList<DataPacketListener> serialListeners = new CopyOnWriteArrayList<DataPacketListener>();
                serialListeners.add(l);
                this.listenDataPacket.add(serialListeners);
                this.indexDataPacket.add(l);
            }
        } else {
            logger.debug("listener with dependency");
            // Now search for the dependency and put things in order
            if (this.indexDataPacket.contains(l)) {
                logger.error("trying to add an existing element");
            } else {
                logger.debug("adding listener: {}", listenerName);
                // Lets find the set with the dependency in it, if we
                // find it lets just add our dependency at the end of
                // the list.
                for (List<DataPacketListener> serialListeners : this.listenDataPacket) {
                    int i = 0;
                    boolean done = false;
                    if (serialListeners.contains(lDependency)) {
                        serialListeners.add(l);
                        done = true;
                    }
                    // If we did fine the element, lets break early
                    if (done) {
                        break;
                    }
                }

                this.indexDataPacket.add(l);
            }
        }
    }

    void unsetListenDataPacket(Map props, IListenDataPacket s) {
        if (this.listenDataPacket == null || this.indexDataPacket == null) {
            logger.error("data structure to store data is NULL");
            return;
        }
        logger.trace("Received UNsetListenDataPacket request");
        for (Object e : props.entrySet()) {
            Map.Entry entry = (Map.Entry) e;
            logger.trace("Prop key:({}) value:({})",entry.getKey(), entry.getValue());
        }

        String listenerName = null;
        Object value;
        // Read the listenerName
        value = props.get("salListenerName");
        if (value instanceof String) {
            listenerName = (String) value;
        }

        if (listenerName == null) {
            logger.error("Trying to set a listener without a Name");
            return;
        }

        DataPacketListener l = new DataPacketListener(listenerName, s, null,
                null);
        if (!this.indexDataPacket.contains(l)) {
            logger.error("trying to remove a non-existing element");
        } else {
            logger.debug("removing listener: {}", listenerName);
            for (List<DataPacketListener> serialListeners : this.listenDataPacket) {
                int i = 0;
                boolean done = false;
                for (i = 0; i < serialListeners.size(); i++) {
                    if (serialListeners.get(i).equals(l)) {
                        serialListeners.remove(i);
                        done = true;
                        break;
                    }
                }
                // Now remove a serialListener that maybe empty
                if (serialListeners.isEmpty()) {
                    this.listenDataPacket.remove(serialListeners);
                }
                // If we did fine the element, lets break early
                if (done) {
                    break;
                }
            }

            this.indexDataPacket.remove(l);
        }
    }

    /**
     * Function called by the dependency manager when all the required
     * dependencies are satisfied
     *
     */
    void init() {
        this.txThread.start();
        this.rxThread.start();
    }

    /**
     * Function called by the dependency manager when at least one
     * dependency become unsatisfied or when the component is shutting
     * down because for example bundle is being stopped.
     *
     */
    void destroy() {
        // Make sure to cleanup the data structure we use to track
        // services
        this.listenDataPacket.clear();
        this.indexDataPacket.clear();
        this.pluginInDataService.clear();
        this.statistics.clear();
        this.rxQueue.clear();
        this.txQueue.clear();
        this.txThread.interrupt();
        this.rxThread.interrupt();
        // Wait for them to be done
        try {
            this.txThread.join();
            this.rxThread.join();
        } catch (InterruptedException ex) {
            // Not a big deal
        }
    }

    private void increaseStat(String name) {
        if (this.statistics == null) {
            return;
        }

        AtomicInteger currValue = null;
        synchronized (this.statistics) {
            currValue = this.statistics.get(name);

            if (currValue == null) {
                this.statistics.put(name, new AtomicInteger(0));
                return;
            }
        }
        currValue.incrementAndGet();
    }

    @Override
    public PacketResult receiveDataPacket(RawPacket inPkt) {
        if (inPkt.getIncomingNodeConnector() == null) {
            increaseStat("nullIncomingNodeConnector");
            return PacketResult.IGNORED;
        }

        // If the queue was full don't wait, rather increase a counter
        // for it
        if (!this.rxQueue.offer(inPkt)) {
            increaseStat("fullRXQueue");
            return PacketResult.IGNORED;
        }

        // Walk the chain of listener going first throw all the
        // parallel ones and for each parallel in serial
        return PacketResult.IGNORED;
    }

    @Override
    public void transmitDataPacket(RawPacket outPkt) {
        if (outPkt.getOutgoingNodeConnector() == null) {
            increaseStat("nullOutgoingNodeConnector");
            return;
        }

        if (!this.txQueue.offer(outPkt)) {
            increaseStat("fullTXQueue");
            return;
        }
    }

    @Override
    public Packet decodeDataPacket(RawPacket pkt) {
        // Sanity checks
        if (pkt == null) {
            return null;
        }
        byte[] data = pkt.getPacketData();
        if (data.length <= 0) {
            return null;
        }
        if (pkt.getEncap().equals(LinkEncap.ETHERNET)) {
            Ethernet res = new Ethernet();
            try {
                res.deserialize(data, 0, data.length * NetUtils.NumBitsInAByte);
            } catch (Exception e) {
                logger.warn("", e);
            }
            return res;
        }
        return null;
    }

    @Override
    public RawPacket encodeDataPacket(Packet pkt) {
        // Sanity checks
        if (pkt == null) {
            return null;
        }
        byte[] data;
        try {
            data = pkt.serialize();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        if (data.length <= 0) {
            return null;
        }
        try {
            RawPacket res = new RawPacket(data);
            return res;
        } catch (ConstructionException cex) {
        }
        // If something goes wrong then we have to return null
        return null;
    }
}
