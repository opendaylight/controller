package org.opendaylight.persisted.autoagents;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.opendaylight.persisted.codec.EncodeDataContainer;
import org.opendaylight.persisted.codec.EncodeUtils;
import org.opendaylight.persisted.codec.ISerializer;
import org.opendaylight.persisted.net.NetworkID;
import org.opendaylight.persisted.net.NetworkNodeConnection;
import org.opendaylight.persisted.net.Packet;

public class ClusterMap<K, V> extends AutonomousAgent implements Map<K, V> {

    private Map<K, V> map = new ConcurrentHashMap<K, V>();
    public static int CLUSTER_MAP_MULTICAST_GROUP_ID = 223;
    public static NetworkID MULTICAST = new NetworkID(
            NetworkNodeConnection.PROTOCOL_ID_BROADCAST.getIPv4Address(),
            CLUSTER_MAP_MULTICAST_GROUP_ID, CLUSTER_MAP_MULTICAST_GROUP_ID);

    public ClusterMap(int subSystemId, NetworkID localhost,AutonomousAgentManager m) {
        super(subSystemId, localhost, m);
        m.registerForMulticast(CLUSTER_MAP_MULTICAST_GROUP_ID, this);
    }

    private static int nextID = 1000;
    private static final int C_PUT = 1;
    private static final int C_CONFIRM_OPERATION = 99;
    static {
        EncodeUtils.registerSerializer(CCommand.class, new CCommand(), 30);
    }

    public static class CCommand implements ISerializer {
        private long changeID = -1;
        private int op = -1;
        private Object key = null;
        private Object value = null;

        public CCommand() {
        }

        public CCommand(int _op, Object _key, Object _value) {
            this.op = _op;
            this.key = _key;
            this.value = _value;
            synchronized (CCommand.class) {
                nextID++;
                this.changeID = nextID;
            }
        }

        @Override
        public void encode(Object value, byte[] byteArray, int location) {

        }

        @Override
        public void encode(Object value, EncodeDataContainer ba) {
            CCommand c = (CCommand) value;
            EncodeUtils.encodeInt64(c.changeID, ba);
            EncodeUtils.encodeInt32(c.op, ba);
            if (c.key != null)
                EncodeUtils.encodeObject(c.key, ba, c.key.getClass());
            else
                EncodeUtils.encodeObject(c.key, ba, String.class);
            if (c.value != null)
                EncodeUtils.encodeObject(c.value, ba, c.value.getClass());
            else
                EncodeUtils.encodeObject(c.value, ba, String.class);
        }

        @Override
        public Object decode(byte[] byteArray, int location, int length) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Object decode(EncodeDataContainer ba, int length) {
            CCommand c = new CCommand();
            c.changeID = EncodeUtils.decodeInt64(ba);
            c.op = EncodeUtils.decodeInt32(ba);
            c.key = EncodeUtils.decodeObject(ba);
            c.value = EncodeUtils.decodeObject(ba);
            return c;
        }

        @Override
        public String getShardName(Object obj) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Object getRecordKey(Object obj) {
            // TODO Auto-generated method stub
            return null;
        }

    }

    @Override
    public void processNext(Packet frame, Object obj) {
        if (obj instanceof ClusterMap.CCommand) {
            processCCommand((ClusterMap.CCommand) obj, frame.getSource());
        }
    }

    private void processCCommand(CCommand cmd, NetworkID source) {
        switch (cmd.op) {
        case C_PUT:
            map.put((K) cmd.key, (V) cmd.value);
            CCommand reply = new CCommand(C_CONFIRM_OPERATION, null, null);
            send(reply, source);
            break;
        case C_CONFIRM_OPERATION:
            break;
        }

    }

    @Override
    public void start() {
        // TODO Auto-generated method stub

    }

    @Override
    public String getName() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int size() {
        // TODO Auto-generated method stub
        return 0;
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
        CCommand c = new CCommand(C_PUT, key, value);
        send(c, MULTICAST);
        return map.put(key, value);
    }

    @Override
    public V remove(Object key) {
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
