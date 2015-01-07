package org.opendaylight.datasand.store;

import org.opendaylight.datasand.codec.EncodeDataContainer;
import org.opendaylight.datasand.codec.TypeDescriptorsContainer;

public abstract class DataPersister {
    protected Shard shard = null;
    protected Class<?> type = null;
    protected TypeDescriptorsContainer container = null;

    public DataPersister(Shard _shard, Class<?> _type,TypeDescriptorsContainer _container) {
        this.container = _container;
        this.shard = _shard;
        this.type = _type;
    }

    public abstract void save();
    public abstract void load();
    public abstract int getObjectCount();
    public abstract boolean contain(Object key);
    public abstract boolean isDeleted(Object data);
    public abstract int write(int parentRecordIndex,int recordIndex,EncodeDataContainer ba);
    public abstract Object delete(Object key);
    public abstract Object delete(int index);
    public abstract Object[] delete(int recordIndexs[]);
    public abstract Object read(Object key);
    public abstract Object read(int index);
    public abstract Object[] read(int recordIndexs[]);
    public abstract Integer getParentIndex(int index);
    public abstract Integer getIndexByKey(Object key);
    public abstract Integer getParentIndexByKey(Object key);
    public abstract int[] getRecordIndexesByParentIndex(int parentRecordIndex);
    public abstract void close();
}