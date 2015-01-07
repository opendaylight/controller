package org.opendaylight.datasand.agents.cmap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.opendaylight.datasand.agents.AutonomousAgentManager;
import org.opendaylight.datasand.agents.cnode.CNode;
import org.opendaylight.datasand.agents.cnode.CNodeCommand;
import org.opendaylight.datasand.agents.cnode.CPeerEntry;
import org.opendaylight.datasand.codec.bytearray.ByteEncoder;
import org.opendaylight.datasand.network.NetworkID;


public class CMap<K,V> extends CNode<Map<K,V>,CMapEntry<K,V>> implements Map<K,V>{

    private static final int MAP_ENTRY_CLASS_CODE = 240;
    private static final int PUT    = 220;
    private static final int REMOVE = 225;
    private int size = 0;
    private ICMapListener<K, V> listener = null;
    static{
        ByteEncoder.registerSerializer(CMapEntry.class, new CMapEntry(), MAP_ENTRY_CLASS_CODE);
    }
    public CMap(int subSystemID,AutonomousAgentManager m, int multicastGroupID,ICMapListener<K, V> _listener){
        super(subSystemID,m,multicastGroupID);
        this.listener = _listener;
        registerHandler(PUT, new PutHandler<K,V>());
        registerHandler(REMOVE, new RemoveHandler<K,V>());
    }
    public void increaseSize(){size++;}
    public void decreaseSize(){size--;}
    public ICMapListener<K, V> getListener(){return this.listener;}
    @Override
    public Map<K, V> createDataTypeInstance() {
        return new HashMap<K,V>();
    }

    @Override
    public Collection<CMapEntry<K,V>> getDataTypeElementCollection(Map<K, V> data) {
        List<CMapEntry<K,V>> list = new LinkedList<CMapEntry<K,V>>();
        for(Map.Entry<K, V> e:data.entrySet()){
            list.add(new CMapEntry<K,V>(e.getKey(),e.getValue()));
        }
        return list;
    }

    @Override
    public boolean isLocalPeerCopyContainData(Map<K, V> data) {
        return !data.isEmpty();
    }

    @Override
    public void handleNodeOriginalData(CMapEntry<K,V> ce) {
        if(!this.containsKey(ce.getKey())){
            increaseSize();
        }
        this.getLocalData().put(ce.getKey(), ce.getValue());
        if(listener!=null){
            listener.peerPut(ce.getKey(), ce.getValue());
        }
    }

    @Override
    public void handlePeerSyncData(CMapEntry<K,V> ce,NetworkID source) {
        CPeerEntry<Map<K, V>> peerEntry = getPeerEntry(source);
        if(!this.containsKey(ce.getKey())){
            increaseSize();
        }
        peerEntry.getPeerData().put((K)ce.getKey(), (V)ce.getValue());
        if(listener!=null){
            listener.peerPut(ce.getKey(), ce.getValue());
        }
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
        throw new IllegalStateException("This method is not supported yet");
    }

    @Override
    public Set<java.util.Map.Entry<K, V>> entrySet() {
        Set<K> keySet = this.keySet();
        Set<Map.Entry<K, V>> eSet = new HashSet<Map.Entry<K,V>>();
        for(K key:keySet){
            eSet.add(new CMapEntry<K, V>(key,this.get(key)));
        }
        return eSet;
    }

    @Override
    public V get(Object key) {
        for(NetworkID id:getSortedList()){
            CPeerEntry<Map<K, V>> entry = getPeerEntry(id);
            if(entry!=null){
                V o = entry.getPeerData().get(key);
                if(o!=null){
                    return o;
                }
            }else{
                V o = getLocalData().get(key);
                if(o!=null){
                    return o;
                }
            }
        }
        return null;
    }

    @Override
    public boolean isEmpty() {
        if(!this.getLocalData().isEmpty())
            return false;
        for(NetworkID id:this.getSortedList()){
            CPeerEntry<Map<K,V>> peerEntry = getPeerEntry(id);
            if(!peerEntry.getPeerData().isEmpty())
                return false;
        }
        return true;
    }

    @Override
    public Set<K> keySet() {
        Set<K> result = new HashSet<K>();
        result.addAll(this.getLocalData().keySet());
        for(NetworkID id:this.getSortedList()){
            CPeerEntry<Map<K,V>> peerEntry = getPeerEntry(id);
            if(peerEntry!=null){
                result.addAll(peerEntry.getPeerData().keySet());
            }
        }
        return result;
    }

    @Override
    public V put(K key, V value) {
        long id = -1;
        synchronized(this){
            id = this.incrementID();
            if(this.isSynchronizing()){
                System.out.println("Waiting...");
                try{this.wait();}catch(Exception err){}
                System.out.println("End Waiting...");
            }
        }
        CNodeCommand<CMapEntry<K, V>> putCommand = new CNodeCommand<CMapEntry<K, V>>(id,PUT,new CMapEntry<K,V>(key, value));
        addJournalForMulticast(putCommand);
        multicast(putCommand);
        if(!this.containsKey(key)){
            increaseSize();
        }
        return this.getLocalData().put(key, value);
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        // TODO Auto-generated method stub
    }

    @Override
    public V remove(Object key) {
        long id = -1;
        synchronized(this){
            id = this.incrementID();
        }
        CNodeCommand<CMapEntry<K, V>> removeCommand = new CNodeCommand<CMapEntry<K, V>>(id,REMOVE,new CMapEntry<K,V>((K)key,null));
        addJournalForMulticast(removeCommand);
        multicast(removeCommand);
        V result = this.getLocalData().remove(key);
        if(result!=null && !this.containsKey(key)){
            decreaseSize();
        }
        return result;
    }

    @Override
    public int size() {
        return this.size;
    }

    @Override
    public Collection<V> values() {
        Set<Map.Entry<K, V>> entrySet = this.entrySet();
        List<V> list = new ArrayList<V>(entrySet.size());
        for(Map.Entry<K, V> e:entrySet){
            list.add(e.getValue());
        }
        return list;
    }
}
