package org.opendaylight.datasand.store;

import java.util.ArrayList;
import java.util.List;

import org.opendaylight.datasand.store.bytearray.ByteArrayObjectDataStore;

public class ServerPOJOTest {
    public static void main(String args[]){
        ByteArrayObjectDataStore stores[] = new ByteArrayObjectDataStore[5];
        int recCountPerStore = 10000;
        for(int j=0;j<stores.length;j++){
            stores[j] = new ByteArrayObjectDataStore("POJODB-"+j,true);
            for(int i=0;i<recCountPerStore;i++){
                Object pojo = buildPojo(j*recCountPerStore+i);
                stores[j].write(pojo, -1);
            }
            stores[j].commit();
        }
    }
    public static PojoObject buildPojo(int pojoIndex){
        PojoObject obj = new PojoObject();
        obj.setTestIndex(pojoIndex);
        obj.setTestString("Name-"+pojoIndex);
        obj.setTestBoolean(true);
        obj.setTestLong(12345678L);
        obj.setTestShort((short)44.44);
        SubPojoObject sp = new SubPojoObject();
        obj.setSubPojo(sp);
        SubPojoList l1 = new SubPojoList();
        l1.setName("Item #"+pojoIndex+":1");
        SubPojoList l2 = new SubPojoList();
        l2.setName("Item #"+pojoIndex+":2");
        List<SubPojoList> list = new ArrayList<>();
        list.add(l1);
        list.add(l2);
        obj.setList(list);
        return obj;
    }
}
