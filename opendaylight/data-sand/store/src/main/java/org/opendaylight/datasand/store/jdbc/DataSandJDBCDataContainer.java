package org.opendaylight.datasand.store.jdbc;

import java.util.HashMap;
import java.util.Map;

import org.opendaylight.datasand.codec.EncodeDataContainer;
import org.opendaylight.datasand.codec.ISerializer;
import org.opendaylight.datasand.codec.TypeDescriptorsContainer;
import org.opendaylight.datasand.network.NetworkID;
import org.opendaylight.datasand.store.jdbc.DataSandJDBCResultSet.RSID;

public class DataSandJDBCDataContainer implements ISerializer{
    private Object data = null;
    private RSID rsID = null;

    public DataSandJDBCDataContainer(){
    }

    public DataSandJDBCDataContainer(Object _data,RSID _rsID){
        this.data = _data;
        this.rsID = _rsID;
    }

    public Object getData() {
        return data;
    }

    public RSID getRsID() {
        return rsID;
    }

    @Override
    public void encode(Object value, byte[] byteArray, int location) {
    }

    @Override
    public void encode(Object value, EncodeDataContainer ba) {
        DataSandJDBCDataContainer dc = (DataSandJDBCDataContainer)value;
        if(dc.rsID!=null){
            ba.getEncoder().encodeInt32(dc.rsID.getAddress(), ba);
            ba.getEncoder().encodeInt64(dc.rsID.getTime(), ba);
            ba.getEncoder().encodeInt32(dc.rsID.getLocalID(), ba);
        }else
            ba.getEncoder().encodeNULL(ba);
        if(dc.data instanceof NetworkID){
            ba.getEncoder().encodeInt16(5, ba);
            ba.getEncoder().encodeObject(dc.data, ba);
        }else
        if(dc.data instanceof DataSandJDBCResultSet){
            ba.getEncoder().encodeInt16(1, ba);
            DataSandJDBCResultSet.encode((DataSandJDBCResultSet)dc.data, ba);
        }else
        if(dc.data instanceof TypeDescriptorsContainer){
            ba.getEncoder().encodeInt16(2, ba);
            byte[] data = ((TypeDescriptorsContainer)dc.data).getRepositoryData();
            ba.getEncoder().encodeByteArray(data,ba);
        }else
        if(dc.data instanceof Map){
            ba.getEncoder().encodeInt16(3, ba);
            Map m = (Map)dc.data;
            ba.getEncoder().encodeSize(m.size(),ba);
            for(Object o:m.entrySet()){
                Map.Entry e = (Map.Entry)o;
                ba.getEncoder().encodeObject(e.getKey(), ba);
                ba.getEncoder().encodeObject(e.getValue(), ba);
            }
        }else
        if(dc.data instanceof Exception){
            ba.getEncoder().encodeInt16(4, ba);
            Exception e = (Exception)dc.data;
            ba.getEncoder().encodeString(e.getMessage(),ba);
        }else{
            ba.getEncoder().encodeInt16(6, ba);
        }
    }

    @Override
    public Object decode(byte[] byteArray, int location, int length) {
        return null;
    }

    @Override
    public Object decode(EncodeDataContainer ba, int length) {
        DataSandJDBCDataContainer dc = new DataSandJDBCDataContainer();
        if(!ba.getEncoder().isNULL(ba)){
            dc.rsID = new RSID(ba.getEncoder().decodeInt32(ba),ba.getEncoder().decodeInt64(ba),ba.getEncoder().decodeInt32(ba));
        }
        int type = ba.getEncoder().decodeInt16(ba);
        if(type==1){
            dc.data = DataSandJDBCResultSet.decode(ba);
        }else
        if(type==2){
            byte data[] = ba.getEncoder().decodeByteArray(ba);
            TypeDescriptorsContainer tc = new TypeDescriptorsContainer("JDBC-Connection");
            tc.load(data);
            dc.data=tc;
        }else
        if(type==3){
            Map m = new HashMap();
            int size = ba.getEncoder().decodeSize(ba);
            for(int i=0;i<size;i++){
                Object key = ba.getEncoder().decodeObject(ba);
                Object value = ba.getEncoder().decodeObject(ba);
                m.put(key, value);
            }
            dc.data = m;
        }else
        if(type==4){
            String msg = ba.getEncoder().decodeString(ba);
            Exception e = new Exception(msg);
            dc.data = e;
        }else
        if(type==5){
            dc.data = ba.getEncoder().decodeObject(ba);
        }
        return dc;
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
