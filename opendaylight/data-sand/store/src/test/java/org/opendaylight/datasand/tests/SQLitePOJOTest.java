package org.opendaylight.datasand.tests;

import org.opendaylight.datasand.store.PojoObject;
import org.opendaylight.datasand.store.SubPojoObject;
import org.opendaylight.datasand.store.sqlite.SQLiteObjectStore;

public class SQLitePOJOTest {

    public static PojoObject buildPojo(int pojoIndex){
        PojoObject obj = new PojoObject();
        obj.setTestIndex(pojoIndex);
        obj.setTestString("Name-"+pojoIndex);
        obj.setTestBoolean(true);
        obj.setTestLong(12345678L);
        obj.setTestShort((short)44.44);
        SubPojoObject sp = new SubPojoObject();
        obj.setSubPojo(sp);
        return obj;
    }

    public static void main(String args[]){
        SQLiteObjectStore db = new SQLiteObjectStore();
        PojoObject obj = buildPojo(0);
        db.write(obj, -1);
    }
}
