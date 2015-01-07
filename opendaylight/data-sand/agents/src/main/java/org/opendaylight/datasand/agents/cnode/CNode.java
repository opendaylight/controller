/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */
package org.opendaylight.datasand.agents.cnode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.opendaylight.datasand.agents.AutonomousAgent;
import org.opendaylight.datasand.agents.AutonomousAgentManager;
import org.opendaylight.datasand.agents.cnode.handlers.ARPMulticastHandler;
import org.opendaylight.datasand.agents.cnode.handlers.AcknowledgeHandler;
import org.opendaylight.datasand.agents.cnode.handlers.NodeJoinHandler;
import org.opendaylight.datasand.agents.cnode.handlers.SetCurrentPeerIDHandler;
import org.opendaylight.datasand.agents.cnode.handlers.SetCurrentPeerIDReplyHandler;
import org.opendaylight.datasand.agents.cnode.handlers.PeerSyncDataHandler;
import org.opendaylight.datasand.agents.cnode.handlers.EnterSyncModeHandler;
import org.opendaylight.datasand.agents.cnode.handlers.NodeOriginalDataHandler;
import org.opendaylight.datasand.agents.cnode.handlers.RequestJournalDataHandler;
import org.opendaylight.datasand.codec.bytearray.ByteEncoder;
import org.opendaylight.datasand.network.NetworkID;
import org.opendaylight.datasand.network.NetworkNodeConnection;
import org.opendaylight.datasand.network.Packet;
/**
 * @author - Sharon Aicler (saichler@cisco.com)
 */
public abstract class CNode<DataType,DataTypeElement> extends AutonomousAgent{

    public static final int NODE_JOIN                   = 100;
    public static final int ARP_MULTICAST               = 110;
    public static final int ENTER_SYNC_MODE             = 120;
    public static final int NODE_ORIGINAL_DATA          = 130;
    public static final int REQUEST_JOURNAL_DATA        = 140;
    public static final int PEER_SYNC_DATA              = 150;
    public static final int SET_CURRENT_PEER_ID         = 160;
    public static final int SET_CURRENT_PEER_ID_REPLY   = 170;
    public static final int ACKNOWLEDGE                 = 180;

    static {
        ByteEncoder.registerSerializer(CNodeCommand.class, new CNodeCommand(), CNodeClassCodes.CODE_CNodeCommand);
    }

    private NetworkID multicastGroupNetworkID = null;
    private int multicastGroupID = -1;
    private Object arpID = new Object();
    private Object timeoutID = new Object();
    private long nextID = 1000;
    private Map<Long,CNodeCommandEntry<DataTypeElement>> journal = new LinkedHashMap<Long, CNodeCommandEntry<DataTypeElement>>();
    private Map<NetworkID,CPeerEntry<DataType>> peers = new HashMap<NetworkID,CPeerEntry<DataType>>();
    private List<NetworkID> sortedID = new ArrayList<NetworkID>();
    private DataType localData = createDataTypeInstance();
    private Map<Integer,ICNodeCommandHandler<DataType,DataTypeElement>> handlers = new HashMap<Integer,ICNodeCommandHandler<DataType,DataTypeElement>>();
    private boolean synchronizing = false;

    public CNode(int subSystemId, AutonomousAgentManager m,int _multicastGroupID){
        super(subSystemId,m);
        this.sortedID.add(this.getAgentID());
        this.multicastGroupID = _multicastGroupID;
        this.multicastGroupNetworkID = new NetworkID(
                NetworkNodeConnection.PROTOCOL_ID_BROADCAST.getIPv4Address(),
                multicastGroupID, multicastGroupID);

        this.registerHandler(NODE_JOIN, new NodeJoinHandler<DataType,DataTypeElement>());
        this.registerHandler(ACKNOWLEDGE, new AcknowledgeHandler<DataType,DataTypeElement>());
        this.registerHandler(NODE_ORIGINAL_DATA, new NodeOriginalDataHandler<DataType,DataTypeElement>());
        this.registerHandler(ENTER_SYNC_MODE, new EnterSyncModeHandler<DataType,DataTypeElement>());
        this.registerHandler(REQUEST_JOURNAL_DATA, new RequestJournalDataHandler<DataType,DataTypeElement>());
        this.registerHandler(PEER_SYNC_DATA, new PeerSyncDataHandler<DataType,DataTypeElement>());
        this.registerHandler(SET_CURRENT_PEER_ID, new SetCurrentPeerIDHandler<DataType,DataTypeElement>());
        this.registerHandler(SET_CURRENT_PEER_ID_REPLY, new SetCurrentPeerIDReplyHandler<DataType,DataTypeElement>());
        this.registerHandler(ARP_MULTICAST, new ARPMulticastHandler<DataType,DataTypeElement>());

        m.registerForMulticast(multicastGroupID, this);
        Packet arpSend = new Packet(arpID,this.getAgentID(), this.getAgentID());
        this.addRepetitiveFrame(arpSend, 10000, 10000, 0);
        Packet timeoutMonitor = new Packet(timeoutID,this.getAgentID(), this.getAgentID());
        this.addRepetitiveFrame(timeoutMonitor, 10000, 10000, 0);
        this.multicast(new CNodeCommand<DataTypeElement>(this.nextID,NODE_JOIN,null));
    }

    public long incrementID(){
        long result = this.nextID++;
        return result;
    }

    public List<NetworkID> getSortedList(){
        return this.sortedID;
    }

    public void registerHandler(Integer pType,ICNodeCommandHandler<DataType, DataTypeElement> handler){
        this.handlers.put(pType, handler);
    }

    public long getNextID(){
        return this.nextID;
    }

    public void setSynchronizing(boolean b){
        this.synchronizing = b;
    }

    public boolean isSynchronizing(){
        return this.synchronizing;
    }

    public void multicast(CNodeCommand<DataTypeElement> cNodeCommand){
        this.send(cNodeCommand, this.multicastGroupNetworkID);
    }

    public void sendARPBroadcast(){
        multicast(new CNodeCommand<DataTypeElement>(this.nextID-1,ARP_MULTICAST,null));
    }

    public void checkForTimeout(){
        for(Iterator<CNodeCommandEntry<DataTypeElement>> iter=journal.values().iterator();iter.hasNext();){
            CNodeCommandEntry<DataTypeElement> e = iter.next();
            if(e.isTimeOut()){
                System.out.println("timeout on "+e);
            }
        }
    }

    public CNodeCommandEntry<DataTypeElement> getJournalEntry(CNodeCommand<DataTypeElement> command){
        return this.journal.get(command.getID());
    }

    public void removeJournalEntry(CNodeCommand<DataTypeElement> command){
        this.journal.remove(command.getID());
    }

    public void addJournalForMulticast(CNodeCommand<DataTypeElement> command){
        if(this.peers.size()>0){
            CNodeCommandEntry<DataTypeElement> entry = new CNodeCommandEntry<DataTypeElement>(command);
            boolean hasOnePeer = false;
            for(CPeerEntry<DataType> e:this.peers.values()){
                if(!e.isUnreachable()){
                    hasOnePeer=true;
                    entry.addToWaitingPeerReply(e.getNetworkID());
                }
            }
            if(hasOnePeer)
                journal.put(entry.getcNodeCommand().getID(), entry);
        }
    }

    public void addJournalForUnicast(CNodeCommand<DataTypeElement> command,NetworkID destination){
        if(this.peers.size()>0){
            CNodeCommandEntry<DataTypeElement> entry = new CNodeCommandEntry<DataTypeElement>(command);
            journal.put(entry.getcNodeCommand().getID(), entry);
            entry.addToWaitingPeerReply(destination);
        }
    }

    @Override
    public void processNext(Packet frame, Object obj) {
        if(obj==timeoutID){
            checkForTimeout();
        }else
        if(obj==arpID){
            sendARPBroadcast();
        }else
        if (!frame.getSource().equals(this.getAgentID()) && obj instanceof CNodeCommand) {
            if(frame.getSource().getSubSystemID()==NetworkNodeConnection.DESTINATION_UNREACHABLE){
                processCommand((CNodeCommand<DataTypeElement>) obj, frame.getSource(),frame.getUnreachableOrigAddress());
            }else
                processCommand((CNodeCommand<DataTypeElement>) obj, frame.getSource(),null);
        }
    }

    public CPeerEntry<DataType> getPeerEntry(NetworkID source){
        if(source.equals(this.getAgentID())) return null;
        CPeerEntry<DataType> peerEntry = (CPeerEntry<DataType>)peers.get(source);
        if(peerEntry==null){
            peerEntry = new CPeerEntry<DataType>(source, createDataTypeInstance());
            peers.put(source, peerEntry);
            sortedID.add(source);
            Collections.sort(sortedID, new NetworkIDComparator());
        }
        return peerEntry;
    }

    private void processCommand(CNodeCommand<DataTypeElement> cmd, NetworkID source,NetworkID unreachableOriginalSource){
        boolean isUnreachable = source.equals(NetworkNodeConnection.PROTOCOL_ID_UNREACHABLE);
        //Retrieve the peer entry
        CPeerEntry<DataType> peerEntry = null;
        if(!isUnreachable){
            peerEntry = getPeerEntry(source);
        }

        ICNodeCommandHandler<DataType,DataTypeElement> handle = this.handlers.get(cmd.getOperation());
        handle.handle(cmd,isUnreachable,source,unreachableOriginalSource,peerEntry,this);
    }

    public DataType getLocalData(){
        return this.localData;
    }

    public Collection<CNodeCommandEntry<DataTypeElement>> getJournalEntries(){
        return this.journal.values();
    }

    public void cleanJournalHistoryForSource(NetworkID source){
        //Remove any expected replys from this node as it is up.
        List<CNodeCommand<DataTypeElement>> finished = new LinkedList<CNodeCommand<DataTypeElement>>();
        for(CNodeCommandEntry<DataTypeElement> e:this.journal.values()){
            e.removeFromWaitingPeerReply(source);
            if(e.isEmpty()){
               finished.add(e.getcNodeCommand());
            }
        }
        for(CNodeCommand<DataTypeElement> c:finished){
            this.removeJournalEntry(c);
        }
        //Update this node change id it the source node
        this.send(new CNodeCommand<DataTypeElement>(this.nextID-1,SET_CURRENT_PEER_ID,null), source);
    }

    public void sendAcknowledge(CNodeCommand<DataTypeElement> cNodeCommand,NetworkID source){
        send(new CNodeCommand<DataTypeElement>(cNodeCommand.getID(),ACKNOWLEDGE, null), source);
    }

    public abstract DataType createDataTypeInstance();
    public abstract Collection<DataTypeElement> getDataTypeElementCollection(DataType data);
    public abstract void handleNodeOriginalData(DataTypeElement dataTypeElement);
    public abstract void handlePeerSyncData(DataTypeElement dataTypeElement,NetworkID source);
    public abstract boolean isLocalPeerCopyContainData(DataType data);

    @Override
    public void start() {
        // TODO Auto-generated method stub
    }

    @Override
    public String getName() {
        return "Cluster Node "+this.getAgentID();
    }

    public void log(String str){
        StringBuffer buff = new StringBuffer("Node ").append(this.getAgentID().getPort()).append(" - ");
        buff.append(str);
        System.out.println(buff);
    }
}
