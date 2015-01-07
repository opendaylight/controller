package org.opendaylight.mdsal.persisted.db;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.datasand.codec.AttributeDescriptor;
import org.opendaylight.datasand.codec.EncodeDataContainer;
import org.opendaylight.datasand.codec.ThreadPool;
import org.opendaylight.datasand.codec.TypeDescriptor;
import org.opendaylight.datasand.codec.bytearray.ByteArrayEncodeDataContainer;
import org.opendaylight.datasand.codec.json.JsonEncodeDataContainer;
import org.opendaylight.datasand.codec.xml.XMLEncodeDataContainer;
import org.opendaylight.datasand.store.ObjectDataStore;
import org.opendaylight.datasand.store.bytearray.ByteArrayObjectDataStore;
import org.opendaylight.persisted.mdsal.MDSALClassExtractor;
import org.opendaylight.persisted.mdsal.MDSALMethodFilter;
import org.opendaylight.persisted.mdsal.MDSALObjectTypeRule;
import org.opendaylight.persisted.mdsal.MDSalAugmentationObserver;
import org.opendaylight.persisted.mdsal.MDSalObjectChildRule;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.persistedtest.rev141020.MyEnumType;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.persistedtest.rev141020.MyType;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.persistedtest.rev141020.SalPersistedDomTest;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.persistedtest.rev141020.SalPersistedDomTest1;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.persistedtest.rev141020.SalPersistedDomTest1Builder;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.persistedtest.rev141020.SalPersistedDomTestBuilder;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.persistedtest.rev141020.TypedefBits;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.persistedtest.rev141020.sal.persisted.dom.test.AugmentSubList;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.persistedtest.rev141020.sal.persisted.dom.test.AugmentSubListBuilder;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.persistedtest.rev141020.sal.persisted.dom.test.ListWithKey;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.persistedtest.rev141020.sal.persisted.dom.test.ListWithKeyBuilder;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.persistedtest.rev141020.sal.persisted.dom.test.ListWithKeyKey;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.persistedtest.rev141020.sal.persisted.dom.test.NoKeysubList;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.persistedtest.rev141020.sal.persisted.dom.test.NoKeysubListBuilder;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.persistedtest.rev141020.sal.persisted.dom.test.SalPersistedSubContainerBuilder;

public class MDSALDBTest {

    private ObjectDataStore database = null;
    private long startTime = 0L;
    private long endTime = 0L;
    private static boolean createTestResources = false;

    public static void main(String args[]){

        MDSALDBTest test = new MDSALDBTest();
        test.setupFlagsAndCreateDB();

        SalPersistedDomTest obj = buildTestElement("Updated Element",500,true,true,true,true,true,4);
        TypeDescriptor td = test.database.getTypeDescriptorsContainer().getTypeDescriptorByObject(obj);

        JsonEncodeDataContainer json = new JsonEncodeDataContainer(td);
        json.getEncoder().encodeObject(obj, json);
        System.out.println(json.toJSON(0));

        XMLEncodeDataContainer xml = new XMLEncodeDataContainer(td);
        xml.getEncoder().encodeObject(obj, xml);
        System.out.println(xml.toXML(0));

        //test.testPerformance500000();
        //test.testSaveAndLoadDatabase();
        //test.testThreads();
        //test.testwritereadwriteread();
        System.gc();
        try{Thread.sleep(1000);}catch(Exception err){}
        System.out.println("final memory="+getUsedMemory());
        test.closeDBAndDeleteIT();
    }

    @Before
    public void setupFlagsAndCreateDB() {
        AttributeDescriptor.IS_SERVER_SIDE = true;
        TypeDescriptor.REGENERATE_SERIALIZERS = true;
        database = new ByteArrayObjectDataStore("MDSalDataStoreTest",true);
        setupObservers();
    }

    public void setupObservers(){
        database.getTypeDescriptorsContainer().clearChildAttributeObservers();
        database.getTypeDescriptorsContainer().addChildAttributeObserver(new MDSalObjectChildRule());
        database.getTypeDescriptorsContainer().addTypeAttributeObserver(new MDSALObjectTypeRule());
        database.getTypeDescriptorsContainer().setClassExtractor(new MDSALClassExtractor());
        database.getTypeDescriptorsContainer().addMethodFilterObserver(new MDSALMethodFilter());
        database.getTypeDescriptorsContainer().setAugmentationObserver(new MDSalAugmentationObserver());
    }

    @After
    public void closeDBAndDeleteIT(){
        if(database!=null){
            database.close();
            database.deleteDatabase();
        }
    }

    private static SalPersistedDomTest buildTestElement(int elementID, boolean addAugmentation,boolean subContainer,boolean subList,boolean subListAugmentation,boolean includeBigDecimal){
        return buildTestElement(null, elementID, addAugmentation, subContainer, subList, subListAugmentation, includeBigDecimal,2);
    }

    private static SalPersistedDomTest buildTestElement(String name,int elementID, boolean addAugmentation,boolean subContainer,boolean subList,boolean subListAugmentation,boolean includeBigDecimal,int numOfSubElements){
        //Seems like BigDecimal conversion is extremel slow so on some of the tests i wish to disable it.
        SalPersistedDomTestBuilder b = new SalPersistedDomTestBuilder();
        if(name==null)
            b.setMainString("Test String " + elementID);
        else
            b.setMainString(name);
        MyType mytype = new MyType("Test Typedef "+elementID);
        b.setTypedeftest(mytype);
        b.setEnumtest(MyEnumType.TestEnum2);
        b.setTestInt8(new Byte((byte)253));
        b.setTestInt16(new Short((short)456));
        b.setTestInt32(Integer.MAX_VALUE);
        b.setTestInt64(Long.MIN_VALUE);
        b.setCountID(elementID);
        if(includeBigDecimal)
            b.setTestDecimal64(new BigDecimal((double)12345.4321));
        b.setTestBinary(new byte[]{(byte)5,(byte)129,(byte)33});
        b.setBitsTest(new TypedefBits(true, false));
        //b.setTestUnion(new TypedefUnion("Union Test-"+elementID));
        if(addAugmentation){
            SalPersistedDomTest1Builder b1 = new SalPersistedDomTest1Builder();
            b1.setMainString2("Augmentation "+elementID);
            MyType amytype = new MyType("Aug Typedef "+elementID);
            b1.setAtypedeftest(amytype);
            b1.setAenumtest(MyEnumType.TestEnum2);
            b1.setAtestInt8(new Byte((byte)253));
            b1.setAtestInt16(new Short((short)456));
            b1.setAtestInt32(Integer.MAX_VALUE);
            b1.setAtestInt64(Long.MIN_VALUE);
            if(includeBigDecimal)
                b1.setAtestDecimal64(new BigDecimal((double)12345.4321));
            b1.setAtestBinary(new byte[]{(byte)5,(byte)129,(byte)33});
            b1.setAbitsTest(new TypedefBits(true, false));

            if(subListAugmentation){
                AugmentSubListBuilder b2 = new AugmentSubListBuilder();
                b2.setSubAugName("Sub Augment E1-"+elementID);
                List<AugmentSubList> list = new ArrayList<AugmentSubList>(2);
                list.add(b2.build());
                b2.setSubAugName("Sub Augment E2-"+elementID);
                list.add(b2.build());
                b1.setAugmentSubList(list);
            }
            b.addAugmentation(SalPersistedDomTest1.class, b1.build());
        }
        if(subContainer){
            SalPersistedSubContainerBuilder b1 = new SalPersistedSubContainerBuilder();
            b1.setSubContainerMainString("Sub Container "+elementID);
            b.setSalPersistedSubContainer(b1.build());
        }
        if(subList){
            List<NoKeysubList> list = new ArrayList<NoKeysubList>(2);
            NoKeysubListBuilder b1 = new NoKeysubListBuilder();
            for(int i=0;i<numOfSubElements;i++){
                b1.setName("Sub List "+(i+1)+"-"+elementID);
                b1.setTypedeftest(mytype);
                b1.setEnumtest(MyEnumType.TestEnum2);
                b1.setTestInt8(new Byte((byte)253));
                b1.setTestInt16(new Short((short)456));
                b1.setTestInt32(Integer.MAX_VALUE);
                b1.setTestInt64(Long.MIN_VALUE);
                if(includeBigDecimal)
                    b1.setTestDecimal64(new BigDecimal((double)12345.4321));
                b1.setTestBinary(new byte[]{(byte)5,(byte)129,(byte)33});
                b1.setBitsTest(new TypedefBits(true, false));
                list.add(b1.build());
            }
            b.setNoKeysubList(list);
            ListWithKeyBuilder b2 = new ListWithKeyBuilder();
            List<ListWithKey> listWithKey = new LinkedList<ListWithKey>();
            for(int i=0;i<numOfSubElements;i++){
                b2.setId("ID-"+(i+1)+"-"+elementID);
                b2.setKey(new ListWithKeyKey(b2.getId()));
                b2.setName("Key Element-"+(i+1)+"-"+elementID);
                listWithKey.add(b2.build());
            }
            b.setListWithKey(listWithKey);
        }
        return b.build();
    }

    @Test
    public void mdsalEncoderTest() {
        SalPersistedDomTest before = buildTestElement(0,true,true,true,true,true);
        ByteArrayEncodeDataContainer ba = new ByteArrayEncodeDataContainer(1024,database.getTypeDescriptorsContainer().getTypeDescriptorByObject(before));
        ba.getEncoder().encodeObject(before, ba,SalPersistedDomTest.class);

        byte data[] = ByteArrayEncodeDataContainer.toSingleByteArray(ba);
        ByteArrayEncodeDataContainer singleSource = new ByteArrayEncodeDataContainer(data,database.getTypeDescriptorsContainer().getTypeDescriptorByObject(before));
        EncodeDataContainer newSource = ByteArrayEncodeDataContainer.fromSingleByteArray(singleSource);

        SalPersistedDomTest after = (SalPersistedDomTest) ba.getEncoder().decodeObject(newSource);
        Assert.assertEquals(before, after);
    }

    @Test
    public void persistTestNoAugmentationNoChildren(){
        SalPersistedDomTest before = buildTestElement(0,false,false,false,false,true);
        database.write(before, -1);
        SalPersistedDomTest after = (SalPersistedDomTest)database.read(SalPersistedDomTest.class, 0);
        Assert.assertEquals(before, after);
    }

    @Test
    public void persistTestWithAugmentationNoChildren(){
        SalPersistedDomTest before = buildTestElement(0,true,false,false,false,true);
        database.write(before, -1);
        SalPersistedDomTest after = (SalPersistedDomTest)database.read(SalPersistedDomTest.class, 0);
        Assert.assertEquals(before, after);
    }

    @Test
    public void persistTestNoAugmentationWithChildContainer(){
        SalPersistedDomTest before = buildTestElement(0,false,true,false,false,true);
        database.write(before, -1);
        SalPersistedDomTest after = (SalPersistedDomTest)database.read(SalPersistedDomTest.class, 0);
        Assert.assertEquals(before, after);
    }

    @Test
    public void persistTestNoAugmentationWithChildList(){
        SalPersistedDomTest before = buildTestElement(0,false,false,true,false,true);
        database.write(before, -1);
        SalPersistedDomTest after = (SalPersistedDomTest)database.read(SalPersistedDomTest.class, 0);
        Assert.assertEquals(before, after);
    }

    @Test
    public void persistTestNoAugmentationWithChildListAndContainer(){
        SalPersistedDomTest before = buildTestElement(0,false,true,true,false,true);
        database.write(before, -1);
        SalPersistedDomTest after = (SalPersistedDomTest)database.read(SalPersistedDomTest.class, 0);
        Assert.assertEquals(before, after);
    }

    @Test
    public void persistTestWithAugmentationWithChildListAndContainer(){
        SalPersistedDomTest before = buildTestElement(0,true,true,true,false,true);
        database.write(before, -1);
        SalPersistedDomTest after = (SalPersistedDomTest)database.read(SalPersistedDomTest.class, 0);
        Assert.assertEquals(before, after);
    }

    @Test
    public void persistTestWithAugmentationWithChildListAndContainerWithSubListAugmentation(){
        SalPersistedDomTest before = buildTestElement(0,true,true,true,true,true);
        database.write(before, -1);
        SalPersistedDomTest after = (SalPersistedDomTest)database.read(SalPersistedDomTest.class, 0);
        Assert.assertEquals(before, after);
    }

    @Test
    public void testRetrieveByChild(){
        setStartTime("Inserting 100");
        SalPersistedDomTest before = null;
        for(int i=0;i<100;i++){
            SalPersistedDomTest b = buildTestElement(i,true,true,true,true,true);
            database.write(b, -1);
            if(i==60)
                before = b;
        }
        setEndTime();
        SalPersistedDomTest after = (SalPersistedDomTest)database.readAllGraphBySingleNode(NoKeysubList.class, 120);
        Assert.assertEquals(before, after);
    }

    @Test
    public void testwritereadwriteread(){
        SalPersistedDomTest before = buildTestElement(0,true,true,true,true,true);
        database.write(before, -1);
        SalPersistedDomTest after = (SalPersistedDomTest)database.read(SalPersistedDomTest.class, 0);
        Assert.assertEquals(before, after);
        before = buildTestElement(1,true,true,true,true,true);
        database.write(before, -1);
        after = (SalPersistedDomTest)database.read(SalPersistedDomTest.class, 1);
        Assert.assertEquals(before, after);
    }

    @Test
    public void testPerformance500000(){
        SalPersistedDomTest before = buildTestElement(0,true,true,true,true,false);
        database.write(before, -1);
        setStartTime("Inserting 500000");
        int testSize = 500000;
        System.out.println();
        for(int i=1;i<testSize;i++){
            before = buildTestElement(i,true,true,true,true,false);
            if(i%50000==0 && i>0){
                System.out.println("Inserted count (Memory="+getUsedMemory()+")="+i);
            }
            database.write(before, -1);
        }
        setEndTime();
        Assert.assertEquals((endTime-startTime)<60000, true);
        setStartTime("Closing Database...");
        database.close();
        database = null;
        setEndTime();
        setStartTime("Opening Database...");
        database = new ByteArrayObjectDataStore("MDSalDataStoreTest",true);
        setupObservers();
        setEndTime();
        database.read(SalPersistedDomTest.class, 0);
        setStartTime("Reading 500000...");
        for(int i=1;i<testSize;i++){
            if(i%50000==0 && i>0){
                System.out.println("Read count (Memory="+getUsedMemory()+")="+i);
            }
            database.read(SalPersistedDomTest.class, i);
        }
        setEndTime();
        Assert.assertEquals((endTime-startTime)<30000, true);
    }

    @Test
    public void testSaveAndLoadDatabase(){
        setStartTime("Inserting 2000");
        List<SalPersistedDomTest> elements = new ArrayList<SalPersistedDomTest>();
        for(int i=0;i<2000;i++){
            SalPersistedDomTest before = buildTestElement(i,true,true,true,true,true);
            elements.add(before);
            database.write(before, -1);
        }
        setEndTime();
        database.close();
        database = null;
        setStartTime("Loading Database...");
        database = new ByteArrayObjectDataStore("MDSalDataStoreTest",true);
        setupObservers();
        setEndTime();
        setStartTime("Comparing 2000...");
        for(int i=0;i<2000;i++){
            SalPersistedDomTest after = (SalPersistedDomTest)database.read(SalPersistedDomTest.class, i);
            SalPersistedDomTest before = elements.get(i);
            Assert.assertEquals(before, after);
        }
        setEndTime();
    }

    @Test
    public void testUpdateElementbyIndexNoOverflow(){
        setStartTime("Inserting 2000");
        List<SalPersistedDomTest> elements = new ArrayList<SalPersistedDomTest>();
        for(int i=0;i<2000;i++){
            SalPersistedDomTest before = buildTestElement(i,true,true,true,true,true);
            elements.add(before);
            database.write(before, -1);
        }
        setEndTime();

        SalPersistedDomTest before = buildTestElement(500,true,true,true,true,true);
        database.update(before, -1, 600);
        SalPersistedDomTest after = (SalPersistedDomTest)database.read(SalPersistedDomTest.class,600);
        Assert.assertEquals(before, after);
    }

    @Test
    public void testUpdateElementbyIndexNoOverflowAddedSubElements(){
        setStartTime("Inserting 2000");
        List<SalPersistedDomTest> elements = new ArrayList<SalPersistedDomTest>();
        for(int i=0;i<2000;i++){
            SalPersistedDomTest before = buildTestElement(i,true,true,true,true,true);
            elements.add(before);
            database.write(before, -1);
        }
        setEndTime();

        SalPersistedDomTest before = buildTestElement("Updated Element",500,true,true,true,true,true,4);
        database.update(before, -1, 500);
        SalPersistedDomTest after = (SalPersistedDomTest)database.read(SalPersistedDomTest.class,500);
        Assert.assertEquals(before, after);
    }

    @Test
    public void testUpdateElementbyIndexWithOverflow(){
        setStartTime("Inserting 2000");
        List<SalPersistedDomTest> elements = new ArrayList<SalPersistedDomTest>();
        for(int i=0;i<2000;i++){
            SalPersistedDomTest before = buildTestElement(i,true,true,true,true,true);
            elements.add(before);
            database.write(before, -1);
        }
        setEndTime();

        SalPersistedDomTest before = buildTestElement("Updated Element With Overflow In Name",500,true,true,true,true,true,2);
        database.update(before, -1, 500);
        SalPersistedDomTest after = (SalPersistedDomTest)database.read(SalPersistedDomTest.class,500);
        Assert.assertEquals(before, after);
    }

    @Test
    public void testUpdateElementbyIndexWithOverflowAddedSubElements(){
        setStartTime("Inserting 2000");
        List<SalPersistedDomTest> elements = new ArrayList<SalPersistedDomTest>();
        for(int i=0;i<2000;i++){
            SalPersistedDomTest before = buildTestElement(i,true,true,true,true,true);
            elements.add(before);
            database.write(before, -1);
        }
        setEndTime();

        SalPersistedDomTest before = buildTestElement("Updated Element With Overflow In Name",500,true,true,true,true,true,4);
        database.update(before, -1, 500);
        SalPersistedDomTest after = (SalPersistedDomTest)database.read(SalPersistedDomTest.class,500);
        Assert.assertEquals(before, after);
    }

    @Test
    public void testUpdateElementbyKeyNoOverflow(){
        setStartTime("Inserting 2000");
        List<SalPersistedDomTest> elements = new ArrayList<SalPersistedDomTest>();
        for(int i=0;i<2000;i++){
            SalPersistedDomTest before = buildTestElement(i,true,true,true,true,true);
            elements.add(before);
            database.write(before, -1);
        }
        setEndTime();

        ListWithKeyBuilder b2 = new ListWithKeyBuilder();
        b2.setId("ID-1-"+650);
        ListWithKeyKey key = new ListWithKeyKey(b2.getId());
        b2.setKey(key);
        b2.setName("Updated");
        ListWithKey before = b2.build();

        database.update(before, -1, -1);

        ListWithKey after = (ListWithKey)database.read(key,ListWithKey.class);
        Assert.assertEquals(before, after);
    }

    @Test
    public void testUpdateElementbyKeyWithOverflow(){
        setStartTime("Inserting 2000");
        List<SalPersistedDomTest> elements = new ArrayList<SalPersistedDomTest>();
        for(int i=0;i<2000;i++){
            SalPersistedDomTest before = buildTestElement(i,true,true,true,true,true);
            elements.add(before);
            database.write(before, -1);
        }
        setEndTime();

        ListWithKeyBuilder b2 = new ListWithKeyBuilder();
        b2.setId("ID-1-"+650);
        ListWithKeyKey key = new ListWithKeyKey(b2.getId());
        b2.setKey(key);
        b2.setName("Updated Element with overflow test");
        ListWithKey before = b2.build();

        database.update(before, -1, -1);

        ListWithKey after = (ListWithKey)database.read(key,ListWithKey.class);
        Assert.assertEquals(before, after);
    }

    @Test
    public void testDeleteObjectByIndexNoChildren(){
        setStartTime("Inserting 2000");
        NoKeysubList _before = null;
        for(int i=0;i<2000;i++){
            SalPersistedDomTest before = buildTestElement(i,true,true,true,true,true);
            if(i==504){
                _before = before.getNoKeysubList().get(0);
            }
            database.write(before, -1);
        }
        setEndTime();

        SalPersistedDomTest before = (SalPersistedDomTest)database.readNoChildren(SalPersistedDomTest.class, 504);
        Assert.assertEquals(true, before!=null);
        SalPersistedDomTest after = (SalPersistedDomTest)database.deleteNoChildren(SalPersistedDomTest.class,504);
        Assert.assertEquals(before, after);
        after = (SalPersistedDomTest)database.readNoChildren(SalPersistedDomTest.class,504);
        Assert.assertEquals(true, after==null);
        after = (SalPersistedDomTest)database.deleteNoChildren(SalPersistedDomTest.class,504);
        Assert.assertEquals(true, after==null);
        NoKeysubList _after = (NoKeysubList)database.read(NoKeysubList.class, 1008);
        Assert.assertEquals(_before, _after);
    }

    @Test
    public void testDeleteObjectByKey(){
        setStartTime("Inserting 2000");
        List<SalPersistedDomTest> elements = new ArrayList<SalPersistedDomTest>();
        for(int i=0;i<2000;i++){
            SalPersistedDomTest before = buildTestElement(i,true,true,true,true,true);
            elements.add(before);
            database.write(before, -1);
        }
        setEndTime();

        SalPersistedDomTest _before = (SalPersistedDomTest)database.read(SalPersistedDomTest.class,500);

        ListWithKeyBuilder b2 = new ListWithKeyBuilder();
        b2.setId("ID-1-"+500);
        ListWithKeyKey key = new ListWithKeyKey("ID-1-"+500);
        ListWithKey before = (ListWithKey)database.read(key,ListWithKey.class);
        ListWithKey after = (ListWithKey)database.delete(key,ListWithKey.class);
        Assert.assertEquals(before, after);
        after = (ListWithKey)database.delete(key,ListWithKey.class);
        Assert.assertEquals(true, after==null);
        SalPersistedDomTest _after = (SalPersistedDomTest)database.read(SalPersistedDomTest.class,500);
        Assert.assertNotEquals(_before, _after);
        Assert.assertEquals(_after.getListWithKey().size(), 1);
        _before = (SalPersistedDomTest)database.delete(SalPersistedDomTest.class,500);
        Assert.assertEquals(_after, _before);
    }

    @Test
    public void testDeleteAllObjectByIndex(){
        setStartTime("Inserting 2000");
        for(int i=0;i<2000;i++){
            SalPersistedDomTest before = buildTestElement(i,true,true,true,true,true);
            database.write(before, -1);
        }
        setEndTime();

        SalPersistedDomTest before = (SalPersistedDomTest)database.read(SalPersistedDomTest.class, 504);
        Assert.assertEquals(true, before!=null);
        SalPersistedDomTest after = (SalPersistedDomTest)database.delete(SalPersistedDomTest.class,504);
        Assert.assertEquals(before, after);
        after = (SalPersistedDomTest)database.read(SalPersistedDomTest.class,504);
        Assert.assertEquals(true, after==null);
        after = (SalPersistedDomTest)database.delete(SalPersistedDomTest.class,504);
        Assert.assertEquals(true, after==null);
    }

    @Test
    public void testSelect100Of10000(){
        setStartTime("Inserting 10000");
        for(int i=0;i<10000;i++){
            SalPersistedDomTest before = buildTestElement(i,true,true,true,true,true);
            database.write(before, -1);
        }
        setEndTime();
        setStartTime("Selecting 1 of those 100...");
        executeQueryTest("select * from SalPersistedDomTest where RowIndex>=422 and RowIndex<=522;","testSelect100Of10000",database);
        setEndTime();
    }

    @Test
    public void testSelect100Of10000JustAugmentationFields(){
        setStartTime("Inserting 10000");
        for(int i=0;i<10000;i++){
            SalPersistedDomTest before = buildTestElement(i,true,true,true,true,true);
            database.write(before, -1);
        }
        setEndTime();
        setStartTime("Selecting 100 of those 10000...");
        executeQueryTest("select MainString2,Atypedeftest,AtestInt32 from SalPersistedDomTest where RowIndex>=700 and RowIndex<=800;","testSelect100Of10000JustAugmentationFields",database);
        setEndTime();
    }

    @Test
    public void testSelect100Of20000WithSubElements(){
        setStartTime("Inserting 10000");
        for(int i=0;i<10000;i++){
            SalPersistedDomTest before = buildTestElement(i,true,true,true,true,true);
            database.write(before, -1);
        }
        setEndTime();
        setStartTime("Selecting 100 of those 20000 sub elements...");
        executeQueryTest("select MainString2,Atypedeftest,AtestInt32,NoKeysubList.Name,NoKeysubList.Typedeftest from SalPersistedDomTest,NoKeysubList where RowIndex>=557 and RowIndex<=657;","testSelect100Of20000WithSubElements",database);
        setEndTime();
    }

    @Test
    public void testSelect1Of20000WithSubElementsWithCriteria(){
        setStartTime("Inserting 10000");
        for(int i=0;i<10000;i++){
            SalPersistedDomTest before = buildTestElement(i,true,true,true,true,true);
            database.write(before, -1);
        }
        setEndTime();
        setStartTime("Selecting 100 of those 20000 sub elements...");
        executeQueryTest("select MainString2,Atypedeftest,AtestInt32,NoKeysubList.Name,NoKeysubList.Typedeftest from SalPersistedDomTest,NoKeysubList where NoKeysubList.Name='Sub List 1-553' and RowIndex>=0 and RowIndex<=10000;","testSelect1Of20000WithSubElementsWithCriteria",database);
        setEndTime();
    }

    @Test
    public void testSelectSimpleObjects() throws SQLException{
        setStartTime("Inserting 10000");
        SalPersistedDomTest beforeArray[] = new SalPersistedDomTest[10000];
        for(int i=0;i<10000;i++){
            beforeArray[i] = buildTestElement(i,true,true,true,true,true);
            database.write(beforeArray[i], -1);
        }
        setEndTime();
        String sql = "select Objects from SalPersistedDomTest where rowindex>=0 and rowIndex<=10000";
        ResultSet rs = database.executeSql(sql);
        while(rs.next()){
            SalPersistedDomTest after = (SalPersistedDomTest)rs.getObject(SalPersistedDomTest.class.getName());
            Assert.assertEquals(beforeArray[after.getCountID()], after);
        }
    }

    @Test
    public void testSelect1Of20000WithSubElementsWithCriteriaOnParent(){
        setStartTime("Inserting 10000");
        for(int i=0;i<10000;i++){
            SalPersistedDomTest before = buildTestElement(i,true,true,true,true,true);
            database.write(before, -1);
        }
        setEndTime();
        setStartTime("Selecting 100 of those 20000 sub elements...");
        executeQueryTest("select MainString2,Atypedeftest,AtestInt32,NoKeysubList.Name,NoKeysubList.Typedeftest from SalPersistedDomTest,NoKeysubList where MainString='Test String 553' and RowIndex>=0 and RowIndex<=10000;","testSelect1Of20000WithSubElementsWithCriteriaOnParent",database);
        setEndTime();
    }

    @Test
    public void testSelect1Of20000WithSubElementsWithCriteriaOnParentAugmentation(){
        setStartTime("Inserting 10000");
        ListWithKey _before  = null;
        for(int i=0;i<10000;i++){
            SalPersistedDomTest before = buildTestElement(i,true,true,true,true,true);
            database.write(before, -1);
            if(i==6361){
                _before = before.getListWithKey().get(0);
            }
        }
        ListWithKey after = (ListWithKey)database.read(_before.getKey(),ListWithKey.class);
        Assert.assertEquals(_before, after);
    }

    @Test
    public void testRetrieveElementByKey(){
        setStartTime("Inserting 10000");
        for(int i=0;i<10000;i++){
            SalPersistedDomTest before = buildTestElement(i,true,true,true,true,true);
            database.write(before, -1);
        }
        setEndTime();
        setStartTime("Selecting 100 of those 20000 sub elements...");
        executeQueryTest("select MainString2,Atypedeftest,AtestInt32,NoKeysubList.Name,NoKeysubList.Typedeftest from SalPersistedDomTest,NoKeysubList where MainString2='Augmentation 553' and RowIndex>=0 and RowIndex<=10000;","testRetrieveElementByKey",database);
        setEndTime();
    }

    @Test
    public void testSelect100Of20000SubElements(){
        setStartTime("Inserting 10000");
        for(int i=0;i<10000;i++){
            SalPersistedDomTest before = buildTestElement(i,true,true,true,true,true);
            database.write(before, -1);
        }
        setEndTime();
        setStartTime("Selecting 100 of those 20000 sub elements...");
        executeQueryTest("select NoKeysubList.Name,Typedeftest from NoKeysubList where RowIndex>=557 and RowIndex<=657;","testSelect100Of20000SubElements",database);
        setEndTime();
    }
    @Test
    public void testThreads(){
        setStartTime("Inserting "+numberOfThreads*numberOfRecordsPerThread+" records using "+numberOfThreads+" threads, "+numberOfRecordsPerThread+" each...");
        ThreadPool tp = new ThreadPool(numberOfThreads, "ThreadTest", 2000);
        finishCount = numberOfThreads;
        Task[] tasks = new Task[numberOfThreads];
        synchronized(threadsFinish){
            for(int i=0;i<numberOfThreads;i++){
                tasks[i] = new Task(database,i);
                tp.addTask(tasks[i]);
            }
            try{threadsFinish.wait();}catch(Exception err){}
        }
        setEndTime();

        SalPersistedDomTest beforeArray[] = new SalPersistedDomTest[numberOfThreads*numberOfRecordsPerThread];
        for(int i=0;i<tasks.length;i++){
            for(SalPersistedDomTest t:tasks[i].beforeList){
                beforeArray[t.getCountID()] = t;
            }
        }
        for(int i=0;i<numberOfThreads*numberOfRecordsPerThread;i++){
            SalPersistedDomTest after = (SalPersistedDomTest)database.read(SalPersistedDomTest.class, i);
            if(!after.equals(beforeArray[after.getCountID()])){
                System.out.println("Error:"+after.getCountID());
            }
            //Assert.assertEquals(beforeArray[after.getCountID()], after);
        }
    }
    private int readyCount = 0;
    private int finishCount = 0;
    private int numberOfThreads = 10;
    private int numberOfRecordsPerThread = 1000;
    private Object threadsSyncStart = new Object();
    private Object threadsFinish = new Object();

    private class Task implements Runnable {
        private ObjectDataStore db = null;
        private int myNumber = -1;
        private List<SalPersistedDomTest> beforeList = new LinkedList<SalPersistedDomTest>();
        public Task(ObjectDataStore _db,int num){
            this.db = _db;
            this.myNumber = num;
        }
        public void run(){
            synchronized(threadsSyncStart){
                readyCount++;
                if(readyCount==numberOfThreads)
                    threadsSyncStart.notifyAll();
                else
                    try{threadsSyncStart.wait();}catch(Exception err){}
            }
            for(int i=0;i<numberOfRecordsPerThread;i++){
                SalPersistedDomTest before = buildTestElement(myNumber*numberOfRecordsPerThread+i,true,true,true,true,true);
                beforeList.add(before);
                db.write(before, -1);
            }
            synchronized(threadsFinish){
                finishCount--;
                if(finishCount==0){
                    threadsFinish.notifyAll();
                }
            }
        }
    }
    public void setStartTime(String message){
        System.out.print(message+"...");
        startTime = System.currentTimeMillis();
    }

    public void setEndTime(){
        endTime = System.currentTimeMillis();
        System.out.println("Done! Time="+(endTime-startTime));
    }

    public static void executeQueryTest(String sql,String filename,ObjectDataStore database){
        if(createTestResources){
            try{
                PrintStream prt = new PrintStream(new File("./src/test/resources/"+filename));
                database.executeSql(sql, prt, false);
                prt.close();
            }catch(Exception err){
                err.printStackTrace();
            }
        }else{
            try{
                ByteArrayOutputStream bout = new ByteArrayOutputStream();
                PrintStream prt = new PrintStream(bout);
                database.executeSql(sql, prt, false);
                String output = new String(bout.toByteArray());
                String expected = readExpectedOutput(filename);
                if(!expected.equals(output)){
                    FileOutputStream out = new FileOutputStream("./src/test/resources/"+filename+".err");
                    out.write(output.getBytes());
                    out.close();
                }
                Assert.assertEquals(expected,output);
            }catch(Exception err){
                err.printStackTrace();
            }
        }
    }

    public static String readExpectedOutput(String filename){
        try{
            File f = new File("./src/test/resources/"+filename);
            FileInputStream in = new FileInputStream(f);
            byte data[] = new byte[(int)f.length()];
            in.read(data);
            in.close();
            return new String(data);
        }catch(Exception  err){
            err.printStackTrace();
        }
        return null;
    }

    public static String getUsedMemory(){
        return ""+formatMemoryNumber(Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory());
    }

    public static String formatMemoryNumber(long l){
        String memStr = ""+(l/1024);
        String result = ","+memStr.substring(memStr.length()-3);
        int i = memStr.length()-3;
        while(i-3>0){
            result = ","+memStr.substring(i-3,i)+result;
        }
        if(i>0)
            result = memStr.substring(0,i)+result;
        return result+"m";
    }
}
