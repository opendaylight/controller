package org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.xsql.rev140626;

import org.opendaylight.controller.md.sal.dom.xsql.XSQLAdapter;
import org.opendaylight.xsql.XSQLProvider;

public class XSQLModule extends org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.xsql.rev140626.AbstractXSQLModule {
    public XSQLModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public XSQLModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.xsql.rev140626.XSQLModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        XSQLAdapter xsqlAdapter = XSQLAdapter.getInstance();
        getSchemaServiceDependency().registerSchemaContextListener(xsqlAdapter);
        xsqlAdapter.setDataBroker(getAsyncDataBrokerDependency());
        final XSQLProvider p = new XSQLProvider();
        Runnable runthis = new Runnable() {
            @Override
            public void run() {
                try{Thread.sleep(10000);}catch(Exception err){}
                p.buildXSQL(getDataBrokerDependency());
            }
        };
        //new Thread(runthis).start();
        return p;
    }
/*
    public static SalTest buildTestElement(String name,int elementID, boolean addAugmentation,boolean subContainer,boolean subList,boolean subListAugmentation,boolean includeBigDecimal,int numOfSubElements){
        //Seems like BigDecimal conversion is extremel slow so on some of the tests i wish to disable it.

        SalTestBuilder b5 = new SalTestBuilder();
        List<SalPersistedDomTest> l = new ArrayList<SalPersistedDomTest>();

        SalPersistedDomTestBuilder b = new SalPersistedDomTestBuilder();
        if(name==null)
            b.setMainString("Test String " + elementID);
        else
            b.setMainString(name);
        b.setKey(new SalPersistedDomTestKey(b.getMainString()));
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
            b1.setMainString2("abcdAugmentation "+elementID);
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
        l.add(b.build());
        b5.setSalPersistedDomTest(l);
        return b5.build();
    }*/
}
