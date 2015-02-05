/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.binding.test.connect.dom;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.concurrent.Future;

import org.junit.Test;
import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.sal.binding.api.data.DataModificationTransaction;
import org.opendaylight.controller.sal.binding.test.AbstractDataServiceTest;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.Top;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.two.level.list.TopLevelList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.two.level.list.TopLevelListBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.two.level.list.TopLevelListKey;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;

/**
 * FIXME: Migrate to use new Data Broker APIs
 */
@SuppressWarnings("deprecation")
public class BrokerIntegrationTest extends AbstractDataServiceTest {

    private static final TopLevelListKey TLL_FOO_KEY = new TopLevelListKey("foo");
    private static final TopLevelListKey TLL_BAR_KEY = new TopLevelListKey("bar");
    private static final TopLevelListKey TLL_BAZ_KEY = new TopLevelListKey("baz");
    private static final InstanceIdentifier<Top> TOP_PATH = InstanceIdentifier.builder(Top.class).build();
    private static final InstanceIdentifier<TopLevelList> FOO_PATH = TOP_PATH.child(TopLevelList.class, TLL_FOO_KEY);
    private static final InstanceIdentifier<TopLevelList> BAR_PATH = TOP_PATH.child(TopLevelList.class, TLL_BAR_KEY);
    private static final InstanceIdentifier<TopLevelList> BAZ_PATH = TOP_PATH.child(TopLevelList.class, TLL_BAZ_KEY);

    @Test
    public void simpleModifyOperation() throws Exception {

        DataObject tllFoo = baDataService.readConfigurationData(FOO_PATH);
        assertNull(tllFoo);
        TopLevelList tllFooData = createTll(TLL_FOO_KEY);

        DataModificationTransaction transaction = baDataService.beginTransaction();
        transaction.putConfigurationData(FOO_PATH, tllFooData);
        Future<RpcResult<TransactionStatus>> commitResult = transaction.commit();
        assertNotNull(commitResult);

        RpcResult<TransactionStatus> result = commitResult.get();

        assertNotNull(result);
        assertNotNull(result.getResult());
        assertEquals(TransactionStatus.COMMITED, result.getResult());

        TopLevelList readedData = (TopLevelList) baDataService.readConfigurationData(FOO_PATH);
        assertNotNull(readedData);
        assertEquals(tllFooData.getKey(), readedData.getKey());

        TopLevelList nodeBarData = createTll(TLL_BAR_KEY);
        TopLevelList nodeBazData = createTll(TLL_BAZ_KEY);

        DataModificationTransaction insertMoreTr = baDataService.beginTransaction();
        insertMoreTr.putConfigurationData(BAR_PATH, nodeBarData);
        insertMoreTr.putConfigurationData(BAZ_PATH, nodeBazData);
        RpcResult<TransactionStatus> result2 = insertMoreTr.commit().get();

        assertNotNull(result2);
        assertNotNull(result2.getResult());
        assertEquals(TransactionStatus.COMMITED, result.getResult());

        Top top = (Top) baDataService.readConfigurationData(TOP_PATH);
        assertNotNull(top);
        assertNotNull(top.getTopLevelList());
        assertEquals(3, top.getTopLevelList().size());

        /**
         * We create transaction no 2
         *
         */
        DataModificationTransaction removalTransaction = baDataService.beginTransaction();
        assertNotNull(transaction);

        /**
         * We remove node 1
         *
         */
        removalTransaction.removeConfigurationData(BAR_PATH);

        /**
         * We commit transaction
         */
        Future<RpcResult<TransactionStatus>> commitResult2 = removalTransaction.commit();
        assertNotNull(commitResult2);

        RpcResult<TransactionStatus> result3 = commitResult2.get();

        assertNotNull(result3);
        assertNotNull(result3.getResult());
        assertEquals(TransactionStatus.COMMITED, result2.getResult());

        DataObject readedData2 = baDataService.readConfigurationData(BAR_PATH);
        assertNull(readedData2);
    }

    private static TopLevelList createTll(final TopLevelListKey key) {
        TopLevelListBuilder ret = new TopLevelListBuilder();
        ret.setKey(key);
        return ret.build();
    }
}
