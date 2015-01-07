package org.opendaylight.datasand.agents.clustermap1;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.opendaylight.datasand.agents.AutonomousAgent;
import org.opendaylight.datasand.agents.AutonomousAgentManager;
import org.opendaylight.datasand.codec.bytearray.ByteEncoder;
import org.opendaylight.datasand.network.NetworkID;
import org.opendaylight.datasand.network.NetworkNodeConnection;
import org.opendaylight.datasand.network.Packet;

public class ClusterMap<K, V> extends AutonomousAgent implements Map<K, V> {

    private Map<K, V> map = new ConcurrentHashMap<K, V>();
    private Map<NetworkID,PeerEntry> peers = new HashMap<NetworkID,PeerEntry>();
    private Object arpTaskIdentifier = new Object();
    private Object timeoutTaskIdentifier = new Object();
    private Map<ChangeID,CCommandEntry> journal = new LinkedHashMap<ChangeID, ClusterMap.CCommandEntry>();
    private ChangeID lastChangeID = null;
    private static final int SYNC_MODE_OFF = 5;
    private static final int SYNC_MODE_ON = 10;
    private int syncModeFlag = SYNC_MODE_OFF;
    private IClusterMapListener<K, V> listener=null;

    public static int CLUSTER_MAP_MULTICAST_GROUP_ID = 223;
    public static NetworkID MULTICAST = new NetworkID(
            NetworkNodeConnection.PROTOCOL_ID_BROADCAST.getIPv4Address(),
            CLUSTER_MAP_MULTICAST_GROUP_ID, CLUSTER_MAP_MULTICAST_GROUP_ID);

    public ClusterMap(int subSystemId, AutonomousAgentManager m,IClusterMapListener<K, V> _listener) {
        super(subSystemId, m);
        this.listener = _listener;
        m.registerForMulticast(CLUSTER_MAP_MULTICAST_GROUP_ID, this);
        Packet arpSend = new Packet(arpTaskIdentifier,this.getAgentID(), this.getAgentID());
        this.lastChangeID = new ChangeID(this.getAgentID());
        this.addRepetitiveFrame(arpSend, 15000, 0, 0);
        Packet timeoutMonitor = new Packet(timeoutTaskIdentifier,this.getAgentID(), this.getAgentID());
        this.addRepetitiveFrame(timeoutMonitor, 10000, 0, 0);
    }

    private static final int C_PUT = 1;
    private static final int C_PUT_ALL = 3;
    private static final int C_SYNC_PUT = 4;
    private static final int C_CHANGEID = 5;
    private static final int C_ARP_BROADCAST = 2;
    private static final int C_REMOVE = 6;
    private static final int C_SYNC_FLUSH = 7;
    private static final int C_SYNC_START = 8;
    private static final int C_SYNC_MODE = 9;
    private static final int C_SYNC_FINISH = 10;
    private static final int C_CONFIRM_OPERATION = 99;

    static {
        ByteEncoder.registerSerializer(CCommand.class, new CCommand(), ClusterMapClassCodes.CODE_CCommand);
        ByteEncoder.registerSerializer(ChangeID.class, new ChangeID(), ClusterMapClassCodes.CODE_ChangeID);
    }

    public static class CCommandEntry {
        private CCommand ccommand = null;
        private Set<NetworkID> expectedReplyFrom = new HashSet<NetworkID>();
        private long insertTime = -1;
    }

    public static class PeerEntry {
        public NetworkID netID = null;
        public long lastReceivedPing = -1;
        public ChangeID changeID = null;
        public int size = -1;
    }

    public boolean isSynchronizing(){
        return this.syncModeFlag==SYNC_MODE_ON;
    }

    private void checkForTimeout(){
        for(Iterator<CCommandEntry> iter=journal.values().iterator();iter.hasNext();){
            CCommandEntry e = iter.next();
            if(System.currentTimeMillis()-e.insertTime>5000){
                if(e.ccommand.getOperation()==C_SYNC_MODE){
                    iter.remove();
                    startSynchronization();
                }else
                if(e.ccommand.getOperation()==C_SYNC_FLUSH){
                    iter.remove();
                    List<NetworkID> pl = (List<NetworkID>)e.ccommand.getKey();
                    NetworkID id = pl.remove(0);
                    System.out.println("Timeout during synchronization from "+id+", skipping to next one");
                    CCommand c = new CCommand(C_SYNC_FLUSH,pl, null, this.getAgentID());
                    send(c,pl.get(0));
                }
            }
        }
    }

    @Override
    public void processNext(Packet frame, Object obj) {
        if(obj==timeoutTaskIdentifier){
            checkForTimeout();
        }else
        if(obj==arpTaskIdentifier){
            sendARPBroadcast();
        }else
        if (!frame.getSource().equals(this.getAgentID()) && obj instanceof CCommand) {
            if(frame.getSource().getSubSystemID()==NetworkNodeConnection.DESTINATION_UNREACHABLE){
                processCCommand((CCommand) obj, frame.getSource(),frame.getUnreachableOrigAddress());
            }else
                processCCommand((CCommand) obj, frame.getSource(),null);
        }
    }

    public void sendARPBroadcast(){
        send(new CCommand(C_ARP_BROADCAST, this.lastChangeID, map.size(),this.getAgentID()), MULTICAST);
    }

    public void sendAll(NetworkID dest){
        for(Map.Entry<K, V> entry:map.entrySet()){
            this.send(new CCommand(C_SYNC_PUT,entry.getKey(),entry.getValue(),this.getAgentID()), dest);
        }
    }

    private NetworkID getSyncMaster(){
        NetworkID syncMaster = this.getAgentID();
        for(PeerEntry entry:peers.values()){
            if(entry.netID.getIPv4Address()<=syncMaster.getIPv4Address() && entry.netID.getPort()==50000){
                syncMaster = entry.netID;
            }
        }
        return syncMaster;
    }

    private void addJurnalForMulticast(CCommand command){
        if(this.peers.size()>0){
            CCommandEntry entry = new CCommandEntry();
            entry.ccommand = command;
            journal.put(entry.ccommand.getChangeID(), entry);
            entry.expectedReplyFrom.addAll(this.peers.keySet());
            entry.insertTime = System.currentTimeMillis();
        }
    }

    private void addJurnalForUnicast(CCommand command,NetworkID destination){
        if(this.peers.size()>0){
            CCommandEntry entry = new CCommandEntry();
            entry.ccommand = command;
            journal.put(entry.ccommand.getChangeID(), entry);
            entry.expectedReplyFrom.add(destination);
            entry.insertTime = System.currentTimeMillis();
        }
    }

    private boolean checkForUnreachable(boolean isUnreachable,CCommand ccommand,NetworkID source,NetworkID unreachableOriginalSource){
        //if this is not an unreachable frame, then check if we need to add it
        //to the known peers
        if(!isUnreachable && !peers.containsKey(source)){
            PeerEntry entry = (PeerEntry)peers.get(source);
            if(entry==null){
                entry = new PeerEntry();
                entry.netID = source;
                peers.put(source, entry);
            }
        }else
        if(isUnreachable){
            //if this is unreachable frame, try to remove the peer from the known peers
            for(Iterator<Map.Entry<NetworkID, PeerEntry>> iter=peers.entrySet().iterator();iter.hasNext();){
                PeerEntry entry = iter.next().getValue();
                if(entry.netID.getIPv4Address()==unreachableOriginalSource.getIPv4Address() && entry.netID.getPort()==unreachableOriginalSource.getPort()){
                    iter.remove();
                }
            }
            //The node is down, remove it from the expected reply list, if exist
            CCommandEntry entry = journal.get(ccommand.getChangeID());
            if(entry!=null){
                for(Iterator<NetworkID> iter=entry.expectedReplyFrom.iterator();iter.hasNext();){
                    NetworkID nid = iter.next();
                    if(nid.getIPv4Address()==unreachableOriginalSource.getIPv4Address() && nid.getPort()==unreachableOriginalSource.getPort()){
                        iter.remove();
                    }
                }
                //If this is the last reply, invoke the end confirmation
                if(entry.expectedReplyFrom.size()==0){
                    receiveConfirmation(ccommand, source);
                    return true;
                }
            }
        }
        return false;
    }

    public void handleStartSyncMode(){
        if(!(this.syncModeFlag==SYNC_MODE_ON)){
            this.syncModeFlag = SYNC_MODE_ON;
            CCommand syncModeCMD = new CCommand(C_SYNC_MODE,null,null,this.getAgentID());
            addJurnalForMulticast(syncModeCMD);
            send(syncModeCMD,MULTICAST);
        }
    }

    public void handleSyncFlush(CCommand ccommand,boolean isUnreachable,NetworkID source){
        List<NetworkID> pl = (List<NetworkID>)ccommand.getKey();
        //if the list is 1, hence we are back in the sync master and the sync is finished. We need to move back to regular mode...
        if(pl.size()==1){
            CCommand syncFinish = new CCommand(C_SYNC_FINISH,null,null,this.getAgentID());
            this.lastChangeID = syncFinish.getChangeID();
            send(syncFinish,MULTICAST);
            synchronized(this){
                this.syncModeFlag = SYNC_MODE_OFF;
                this.notifyAll();
            }
        }else{
            //Unreachable node, remove the node from the list and try the next one...
            if(isUnreachable){
                pl.remove(0);
                CCommand c = new CCommand(C_SYNC_FLUSH,pl, null, this.getAgentID());
                addJurnalForUnicast(c,pl.get(0));
                send(c,pl.get(0));
            }else{
                //Flush all the jurnal data
                for(Iterator<CCommandEntry> iter=journal.values().iterator();iter.hasNext();){
                    CCommandEntry entry = iter.next();
                    if(entry.ccommand.getOperation()==C_PUT){
                        this.map.put((K)entry.ccommand.getKey(), (V)entry.ccommand.getValue());
                        this.send(entry.ccommand, MULTICAST);
                        iter.remove();
                    }else
                    if(entry.ccommand.getOperation()==C_REMOVE){
                        this.map.remove((K)entry.ccommand.getKey());
                        this.send(entry.ccommand, MULTICAST);
                        iter.remove();
                    }
                }
                //send message to the next one
                pl.remove(0);
                CCommand c = new CCommand(C_SYNC_FLUSH,pl, null, this.getAgentID());
                addJurnalForUnicast(c,pl.get(0));
                send(c,pl.get(0));
            }
        }
        sendConfirmation(ccommand, source);
    }

    private void processCCommand(CCommand ccommand, NetworkID source,NetworkID unreachableOriginalSource) {

        boolean isUnreachable = source.equals(NetworkNodeConnection.PROTOCOL_ID_UNREACHABLE);
        boolean isUnreachableLast = checkForUnreachable(isUnreachable, ccommand, source, unreachableOriginalSource);
        if(isUnreachableLast)
            return;

        switch (ccommand.getOperation()) {
        case C_SYNC_MODE:
            if(!(this.syncModeFlag==SYNC_MODE_ON)){
                this.syncModeFlag = SYNC_MODE_ON;
                send(new CCommand(C_CONFIRM_OPERATION, ccommand.getChangeID(), null,this.getAgentID()), source);
            }
            break;
        case C_SYNC_START:
            handleStartSyncMode();
            break;
        case C_ARP_BROADCAST:
            if(!isUnreachable && !source.equals(this.getAgentID())){
                PeerEntry entry = (PeerEntry)peers.get(source);
                //update peer data
                entry.lastReceivedPing = System.currentTimeMillis();
                entry.changeID = (ChangeID)ccommand.getKey();
                entry.size = (Integer)ccommand.getValue();

                if(!entry.changeID.equals(this.lastChangeID) || entry.size!=this.map.size()){
                    NetworkID syncMaster = getSyncMaster();
                    boolean amISyncMaster = this.getAgentID().equals(syncMaster);
                    //new node joined, send all to it...
                    if(entry.size==0){
                        if(amISyncMaster){
                            this.sendAll(source);
                        }
                    }else{
                        if(amISyncMaster){
                            handleStartSyncMode();
                        }else
                            send(new CCommand(C_SYNC_START,null,null,this.getAgentID()),syncMaster);
                    }
                }
            }
            break;
        case C_CHANGEID:
            this.lastChangeID = (ChangeID)ccommand.getKey();
            break;
        case C_SYNC_FLUSH:
            handleSyncFlush(ccommand, isUnreachableLast,source);
            break;
        case C_SYNC_PUT:
            if(!isUnreachable){
                map.put((K) ccommand.getKey(), (V) ccommand.getValue());
            }
            break;
        case C_REMOVE:
            if(!isUnreachable){
                map.remove((K) ccommand.getKey());
                this.lastChangeID = ccommand.getChangeID();
                sendConfirmation(ccommand, source);
                if(listener!=null){
                    try{
                        listener.peerRemove((K) ccommand.getKey());
                    }catch(Exception err){
                        err.printStackTrace();
                    }
                }
            }
            break;
        case C_PUT:
            if(!isUnreachable){
                map.put((K) ccommand.getKey(), (V) ccommand.getValue());
                this.lastChangeID = ccommand.getChangeID();
                sendConfirmation(ccommand, source);
                if(listener!=null){
                    try{
                        listener.peerPut((K) ccommand.getKey(), (V) ccommand.getValue());
                    }catch(Exception err){
                        err.printStackTrace();
                    }
                }
            }
            break;
        case C_SYNC_FINISH:
            if(!isUnreachable){
                this.lastChangeID=ccommand.getChangeID();
                synchronized(this){
                    this.syncModeFlag = 5;
                    this.notifyAll();
                }
            }
            break;
        case C_CONFIRM_OPERATION:
            if(!isUnreachable){
                receiveConfirmation(ccommand, source);
            }
            break;
        }
    }

    private void sendConfirmation(CCommand ccommand,NetworkID source){
        send(new CCommand(C_CONFIRM_OPERATION, ccommand.getChangeID(), null,this.getAgentID()), source);
    }

    private void startSynchronization(){
        for(Iterator<CCommandEntry> iter=journal.values().iterator();iter.hasNext();){
            CCommandEntry e =  iter.next();
            if(e.ccommand.getOperation()==C_PUT){
                this.map.put((K)e.ccommand.getKey(), (V)e.ccommand.getValue());
                this.send(e.ccommand, MULTICAST);
                iter.remove();
            }else
            if(e.ccommand.getOperation()==C_REMOVE){
                this.map.remove((K)e.ccommand.getKey());
                this.send(e.ccommand, MULTICAST);
                iter.remove();
            }
        }

        List<NetworkID> peerList = new LinkedList<NetworkID>();
        for(PeerEntry pe:peers.values()){
            peerList.add(pe.netID);
        }

        peerList.add(this.getAgentID());
        CCommand c = new CCommand(C_SYNC_FLUSH,peerList, null, this.getAgentID());
        addJurnalForUnicast(c,peerList.get(0));
        send(c,peerList.get(0));
    }

    private void receiveConfirmation(CCommand ccommand,NetworkID source){
        CCommandEntry entry = journal.get(ccommand.getKey());
        if(entry!=null){
            entry.expectedReplyFrom.remove(source);
            if(entry.expectedReplyFrom.isEmpty()){
                journal.remove(ccommand.getKey());
                if(entry.ccommand.getOperation()==C_SYNC_MODE){
                    startSynchronization();
                }
            }
        }
    }

    @Override
    public void start() {
    }

    @Override
    public String getName() {
        return "Cluster Map";
    }

    @Override
    public int size() {
        return map.size();
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return map.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return map.containsValue(value);
    }

    @Override
    public V get(Object key) {
        return map.get(key);
    }

    @Override
    public V put(K key, V value) {
        if(isSynchronizing()){
            synchronized(this){
                if(isSynchronizing()){
                    System.out.println("Put Waiting....");
                    try{this.wait();}catch(Exception err){}
                    System.out.println("Finish Put Waiting....");
                }
            }
        }
        CCommand putCommand = new CCommand(C_PUT, key, value,this.getAgentID());
        addJurnalForMulticast(putCommand);
        send(putCommand, MULTICAST);
        return map.put(key, value);
    }

    @Override
    public V remove(Object key) {
        if(isSynchronizing()){
            synchronized(this){
                if(isSynchronizing()){
                    try{this.wait();}catch(Exception err){}
                }
            }
        }
        CCommand removeCommand = new CCommand(C_REMOVE, key,null,this.getAgentID());
        addJurnalForMulticast(removeCommand);
        send(removeCommand, MULTICAST);
        return map.remove(key);
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        map.putAll(m);
    }

    @Override
    public void clear() {
        map.clear();
    }

    @Override
    public Set<K> keySet() {
        return map.keySet();
    }

    @Override
    public Collection<V> values() {
        return map.values();
    }

    @Override
    public Set<java.util.Map.Entry<K, V>> entrySet() {
        return map.entrySet();
    }
}
