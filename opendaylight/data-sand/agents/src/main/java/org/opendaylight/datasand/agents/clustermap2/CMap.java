package org.opendaylight.datasand.agents.clustermap2;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.opendaylight.datasand.agents.AutonomousAgent;
import org.opendaylight.datasand.agents.AutonomousAgentManager;
import org.opendaylight.datasand.agents.clustermap1.ClusterMapClassCodes;
import org.opendaylight.datasand.codec.bytearray.ByteEncoder;
import org.opendaylight.datasand.network.NetworkID;
import org.opendaylight.datasand.network.NetworkNodeConnection;
import org.opendaylight.datasand.network.Packet;

public class CMap<K, V> extends AutonomousAgent implements Map<K, V> {

    public static int CMAP_MULTICAST_GROUP_ID = 227;
    public static final int P_ARP_BROADCAST = 10;
    public static final int P_PUT           = 11;
    public static final int P_REMOVE        = 12;
    public static final int P_ACK           = 13;
    public static final int P_CLEAR         = 14;
    static {
        ByteEncoder.registerSerializer(CMCommand.class, new CMCommand(), ClusterMapClassCodes.CODE_CMCommand);
    }

    public static NetworkID MULTICAST = new NetworkID(
            NetworkNodeConnection.PROTOCOL_ID_BROADCAST.getIPv4Address(),
            CMAP_MULTICAST_GROUP_ID, CMAP_MULTICAST_GROUP_ID);

    private ICMapListener<K,V>listener = null;
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
    }

    public static class CMDEntry {
        private CMCommand cmd = null;
        private Set<NetworkID> expectedReplyFrom = new HashSet<NetworkID>();
        private long insertTime = -1;
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
        this.addRepetitiveFrame(arpSend, 15000, 0, 0);
        Packet timeoutMonitor = new Packet(timeoutTaskIdentifier,this.getAgentID(), this.getAgentID());
        this.addRepetitiveFrame(timeoutMonitor, 10000, 0, 0);
        this.sendARPBroadcast();
    }

    public void sendARPBroadcast(){
        send(new CMCommand(P_ARP_BROADCAST, this.localMap.size(), null, this.nextChangeID-1), MULTICAST);
    }

    public void sendAcknowledge(CMCommand cmd,NetworkID source){
        send(new CMCommand(P_ACK, null, null,cmd.getChangeNumber()), source);
    }

    public boolean isSynchronizing(){
        return this.synchronizing;
    }

    @Override
    public void processNext(Packet frame, Object obj) {
        if(obj==arpTaskIdentifier){
            sendARPBroadcast();
        }
        if (!frame.getSource().equals(this.getAgentID()) && obj instanceof CMCommand) {
            if(frame.getSource().getSubSystemID()==NetworkNodeConnection.DESTINATION_UNREACHABLE){
                processCommand((CMCommand) obj, frame.getSource(),frame.getUnreachableOrigAddress());
            }else
                processCommand((CMCommand) obj, frame.getSource(),null);
        }
    }

    private void addJurnalForMulticast(CMCommand command){
        if(this.peers.size()>0){
            CMDEntry entry = new CMDEntry();
            entry.cmd = command;
            journal.put(entry.cmd.getChangeNumber(), entry);
            entry.expectedReplyFrom.addAll(this.peers.keySet());
            entry.insertTime = System.currentTimeMillis();
        }
    }

    private void receiveAcknowledge(CMCommand cmd,NetworkID source){
        CMDEntry entry = journal.get(cmd.getChangeNumber());
        if(entry!=null){
            entry.expectedReplyFrom.remove(source);
            if(entry.expectedReplyFrom.isEmpty()){
                journal.remove(cmd.getChangeNumber());
            }
        }
    }

    private void processCommand(CMCommand cmd, NetworkID source,NetworkID unreachableOriginalSource){
        boolean isUnreachable = source.equals(NetworkNodeConnection.PROTOCOL_ID_UNREACHABLE);
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
            case P_ARP_BROADCAST:
                if(!isUnreachable){
                    //update peer data
                    peerEntry.lastReceivedPing = System.currentTimeMillis();
                    if(peerEntry.lastChangeNumber<cmd.getChangeNumber()){
                        peerEntry.lastChangeNumber = cmd.getChangeNumber();
                        peerEntry.mapSize = (Integer)cmd.getKey();
                    }else{
                        //peer was re-loaded
                    }
                }
                break;
            case P_PUT:
                if(!isUnreachable){
                    if(peerEntry.lastChangeNumber==cmd.getChangeNumber()-1){
                        peerEntry.map.put((K) cmd.getKey(), (V) cmd.getValue());
                        peerEntry.lastReceivedPing = System.currentTimeMillis();
                        peerEntry.lastChangeNumber = cmd.getChangeNumber();
                        sendAcknowledge(cmd, source);
                    }
                }
                break;
            case P_REMOVE:
                if(!isUnreachable){
                    if(peerEntry.lastChangeNumber==cmd.getChangeNumber()-1){
                        peerEntry.map.remove((K) cmd.getKey());
                        peerEntry.lastReceivedPing = System.currentTimeMillis();
                        peerEntry.lastChangeNumber = cmd.getChangeNumber();
                        sendAcknowledge(cmd, source);
                    }
                }
                break;
            case P_ACK:
                receiveAcknowledge(cmd, source);
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
        }
        CMCommand putCommand = new CMCommand(P_PUT, key, value,id);
        addJurnalForMulticast(putCommand);
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
        CMCommand removeCommand = new CMCommand(P_REMOVE, key,null,id);
        addJurnalForMulticast(removeCommand);
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
