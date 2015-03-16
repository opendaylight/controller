/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.binding.test.connect.dom;

import org.opendaylight.controller.sal.binding.test.AbstractDataServiceTest;


/**
 * FIXME: Migrate to use new Data Broker APIs
 */
@SuppressWarnings("deprecation")
public class ChangeOriginatedInDomBrokerTest extends AbstractDataServiceTest {
//
//    protected static final Logger LOG = LoggerFactory.getLogger(ChangeOriginatedInDomBrokerTest.class);
//
//    private static final QName TLL_NAME_QNAME = QName.create(TopLevelList.QNAME, "name");
//    private static final QName LIST1_ATTR_STR_QNAME = QName.create(List1.QNAME, "attr-str");
//
//    private static final String TLL_NAME = "1";
//    private static final int LIST11_ATTR_INT = 1234;
//    private static final String LIST1_ATTR_STR = "foo:foo";
//
//    private static final TopLevelListKey TLL_KEY = new TopLevelListKey(TLL_NAME);
//    private static final List1Key LIST1_KEY = new List1Key(LIST1_ATTR_STR);
//    private static final List11Key LIST11_KEY = new List11Key(LIST11_ATTR_INT);
//
//    protected final SettableFuture<DataChangeEvent<InstanceIdentifier<?>, DataObject>> modificationCapture = SettableFuture.create();
//
//    private static final Map<QName, Object> TLL_KEY_BI = Collections.<QName, Object> singletonMap(TLL_NAME_QNAME,
//            TLL_NAME);
//
//    private static final InstanceIdentifier<TopLevelList> NODE_INSTANCE_ID_BA = InstanceIdentifier.builder(Top.class) //
//            .child(TopLevelList.class, TLL_KEY).toInstance();
//
//    private static final Map<QName, Object> LIST1_KEY_BI = //
//    ImmutableMap.<QName, Object> of(LIST1_ATTR_STR_QNAME, LIST1_ATTR_STR);;
//
//    private static final org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier LIST1_INSTANCE_ID_BI = //
//    org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.builder() //
//            .node(Top.QNAME) //
//            .nodeWithKey(TopLevelList.QNAME, TLL_KEY_BI) //
//            .nodeWithKey(List1.QNAME, LIST1_KEY_BI) //
//            .toInstance();
//
//    private static final InstanceIdentifier<List1> LIST1_PATH_BA = //
//            NODE_INSTANCE_ID_BA.builder() //
//            .augmentation(TllComplexAugment.class) //
//            .child(List1.class, LIST1_KEY) //
//            .toInstance();
//
//    @Test
//    public void simpleModifyOperation() throws Exception {
//
//        assertNull(biDataService.readConfigurationData(LIST1_INSTANCE_ID_BI));
//
//        registerChangeListener();
//
//        CompositeNode domflow = createTestList1();
//        DataModificationTransaction biTransaction = biDataService.beginTransaction();
//        biTransaction.putConfigurationData(LIST1_INSTANCE_ID_BI, domflow);
//        RpcResult<TransactionStatus> biResult = biTransaction.commit().get();
//        assertEquals(TransactionStatus.COMMITED, biResult.getResult());
//        DataChangeEvent<InstanceIdentifier<?>, DataObject> event = modificationCapture.get(1000,TimeUnit.MILLISECONDS);
//        assertNotNull(event);
//        LOG.info("Created Configuration :{}",event.getCreatedConfigurationData());
//        List1 list1 = (List1) event.getCreatedConfigurationData().get(LIST1_PATH_BA);
//        assertNotNull(list1);
//        assertNotNull(list1.getAttrStr());
//        assertNotNull(list1.getList11());
//        assertNotNull(list1.getList12());
//        assertEquals(TransactionStatus.COMMITED, biResult.getResult());
//
//    }
//
//    private void registerChangeListener() {
//        baDataService.registerDataChangeListener(LIST1_PATH_BA, new DataChangeListener() {
//
//            @Override
//            public void onDataChanged(final DataChangeEvent<InstanceIdentifier<?>, DataObject> change) {
//                LOG.info("Data Change listener invoked.");
//                modificationCapture.set(change);
//            }
//        });
//    }
//
//    private CompositeNode createTestList1() {
//        List1Builder l1b = new List1Builder();
//        List11Builder l11b = new List11Builder();
//        List12Builder l12b = new List12Builder();
//        l11b.setKey(LIST11_KEY);
//        l11b.setAttrStr("foo:foo:foo");
//        l12b.setKey(new List12Key(321));
//        l12b.setAttrStr("foo:foo:bar");
//        l1b.setKey(LIST1_KEY);
//        l1b.setList11(ImmutableList.of(l11b.build()));
//        l1b.setList12(ImmutableList.of(l12b.build()));
//        CompositeNode domList1 = mappingService.toDataDom(l1b.build());
//        return domList1;
//    }
}
