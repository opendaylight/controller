package org.opendaylight.datasand.tests;


import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.datasand.codec.AttributeDescriptor;
import org.opendaylight.datasand.codec.TypeDescriptor;
import org.opendaylight.datasand.codec.TypeDescriptorsContainer;
import org.opendaylight.datasand.codec.bytearray.ByteArrayEncodeDataContainer;
import org.opendaylight.datasand.codec.json.JsonEncodeDataContainer;
import org.opendaylight.datasand.codec.xml.XMLEncodeDataContainer;
import org.opendaylight.datasand.store.ObjectDataStore;
import org.opendaylight.datasand.store.bytearray.ByteArrayObjectDataStore;

public class POJODBTest {
    private ObjectDataStore database = null;
    private long startTime = 0L;
    private long endTime = 0L;
    private static boolean createTestResources = false;

    @Before
    public void setupFlagsAndCreateDB() {
        AttributeDescriptor.IS_SERVER_SIDE = true;
        TypeDescriptor.REGENERATE_SERIALIZERS = true;
        database = new ByteArrayObjectDataStore("POJOStoreTest",true);
    }
    @After
    public void closeDBAndDeleteIT(){
        if(database!=null){
            database.close();
            database.deleteDatabase();
            database = null;
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
        l1.setName("Item #1");
        SubPojoList l2 = new SubPojoList();
        l2.setName("Item #2");
        List<SubPojoList> list = new ArrayList<>();
        list.add(l1);
        list.add(l2);
        obj.setList(list);
        return obj;
    }

    @Test
    public void testPojoSerialization(){
        PojoObject before = buildPojo(123);
        ByteArrayEncodeDataContainer dc = new ByteArrayEncodeDataContainer(1024,database.getTypeDescriptorsContainer().getTypeDescriptorByObject(before));
        dc.getEncoder().encodeObject(before, dc, PojoObject.class);
        byte[] data = dc.getBytes();
        dc = new ByteArrayEncodeDataContainer(data,database.getTypeDescriptorsContainer().getTypeDescriptorByObject(before));
        PojoObject after = (PojoObject)dc.getEncoder().decodeObject(dc);
        Assert.assertEquals(true, isEqual(before,after,database.getTypeDescriptorsContainer()));
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
            Assert.assertEquals(true, isEqual(pojos.get(i),after,database.getTypeDescriptorsContainer()));
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
        database = new ByteArrayObjectDataStore("POJOStoreTest",true);
        for(int i=0;i<pojos.size();i++){
            PojoObject after = (PojoObject)database.read(PojoObject.class, i);
            Assert.assertEquals(true, isEqual(pojos.get(i),after,database.getTypeDescriptorsContainer()));
        }
    }

    public static boolean isEqual(Object o1,Object o2,TypeDescriptorsContainer container){
        TypeDescriptor td1 = container.getTypeDescriptorByObject(o1);
        TypeDescriptor td2 = container.getTypeDescriptorByObject(o2);
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
            if(container.hasTypeDescriptor(v1)){
                if(!isEqual(v1, v2,container))
                    return false;
            }else
            if(!v1.equals(v2))
                return false;
        }
        return true;
    }

    public static void main(String args[]){
        {
        TypeDescriptor.REGENERATE_SERIALIZERS = false;
        TypeDescriptorsContainer container = new TypeDescriptorsContainer("./JSONTest");
        PojoObject obj = buildPojo(254);
        TypeDescriptor td = container.getTypeDescriptorByObject(obj);
        JsonEncodeDataContainer json = new JsonEncodeDataContainer(td);
        json.getEncoder().encodeObject(obj, json);
        System.out.println(json.toJSON(0));
        }
        {
        TypeDescriptorsContainer container = new TypeDescriptorsContainer("./XMLTest");
        PojoObject obj = buildPojo(254);
        TypeDescriptor td = container.getTypeDescriptorByObject(obj);
        XMLEncodeDataContainer xml = new XMLEncodeDataContainer(td);
        xml.getEncoder().encodeObject(obj, xml);
        System.out.println(xml.toXML(0));
        }
    }
}
