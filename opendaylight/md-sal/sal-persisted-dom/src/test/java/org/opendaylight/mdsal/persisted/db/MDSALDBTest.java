package org.opendaylight.mdsal.persisted.db;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.persisted.ObjectDataStore;
import org.opendaylight.persisted.codec.AttributeDescriptor;
import org.opendaylight.persisted.codec.EncodeDataContainer;
import org.opendaylight.persisted.codec.EncodeUtils;
import org.opendaylight.persisted.codec.TypeDescriptor;
import org.opendaylight.persisted.mdsal.MDSALClassExtractor;
import org.opendaylight.persisted.mdsal.MDSALObjectTypeRule;
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
        MDSALDBTest.cleanAll();
        MDSALDBTest test = new MDSALDBTest();
        test.setupFlagsAndCreateDB();
        test.testPerformance500000();
        System.gc();
        try{Thread.sleep(1000);}catch(Exception err){}
        System.out.println("final memory="+getUsedMemory());
        test.closeDBAndDeleteIT();
    }

    @BeforeClass
    public static void cleanAll(){
        ObjectDataStore.deleteDatabase();
        TypeDescriptor.addChildAttributeRule(new MDSalObjectChildRule());
        TypeDescriptor.addTypeAttributeRule(new MDSALObjectTypeRule());
        TypeDescriptor.setClassExtractor(new MDSALClassExtractor());
    }

    @Before
    public void setupFlagsAndCreateDB() {
        AttributeDescriptor.IS_SERVER_SIDE = true;
        TypeDescriptor.REGENERATE_SERIALIZERS = true;
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

    private static SalPersistedDomTest buildTestElement(int elementID, boolean addAugmentation,boolean subContainer,boolean subList,boolean subListAugmentation,boolean includeBigDecimal){
        return buildTestElement(null, elementID, addAugmentation, subContainer, subList, subListAugmentation, includeBigDecimal);
    }

    private static SalPersistedDomTest buildTestElement(String name,int elementID, boolean addAugmentation,boolean subContainer,boolean subList,boolean subListAugmentation,boolean includeBigDecimal){
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
            b1.setName("Sub List 1-"+elementID);
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
            b1.setName("Sub List 2-"+elementID);
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
            b.setNoKeysubList(list);

            List<ListWithKey> listWithKey = new LinkedList<ListWithKey>();
            ListWithKeyBuilder b2 = new ListWithKeyBuilder();
            b2.setId("ID-1-"+elementID);
            b2.setKey(new ListWithKeyKey(b2.getId()));
            b2.setName("Key Element-"+elementID);
            listWithKey.add(b2.build());

            b2.setId("ID-2-"+elementID);
            b2.setKey(new ListWithKeyKey(b2.getId()));
            b2.setName("Key Element-"+elementID);
            listWithKey.add(b2.build());
            b.setListWithKey(listWithKey);
        }
        return b.build();
    }

    @Test
    public void mdsalEncoderTest() {
        EncodeDataContainer ba = new EncodeDataContainer(1024);
        SalPersistedDomTest before = buildTestElement(0,true,true,true,true,true);
        EncodeUtils.encodeObject(before, ba,SalPersistedDomTest.class);

        byte data[] = EncodeDataContainer.toSingleByteArray(ba);
        EncodeDataContainer singleSource = new EncodeDataContainer(data);
        EncodeDataContainer newSource = EncodeDataContainer.fromSingleByteArray(singleSource);

        SalPersistedDomTest after = (SalPersistedDomTest) EncodeUtils.decodeObject(newSource);
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
    public void testPerformance500000(){
        setStartTime("Inserting 500000");
        System.out.println();
        for(int i=0;i<500000;i++){
            SalPersistedDomTest before = buildTestElement(i,true,true,true,true,false);
            if(i%50000==0 && i>0){
                System.out.println("Inserted count (Memory="+getUsedMemory()+")="+i);
            }
            database.write(before, -1);
        }
        setEndTime();
        Assert.assertEquals((endTime-startTime)<60000, true);
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
        database = new ObjectDataStore();
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
    public void testUpdateElementbyIndexWithOverflow(){
        setStartTime("Inserting 2000");
        List<SalPersistedDomTest> elements = new ArrayList<SalPersistedDomTest>();
        for(int i=0;i<2000;i++){
            SalPersistedDomTest before = buildTestElement(i,true,true,true,true,true);
            elements.add(before);
            database.write(before, -1);
        }
        setEndTime();

        SalPersistedDomTest before = buildTestElement("Updated Element With Overflow In Name",500,true,true,true,true,true);
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
        executeQueryTest("select MainString2,Atypedeftest,AtestInt32,NoKeysubList.Name,NoKeysubList.Typedeftest from SalPersistedDomTest,NoKeysubList where MainString2='Augmentation 553' and RowIndex>=0 and RowIndex<=10000;","testSelect1Of20000WithSubElementsWithCriteriaOnParentAugmentation",database);
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
