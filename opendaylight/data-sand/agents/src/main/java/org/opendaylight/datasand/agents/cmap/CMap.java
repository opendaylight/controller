package org.opendaylight.datasand.agents.cmap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.opendaylight.datasand.agents.AutonomousAgent;
import org.opendaylight.datasand.agents.AutonomousAgentManager;
import org.opendaylight.datasand.codec.bytearray.ByteEncoder;
import org.opendaylight.datasand.network.NetworkID;
import org.opendaylight.datasand.network.NetworkNodeConnection;
import org.opendaylight.datasand.network.Packet;

public class CMap<K, V> extends AutonomousAgent implements Map<K, V> {

    public static int CMAP_MULTICAST_GROUP_ID = 227;
    public static final int P_NEW_NODE      = 9;
    public static final int P_ARP_BROADCAST = 10;
    public static final int P_PUT           = 11;
    public static final int P_REMOVE        = 12;
    public static final int P_ACK           = 13;
    public static final int P_CLEAR         = 14;
    public static final int P_SYNC_PUT      = 15;
    public static final int P_SET_CHANGE    = 17;
    public static final int P_SET_CHANGE_R  = 18;
    public static final int P_SYNC_MODE     = 19;
    public static final int P_SYNC_MODE_SEND= 20;
    public static final int P_SYNC_PUT_ORIG = 21;

    static {
        ByteEncoder.registerSerializer(RMICMD.class, new RMICMD(), CMapClassCodes.CODE_RMICMD);
    }

    public static NetworkID MULTICAST = new NetworkID(
            NetworkNodeConnection.PROTOCOL_ID_BROADCAST.getIPv4Address(),
            CMAP_MULTICAST_GROUP_ID, CMAP_MULTICAST_GROUP_ID);

    private ICMapListener<K,V> listener = null;
    private Object arpTaskIdentifier = new Object();
    private Object timeoutTaskIdentifier = new Object();
    private long nextChangeID = 1000;
    private Map<NetworkID,PeerEntry<K,V>> peers = new HashMap<NetworkID,PeerEntry<K,V>>();
    private Map<Long,CMDEntry> journal = new LinkedHashMap<Long, CMDEntry>();
    private Map<K,V> localMap = new HashMap<K, V>();
    private List<NetworkID> sortedID = new ArrayList<NetworkID>();
    private int size = 0;
    private boolean synchronizing = false;

    public static class PeerEntry<K,V> {
        public NetworkID netID = null;
        public long lastReceivedPing = -1;
        public long lastChangeNumber = 999;
        public int mapSize = -1;
        public Map<K,V> map = new HashMap<K, V>();
        boolean isUnreachable = false;
    }

    public static class CMDEntry {
        private RMICMD cmd = null;
        private Set<NetworkID> expectedReplyFrom = new HashSet<NetworkID>();
        private long insertTime = -1;
        public String toString(){
            StringBuffer buff = new StringBuffer();
            buff.append("OP=").append(this.cmd.getOperation()).append("\n");
            for(NetworkID id:expectedReplyFrom){
                buff.append(id).append("\n");
            }
            return buff.toString();
        }
    }

    public static class NetIDComparatr implements Comparator<NetworkID>{
        @Override
        public int compare(NetworkID o1, NetworkID o2) {
            if(o1==null && o2==null)
                return 0;
            if(o1==null && o2!=null)
                return 1;
            if(o1!=null && o2==null)
                return -1;
            if(o1.getIPv4Address()<o2.getIPv4Address())
                return -1;
            else
            if(o1.getIPv4Address()>o2.getIPv4Address())
                return 1;
            if(o1.getPort()<o2.getPort())
                return -1;
            else
            if(o1.getPort()>o2.getPort())
                return 1;
            if(o1.getSubSystemID()<o2.getSubSystemID())
                return -1;
            else
            if(o1.getSubSystemID()>o2.getSubSystemID())
                return 1;
            return 0;
        }
    }

    public CMap(int subSystemId, AutonomousAgentManager m,ICMapListener<K, V> _listener) {
        super(subSystemId, m);
        this.listener = _listener;
        this.sortedID.add(this.getAgentID());
        m.registerForMulticast(CMAP_MULTICAST_GROUP_ID, this);
        Packet arpSend = new Packet(arpTaskIdentifier,this.getAgentID(), this.getAgentID());
        this.addRepetitiveFrame(arpSend, 10000, 10000, 0);
        Packet timeoutMonitor = new Packet(timeoutTaskIdentifier,this.getAgentID(), this.getAgentID());
        this.addRepetitiveFrame(timeoutMonitor, 10000, 10000, 0);
        this.send(new RMICMD(P_NEW_NODE,null,null,nextChangeID), MULTICAST);
    }

    public void sendARPBroadcast(){
        send(new RMICMD(P_ARP_BROADCAST, this.localMap.size(), null, this.nextChangeID-1), MULTICAST);
    }

    public void sendAcknowledge(RMICMD cmd,NetworkID source){
        send(new RMICMD(P_ACK, null, null,cmd.getChangeNumber()), source);
    }

    public boolean isSynchronizing(){
        return this.synchronizing;
    }

    public void checkForTimeout(){
        for(Iterator<CMDEntry> iter=journal.values().iterator();iter.hasNext();){
            CMDEntry e = iter.next();
            if(System.currentTimeMillis()-e.insertTime>5000){
                System.out.println("timeout on "+e);
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
        if (!frame.getSource().equals(this.getAgentID()) && obj instanceof RMICMD) {
            if(frame.getSource().getSubSystemID()==NetworkNodeConnection.DESTINATION_UNREACHABLE){
                processCommand((RMICMD) obj, frame.getSource(),frame.getUnreachableOrigAddress());
            }else
                processCommand((RMICMD) obj, frame.getSource(),null);
        }
    }

    private void addJournalForMulticast(RMICMD command){
        if(this.peers.size()>0){
            CMDEntry entry = new CMDEntry();
            entry.cmd = command;
            boolean hasOnePeer = false;
            for(PeerEntry<K, V> e:this.peers.values()){
                if(!e.isUnreachable){
                    hasOnePeer=true;
                    entry.expectedReplyFrom.add(e.netID);
                }
            }
            entry.insertTime = System.currentTimeMillis();
            if(hasOnePeer)
                journal.put(entry.cmd.getChangeNumber(), entry);
        }
    }

    private void addJournalForUnicast(RMICMD command,NetworkID destination){
        if(this.peers.size()>0){
            CMDEntry entry = new CMDEntry();
            entry.cmd = command;
            journal.put(entry.cmd.getChangeNumber(), entry);
            entry.expectedReplyFrom.add(destination);
            entry.insertTime = System.currentTimeMillis();
        }
    }

    private void receiveAcknowledge(RMICMD cmd,NetworkID source){
        CMDEntry entry = journal.get(cmd.getChangeNumber());
        if(entry!=null){
            entry.expectedReplyFrom.remove(source);
            if(entry.expectedReplyFrom.isEmpty()){
                journal.remove(cmd.getChangeNumber());
            }
        }
    }

    private boolean handleUnreachable(NetworkID unreachableOriginalSource, RMICMD cmd){
        //if this is unreachable frame, try to remove the peer from the known peers
        for(Iterator<Map.Entry<NetworkID, PeerEntry<K,V>>> iter=peers.entrySet().iterator();iter.hasNext();){
            PeerEntry<K,V> entry = iter.next().getValue();
            if(entry.netID.getIPv4Address()==unreachableOriginalSource.getIPv4Address() && entry.netID.getPort()==unreachableOriginalSource.getPort()){
                //mark entry as unreachable
                entry.isUnreachable = true;
            }
        }
        //The node is down, remove it from the expected replys list, if exist
        CMDEntry entry = journal.get(cmd.getChangeNumber());
        if(entry!=null){
            for(Iterator<NetworkID> iter=entry.expectedReplyFrom.iterator();iter.hasNext();){
                NetworkID nid = iter.next();
                if(nid.getIPv4Address()==unreachableOriginalSource.getIPv4Address() && nid.getPort()==unreachableOriginalSource.getPort()){
                    iter.remove();
                }
            }
            //If this is the last reply, invoke the end confirmation
            if(entry.expectedReplyFrom.size()==0){
                this.receiveAcknowledge(cmd, unreachableOriginalSource);
                return true;
            }
        }
        return false;
    }

    private void handleOriginalData(RMICMD cmd,NetworkID source){
        if(!this.containsKey(cmd.getKey())){
            size++;
        }
        this.localMap.put((K)cmd.getKey(), (V)cmd.getValue());
        sendAcknowledge(cmd, source);
    }

    private void handleNewNodeInCluster(PeerEntry<K, V> peerEntry,NetworkID source){
        System.out.println("Enter Sync mode="+this.getAgentID());
        this.synchronizing = true;
        this.send(new RMICMD(P_SYNC_MODE,null,null,-1),source);
        int id=-1;
        //if this node container data from the source node, it means that the source node was down
        //and now it is up again so send it its original data
        if(peerEntry.map.size()>0){
            System.out.println("Found data for "+source+", sending it.");
            peerEntry.isUnreachable = false;
            for(Map.Entry<K, V> e:peerEntry.map.entrySet()){
                RMICMD putCommand = new RMICMD(P_SYNC_PUT_ORIG, e.getKey(), e.getValue(),id);
                addJournalForUnicast(putCommand, source);
                this.send(putCommand, source);
                id--;
            }
        }
        //Send it this node map data
        peerEntry.lastChangeNumber=1000;
        System.out.println("Initial Sending Data from "+this.getAgentID()+" to "+source);
        for(Map.Entry<K, V> e:this.localMap.entrySet()){
            RMICMD putCommand = new RMICMD(P_SYNC_PUT, e.getKey(), e.getValue(),id);
            addJournalForUnicast(putCommand, source);
            this.send(putCommand, source);
            id--;
        }
        //Remove any expected replys from this node as it is up.
        List<RMICMD> finished = new LinkedList<RMICMD>();
        for(CMDEntry e:this.journal.values()){
            e.expectedReplyFrom.remove(source);
            if(e.expectedReplyFrom.isEmpty()){
               finished.add(e.cmd);
            }
        }
        for(RMICMD c:finished){
            this.receiveAcknowledge(c, source);
        }
        //Update this node change id it the source node
        this.send(new RMICMD(P_SET_CHANGE,null,null,this.nextChangeID-1), source);
    }

    private void handleSyncModeSend(NetworkID source){
        System.out.println("Enter Syncmode="+this.getAgentID());
        this.synchronizing = true;
        for(CMDEntry e:journal.values()){
            if(e.cmd.getOperation()==P_PUT && e.expectedReplyFrom.contains(source)){
                this.send(e.cmd, source);
            }else
            if(e.cmd.getOperation()==P_REMOVE && e.expectedReplyFrom.contains(source)){
                this.send(e.cmd, source);
            }
        }
        this.send(new RMICMD(P_SET_CHANGE_R,null,null,this.nextChangeID-1), source);
        synchronized(this){
            System.out.println("Exiting Sync mode="+this.getAgentID());
            this.synchronizing = false;
            this.notifyAll();
        }
    }

    private void processCommand(RMICMD cmd, NetworkID source,NetworkID unreachableOriginalSource){
        boolean isUnreachable = source.equals(NetworkNodeConnection.PROTOCOL_ID_UNREACHABLE);
        //If this is unreachable and the handleUnreachable has removed the command journal, then do not continue.
        if(isUnreachable && handleUnreachable(unreachableOriginalSource, cmd))
            return;
        //Retrieve the peer entry
        PeerEntry<K,V> peerEntry = null;
        if(!isUnreachable){
            peerEntry = (PeerEntry<K,V>)peers.get(source);
            if(peerEntry==null){
                peerEntry = new PeerEntry<K,V>();
                peerEntry.netID = source;
                peers.put(source, peerEntry);
                sortedID.add(source);
                Collections.sort(sortedID, new NetIDComparatr());
            }
        }

        switch(cmd.getOperation()){
            case P_SYNC_PUT_ORIG:
                if(!isUnreachable){
                    //This is the original data before this node went down, just update the local map
                    handleOriginalData(cmd, source);
                }
                break;
            case P_NEW_NODE:
                if(!isUnreachable){
                    handleNewNodeInCluster(peerEntry, source);
                }
                break;
            case P_SYNC_MODE_SEND:
                if(!isUnreachable){
                    //One of the node adjacent requested all the data that is pending in this node journal for it.
                    handleSyncModeSend(source);
                }
                break;
            case P_SYNC_MODE:
                if(!isUnreachable){
                    System.out.println("Entering Syn cmode="+this.getAgentID());
                    this.synchronizing = true;
                }
                break;
            case P_SET_CHANGE_R:
                if(!isUnreachable){
                    peerEntry.lastChangeNumber = cmd.getChangeNumber();
                    synchronized(this){
                        System.out.println("Exiting Sync mode="+this.getAgentID());
                        synchronizing = false;
                        this.notifyAll();
                    }
                }
                break;
            case P_SET_CHANGE:
                if(!isUnreachable){
                    peerEntry.lastChangeNumber = cmd.getChangeNumber();
                    if(this.nextChangeID==1000)
                        this.nextChangeID++;
                    this.send(new RMICMD(P_SET_CHANGE_R,null,null,this.nextChangeID-1), source);
                    synchronized(this){
                        System.out.println("Existing Sync mode="+this.getAgentID());
                        this.synchronizing = false;
                        this.notifyAll();
                    }
                }
                break;
            case P_SYNC_PUT:
                if(!isUnreachable){
                    if(!this.containsKey(cmd.getKey())){
                        size++;
                    }
                    peerEntry.map.put((K) cmd.getKey(), (V) cmd.getValue());
                    sendAcknowledge(cmd, source);
                    if(listener!=null){
                        listener.peerPut((K) cmd.getKey(), (V) cmd.getValue());
                    }
                }
                break;
            case P_ARP_BROADCAST:
                if(!isUnreachable){
                    //update peer data
                    peerEntry.lastReceivedPing = System.currentTimeMillis();
                    if(peerEntry.lastChangeNumber>cmd.getChangeNumber() && cmd.getChangeNumber()!=999){
                        System.out.println("Peer Reload");
                    }
                    if(cmd.getChangeNumber()!=999){
                        if(peerEntry.lastChangeNumber!=cmd.getChangeNumber()){
                            System.out.println("Updating Data from "+this.getAgentID()+" to "+source);
                            this.synchronizing = true;
                            this.send(new RMICMD(P_SYNC_MODE_SEND,null,null,-1),source);
                            for(CMDEntry e:journal.values()){
                                if(e.cmd.getOperation()==P_PUT && e.expectedReplyFrom.contains(source)){
                                    this.send(e.cmd, source);
                                }else
                                if(e.cmd.getOperation()==P_REMOVE && e.expectedReplyFrom.contains(source)){
                                    this.send(e.cmd, source);
                                }
                            }
                            this.send(new RMICMD(P_SET_CHANGE,null,null,this.nextChangeID-1), source);
                        }else{
                            peerEntry.lastChangeNumber = cmd.getChangeNumber();
                            peerEntry.mapSize = (Integer)cmd.getKey();
                        }
                    }
                }
                break;
            case P_PUT:
                if(!isUnreachable){
                    if(!this.containsKey(cmd.getKey())){
                        size++;
                    }
                    peerEntry.map.put((K) cmd.getKey(), (V) cmd.getValue());
                    peerEntry.lastReceivedPing = System.currentTimeMillis();
                    peerEntry.lastChangeNumber = cmd.getChangeNumber();
                    sendAcknowledge(cmd, source);
                    if(listener!=null){
                        listener.peerPut((K) cmd.getKey(), (V) cmd.getValue());
                    }
                }
                break;
            case P_REMOVE:
                if(!isUnreachable){
                    Object o = peerEntry.map.remove((K) cmd.getKey());
                    if(o!=null && !this.containsKey(cmd.getKey())){
                        size--;
                    }
                    peerEntry.lastReceivedPing = System.currentTimeMillis();
                    peerEntry.lastChangeNumber = cmd.getChangeNumber();
                    sendAcknowledge(cmd, source);
                    if(listener!=null){
                        listener.peerRemove((K) cmd.getKey());
                    }
                }
                break;
            case P_ACK:
                if(!isUnreachable){
                    receiveAcknowledge(cmd, source);
                }
                break;
        }
    }

    @Override
    public void start() {
        // TODO Auto-generated method stub
    }

    @Override
    public String getName() {
        return "Cluster Map "+this.getAgentID();
    }

    @Override
    public void clear() {

    }

    @Override
    public boolean containsKey(Object key) {
        return this.get(key)!=null;
    }

    @Override
    public boolean containsValue(Object value) {
        throw new UnsupportedOperationException("Not suppoorted yet");
    }

    @Override
    public Set<java.util.Map.Entry<K, V>> entrySet() {
        throw new UnsupportedOperationException("Not suppoorted yet");
    }

    @Override
    public V get(Object key) {
        for(NetworkID id:sortedID){
            PeerEntry<K, V> entry = peers.get(id);
            if(entry!=null){
                V o = entry.map.get(key);
                if(o!=null){
                    return o;
                }
            }else{
                V o = localMap.get(key);
                if(o!=null){
                    return o;
                }
            }
        }
        return null;
    }

    @Override
    public boolean isEmpty() {
        if(!this.localMap.isEmpty()){
            return false;
        }
        for(PeerEntry<K, V> entry:peers.values()){
            if(!entry.map.isEmpty())
                return false;
        }
        return true;
    }

    @Override
    public Set<K> keySet() {
        Set<K> result = new HashSet<K>();
        result.addAll(this.localMap.keySet());
        for(PeerEntry<K, V> e:peers.values()){
            result.addAll(e.map.keySet());
        }
        return null;
    }

    @Override
    public V put(K key, V value) {
        long id = -1;
        synchronized(this){
            id = nextChangeID;
            nextChangeID++;
            if(synchronizing){
                System.out.println("Waiting...");
                try{this.wait();}catch(Exception err){}
                System.out.println("End Waiting...");
            }
        }
        RMICMD putCommand = new RMICMD(P_PUT, key, value,id);
        addJournalForMulticast(putCommand);
        send(putCommand, MULTICAST);
        if(!this.containsKey(key)){
            size++;
        }
        return this.localMap.put(key, value);
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        for(Map.Entry<? extends K,? extends V> e:m.entrySet()){
            this.put(e.getKey(), e.getValue());
        }
    }

    @Override
    public V remove(Object key) {
        long id = -1;
        synchronized(this){
            id = nextChangeID;
            nextChangeID++;
        }
        RMICMD removeCommand = new RMICMD(P_REMOVE, key,null,id);
        addJournalForMulticast(removeCommand);
        send(removeCommand, MULTICAST);
        V result = this.localMap.remove(key);
        if(result!=null && !this.containsKey(key)){
            size--;
        }
        return result;
    }

    @Override
    public int size() {
        return this.size;
    }

    @Override
    public Collection<V> values() {
        // TODO Auto-generated method stub
        return null;
    }
}
