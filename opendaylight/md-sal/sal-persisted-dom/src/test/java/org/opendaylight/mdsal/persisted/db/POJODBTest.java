package org.opendaylight.mdsal.persisted.db;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.mdsal.persisted.db.testmodel.PojoObject;
import org.opendaylight.mdsal.persisted.db.testmodel.SubPojoObject;
import org.opendaylight.persisted.ObjectDataStore;
import org.opendaylight.persisted.codec.AttributeDescriptor;
import org.opendaylight.persisted.codec.EncodeDataContainer;
import org.opendaylight.persisted.codec.EncodeUtils;
import org.opendaylight.persisted.codec.TypeDesciptorRepository;
import org.opendaylight.persisted.codec.TypeDescriptor;

public class POJODBTest {
    private ObjectDataStore database = null;
    private long startTime = 0L;
    private long endTime = 0L;
    private static boolean createTestResources = false;

    @BeforeClass
    public static void cleanAll(){
        TypeDescriptor.resetObservers();
        ObjectDataStore.deleteDatabase();
    }

    @AfterClass
    public static void finishClean(){
        ObjectDataStore.deleteDatabase();
        TypeDesciptorRepository.getInstance().deleteRepository();
    }

    @Before
    public void setupFlagsAndCreateDB() {
        AttributeDescriptor.IS_SERVER_SIDE = true;
        ObjectDataStore.SHOULD_SORT_FIELDS = true;
        database = new ObjectDataStore();
    }
    @After
    public void closeDBAndDeleteIT(){
        if(database!=null){
            database.close();
            database = null;
        }
        ObjectDataStore.deleteDatabase();
    }

    public PojoObject buildPojo(int pojoIndex){
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

    @Test
    public void testPojoSerialization(){
        PojoObject before = buildPojo(123);
        EncodeDataContainer dc = new EncodeDataContainer(1024);
        EncodeUtils.encodeObject(before, dc, PojoObject.class);
        byte[] data = dc.getBytes();
        dc = new EncodeDataContainer(data);
        PojoObject after = (PojoObject)EncodeUtils.decodeObject(dc);
        Assert.assertEquals(true, isEqual(before,after));
    }

    @Test
    public void testPojoPersistency(){
        List<PojoObject> pojos = new ArrayList<PojoObject>(10000);
        for(int i=0;i<10000;i++){
            PojoObject before = buildPojo(123);
            database.write(before, i);
            pojos.add(before);
        }
        for(int i=0;i<pojos.size();i++){
            PojoObject after = (PojoObject)database.read(PojoObject.class, i);
            Assert.assertEquals(true, isEqual(pojos.get(i),after));
        }
    }

    @Test
    public void testPojoPersistencyCloseDB(){
        List<PojoObject> pojos = new ArrayList<PojoObject>(10000);
        for(int i=0;i<10000;i++){
            PojoObject before = buildPojo(123);
            database.write(before, i);
            pojos.add(before);
        }
        database.close();
        database = null;
        TypeDesciptorRepository.cleanInstance();
        database = new ObjectDataStore();
        for(int i=0;i<pojos.size();i++){
            PojoObject after = (PojoObject)database.read(PojoObject.class, i);
            Assert.assertEquals(true, isEqual(pojos.get(i),after));
        }
    }

    public static boolean isEqual(Object o1,Object o2){
        TypeDescriptor td1 = TypeDesciptorRepository.getInstance().getTypeDescriptorByObject(o1);
        TypeDescriptor td2 = TypeDesciptorRepository.getInstance().getTypeDescriptorByObject(o2);
        if(!td1.getTypeClass().equals(td2.getTypeClass()))
            return false;
        for(AttributeDescriptor ad:td1.getAttributes()){
            Object v1 = ad.get(o1, null, td1.getTypeClass());
            Object v2 = ad.get(o2, null, td1.getTypeClass());
            if(v1==null && v2==null)
                continue;
            if(v1==null && v2!=null)
                return false;
            if(v1!=null && v2==null)
                return false;
            if(TypeDesciptorRepository.getInstance().hasTypeDescriptor(v1)){
                if(!isEqual(v1, v2))
                    return false;
            }else
            if(!v1.equals(v2))
                return false;
        }
        return true;
    }
}
