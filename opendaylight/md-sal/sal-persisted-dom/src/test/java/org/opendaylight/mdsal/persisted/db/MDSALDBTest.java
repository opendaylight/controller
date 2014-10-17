package org.opendaylight.mdsal.persisted.db;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.persisted.MDSALDatabase;
import org.opendaylight.persisted.codec.BytesArray;
import org.opendaylight.persisted.codec.MDSALColumn;
import org.opendaylight.persisted.codec.MDSALEncoder;
import org.opendaylight.persisted.codec.MDSALTable;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.persistedtest.rev141020.MyEnumType;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.persistedtest.rev141020.MyType;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.persistedtest.rev141020.SalPersistedDomTest;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.persistedtest.rev141020.SalPersistedDomTest1;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.persistedtest.rev141020.SalPersistedDomTest1Builder;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.persistedtest.rev141020.SalPersistedDomTestBuilder;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.persistedtest.rev141020.TypedefBits;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.persistedtest.rev141020.sal.persisted.dom.test.AugmentSubList;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.persistedtest.rev141020.sal.persisted.dom.test.AugmentSubListBuilder;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.persistedtest.rev141020.sal.persisted.dom.test.NoKeysubList;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.persistedtest.rev141020.sal.persisted.dom.test.NoKeysubListBuilder;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.persistedtest.rev141020.sal.persisted.dom.test.SalPersistedSubContainerBuilder;

public class MDSALDBTest {

    private MDSALDatabase database = null;
    private long startTime = 0L;
    private long endTime = 0L;

    @BeforeClass
    public static void cleanAll(){
        MDSALDatabase.deleteDatabase();
    }

    @Before
    public void setupFlagsAndCreateDB() {
        MDSALColumn.IS_SERVER_SIDE = true;
        MDSALTable.REGENERATE_SERIALIZERS = true;
        database = new MDSALDatabase();
    }
    @After
    public void closeDBAndDeleteIT(){
        if(database!=null){
            database.close();
            database = null;
        }
        MDSALDatabase.deleteDatabase();
    }

    private static SalPersistedDomTest buildTestElement(int elementID, boolean addAugmentation,boolean subContainer,boolean subList,boolean subListAugmentation){
        SalPersistedDomTestBuilder b = new SalPersistedDomTestBuilder();
        b.setMainString("Test String " + elementID);
        MyType mytype = new MyType("Test Typedef "+elementID);
        b.setTypedeftest(mytype);
        b.setEnumtest(MyEnumType.TestEnum2);
        b.setTestInt8(new Byte((byte)253));
        b.setTestInt16(new Short((short)456));
        b.setTestInt32(Integer.MAX_VALUE);
        b.setTestInt64(Long.MIN_VALUE);
        b.setTestDecimal64(new BigDecimal((double)12345.4321));
        b.setTestBinary(new byte[]{(byte)5,(byte)129,(byte)33});
        b.setBitsTest(new TypedefBits(true, false));
        //b.setTestUnion(new TypedefUnion("Union Test-"+elementID));
        if(addAugmentation){
            SalPersistedDomTest1Builder b1 = new SalPersistedDomTest1Builder();
            b1.setMainString2("Augmentation "+elementID);
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
            list.add(b1.build());
            b1.setName("Sub List 2-"+elementID);
            list.add(b1.build());
            b.setNoKeysubList(list);
        }
        return b.build();
    }

    @Test
    public void mdsalEncoderTest() {
        BytesArray ba = new BytesArray(1024);
        SalPersistedDomTest before = buildTestElement(0,true,true,true,true);
        MDSALEncoder.encodeObject(before, ba,SalPersistedDomTest.class);

        byte data[] = BytesArray.toSingleByteArray(ba);
        BytesArray singleSource = new BytesArray(data);
        BytesArray newSource = BytesArray.fromSingleByteArray(singleSource);

        SalPersistedDomTest after = (SalPersistedDomTest) MDSALEncoder.decodeObject(newSource);
        Assert.assertEquals(before, after);
    }

    @Test
    public void persistTestNoAugmentationNoChildren(){
        SalPersistedDomTest before = buildTestElement(0,false,false,false,false);
        database.write(before, -1);
        SalPersistedDomTest after = (SalPersistedDomTest)database.read(SalPersistedDomTest.class, 0);
        Assert.assertEquals(before, after);
    }

    @Test
    public void persistTestWithAugmentationNoChildren(){
        SalPersistedDomTest before = buildTestElement(0,true,false,false,false);
        database.write(before, -1);
        SalPersistedDomTest after = (SalPersistedDomTest)database.read(SalPersistedDomTest.class, 0);
        Assert.assertEquals(before, after);
    }

    @Test
    public void persistTestNoAugmentationWithChildContainer(){
        SalPersistedDomTest before = buildTestElement(0,false,true,false,false);
        database.write(before, -1);
        SalPersistedDomTest after = (SalPersistedDomTest)database.read(SalPersistedDomTest.class, 0);
        Assert.assertEquals(before, after);
    }

    @Test
    public void persistTestNoAugmentationWithChildList(){
        SalPersistedDomTest before = buildTestElement(0,false,false,true,false);
        database.write(before, -1);
        SalPersistedDomTest after = (SalPersistedDomTest)database.read(SalPersistedDomTest.class, 0);
        Assert.assertEquals(before, after);
    }

    @Test
    public void persistTestNoAugmentationWithChildListAndContainer(){
        SalPersistedDomTest before = buildTestElement(0,false,true,true,false);
        database.write(before, -1);
        SalPersistedDomTest after = (SalPersistedDomTest)database.read(SalPersistedDomTest.class, 0);
        Assert.assertEquals(before, after);
    }

    @Test
    public void persistTestWithAugmentationWithChildListAndContainer(){
        SalPersistedDomTest before = buildTestElement(0,true,true,true,false);
        database.write(before, -1);
        SalPersistedDomTest after = (SalPersistedDomTest)database.read(SalPersistedDomTest.class, 0);
        Assert.assertEquals(before, after);
    }

    @Test
    public void persistTestWithAugmentationWithChildListAndContainerWithSubListAugmentation(){
        SalPersistedDomTest before = buildTestElement(0,true,true,true,true);
        database.write(before, -1);
        SalPersistedDomTest after = (SalPersistedDomTest)database.read(SalPersistedDomTest.class, 0);
        Assert.assertEquals(before, after);
    }

    @Test
    public void testPerformance10000(){
        setStartTime("Inserting 10000");
        for(int i=0;i<10000;i++){
            SalPersistedDomTest before = buildTestElement(i,true,true,true,true);
            database.write(before, -1);
        }
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
}
