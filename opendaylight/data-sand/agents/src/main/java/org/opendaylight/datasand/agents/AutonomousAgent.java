/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */
package org.opendaylight.datasand.agents;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.opendaylight.datasand.codec.bytearray.ByteArrayEncodeDataContainer;
import org.opendaylight.datasand.codec.bytearray.ByteEncoder;
import org.opendaylight.datasand.network.NetworkID;
import org.opendaylight.datasand.network.NetworkNodeConnection;
import org.opendaylight.datasand.network.Packet;
import org.opendaylight.datasand.network.PriorityLinkedList;
/**
 * @author - Sharon Aicler (saichler@cisco.com)
 */
public abstract class AutonomousAgent implements Runnable {

    private NetworkID agentID = null;
    private AutonomousAgentManager manager = null;
    protected PriorityLinkedList<Packet> incoming = new PriorityLinkedList<Packet>();
    protected boolean working = false;
    private Packet currentFrame = null;
    private List<RepetitiveFrameEntry> repetitiveTasks = new ArrayList<RepetitiveFrameEntry>();
    private long lastRepetitiveCheck = 0;
    private Map<Long,MessageEntry> journal = new LinkedHashMap<Long, MessageEntry>();
    private Map<NetworkID,PeerEntry> peers = new HashMap<NetworkID,PeerEntry>();
    private Message timeoutID = new Message();

    public boolean _ForTestOnly_pseudoSendEnabled = false;

    static {
        ByteEncoder.registerSerializer(Message.class, new Message(), 32);
    }

    public AutonomousAgent(int subSystemID,AutonomousAgentManager _manager) {
        this.agentID = new NetworkID(_manager.getNetworkNode().getLocalHost().getIPv4Address(),_manager.getNetworkNode().getLocalHost().getPort(), subSystemID);
        this.manager = _manager;
        this.manager.registerAgent(this);
        registerRepetitiveMessage(10000, 10000, 0, timeoutID);
    }

    public NetworkID getAgentID() {
        return this.agentID;
    }

    public void addFrame(Packet p) {
        incoming.add(p, p.getPriority());
    }

    public void checkForRepetitive() {
        if (System.currentTimeMillis() - lastRepetitiveCheck > 10000) {
            for (RepetitiveFrameEntry e : repetitiveTasks) {
                if (e.shouldExecute()) {
                    incoming.add(e.frame, e.priority);
                }
            }
            lastRepetitiveCheck = System.currentTimeMillis();
        }
    }

    public void pop() {
        working = true;
        currentFrame = incoming.next();
    }

    public void run() {
        currentFrame.decode(manager.getTypeDescriptorsContainer());
        if(currentFrame.getDecodedObject()==timeoutID){
            this.checkForTimeoutMessages();
        }else
        if(currentFrame.getSource().getSubSystemID()==NetworkNodeConnection.DESTINATION_UNREACHABLE){
            processDestinationUnreachable((Message)currentFrame.getDecodedObject(),currentFrame.getUnreachableOrigAddress());
        }else
        if (currentFrame.getDecodedObject() instanceof ISideTask) {
            this.getAgentManager().runSideTask((ISideTask) currentFrame.getDecodedObject());
        } else {
            processMessage((Message)currentFrame.getDecodedObject(),currentFrame.getSource(),currentFrame.getDestination());
        }
        currentFrame = null;
        synchronized (manager.getSyncObject()) {
            working = false;
            manager.getSyncObject().notifyAll();
        }
    }

    public abstract void processDestinationUnreachable(Message message,NetworkID unreachableSource);
    public abstract void processMessage(Message message,NetworkID source,NetworkID destination);
    public abstract void start();
    public abstract String getName();

    public void send(Message obj, NetworkID destination) {
        if(_ForTestOnly_pseudoSendEnabled) return;
        ByteArrayEncodeDataContainer ba = null;
        if(ByteEncoder.getRegisteredSerializer(obj.getClass())!=null){
            ba = new ByteArrayEncodeDataContainer(1024,this.manager.getTypeDescriptorsContainer().getEmptyTypeDescriptor());
        }else{
            ba = new ByteArrayEncodeDataContainer(1024,this.manager.getTypeDescriptorsContainer().getTypeDescriptorByObject(obj));
        }
        ba.getEncoder().encodeObject(obj, ba,this.manager.getTypeDescriptorsContainer().getElementClass(obj));
        manager.getNetworkNode().send(ba.getData(), this.agentID, destination);
    }

    public void send(byte data[], NetworkID destination) {
        manager.getNetworkNode().send(data, this.agentID, destination);
    }

    public void registerRepetitiveMessage(long interval,long intervalStart,int priority,Message message){
        Packet p = new Packet(message,this.getAgentID(), this.getAgentID());
        RepetitiveFrameEntry entry = new RepetitiveFrameEntry(p, interval,intervalStart, priority);
        if(entry.shouldExecute()){
            incoming.add(entry.frame, entry.priority);
            this.getAgentManager().messageWasEnqueued();
        }
        repetitiveTasks.add(entry);
    }

    private static class RepetitiveFrameEntry {
        private Packet frame = null;
        private long interval = -1;
        private long intervalStart = -1;
        private long lastExecuted = System.currentTimeMillis();
        private boolean started = false;
        private int priority = 2;

        public RepetitiveFrameEntry(Packet _frame, long _interval,long _intervalStart, int _priority) {
            this.frame = _frame;
            this.interval = _interval;
            this.intervalStart = _intervalStart;
            this.priority = _priority;
        }

        public boolean shouldExecute() {
            if (System.currentTimeMillis() - lastExecuted > interval){
                lastExecuted = System.currentTimeMillis();
                return true;
            }

            if (!started && (System.currentTimeMillis() - lastExecuted > intervalStart || intervalStart == 0)) {
                started = true;
                lastExecuted = System.currentTimeMillis();
                return true;
            }
            return false;
        }
    }

    protected AutonomousAgentManager getAgentManager() {
        return this.manager;
    }

    public void checkForTimeoutMessages(){
        for(Iterator<MessageEntry> iter=journal.values().iterator();iter.hasNext();){
            MessageEntry e = iter.next();
            if(e.hasTimedOut()){
                for(NetworkID peer:e.getPeers()){
                    handleTimedOutMessage(e.getMessage(),peer);
                }
            }
        }
    }

    public void handleTimedOutMessage(Message message,NetworkID peer){

    }

    public void addMessageEntry(MessageEntry entry){
        this.journal.put(entry.getMessage().getMessageID(), entry);
    }

    public MessageEntry addUnicastJournal(Message m,NetworkID peer){
        MessageEntry entry = new MessageEntry(m, peer, MessageEntry.DEFAULT_TIMEOUT);
        this.journal.put(m.getMessageID(), entry);
        return entry;
    }

    public MessageEntry addARPJournal(Message message){
        if(this.peers.size()>0){
            MessageEntry entry = new MessageEntry(message);
            boolean hasOnePeer = false;
            for(PeerEntry e:this.peers.values()){
                if(!e.isUnreachable()){
                    hasOnePeer=true;
                    entry.addPeer(e.getNetworkID());
                }
            }
            if(hasOnePeer)
                this.addMessageEntry(entry);
            return entry;
        }
        return null;
    }

    public PeerEntry getPeerEntry(NetworkID source){
        if(source.equals(this.getAgentID())) return null;
        PeerEntry peerEntry = peers.get(source);
        if(peerEntry==null){
            peerEntry = new PeerEntry(source);
            peers.put(source, peerEntry);
        }
        return peerEntry;
    }

    public void replacePeerEntry(NetworkID source,PeerEntry entry){
        this.peers.put(source, entry);
    }

    public void addPeerToARPJournal(Message m,NetworkID peer){
        MessageEntry entry = journal.get(m.getMessageID());
        entry.addPeer(peer);
    }

    public MessageEntry getJournalEntry(Message m){
        return this.journal.get(m.getMessageID());
    }

    public MessageEntry removeJournalEntry(Message m){
        return this.journal.remove(m.getMessageID());
    }

    public Collection<MessageEntry> getJournalEntries(){
        return this.journal.values();
    }
}
