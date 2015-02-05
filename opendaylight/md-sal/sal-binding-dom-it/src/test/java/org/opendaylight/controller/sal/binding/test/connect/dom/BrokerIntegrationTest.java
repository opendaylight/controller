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

    @Test
    public void simpleModifyOperation() throws Exception {

        InstanceIdentifier<TopLevelList> node1 = createNodePath("0");
        DataObject node = baDataService.readConfigurationData(node1);
        assertNull(node);
        TopLevelList nodeData1 = createNode("0");

        DataModificationTransaction transaction = baDataService.beginTransaction();
        transaction.putConfigurationData(node1, nodeData1);
        Future<RpcResult<TransactionStatus>> commitResult = transaction.commit();
        assertNotNull(commitResult);

        RpcResult<TransactionStatus> result = commitResult.get();

        assertNotNull(result);
        assertNotNull(result.getResult());
        assertEquals(TransactionStatus.COMMITED, result.getResult());

        TopLevelList readedData = (TopLevelList) baDataService.readConfigurationData(node1);
        assertNotNull(readedData);
        assertEquals(nodeData1.getKey(), readedData.getKey());

        InstanceIdentifier<TopLevelList> nodeFoo = createNodePath("foo");
        InstanceIdentifier<TopLevelList> nodeBar = createNodePath("bar");
        TopLevelList nodeFooData = createNode("foo");
        TopLevelList nodeBarData = createNode("bar");

        DataModificationTransaction insertMoreTr = baDataService.beginTransaction();
        insertMoreTr.putConfigurationData(nodeFoo, nodeFooData);
        insertMoreTr.putConfigurationData(nodeBar, nodeBarData);
        RpcResult<TransactionStatus> result2 = insertMoreTr.commit().get();

        assertNotNull(result2);
        assertNotNull(result2.getResult());
        assertEquals(TransactionStatus.COMMITED, result.getResult());

        Top allNodes = (Top) baDataService.readConfigurationData(InstanceIdentifier.builder(Top.class)
                .build());
        assertNotNull(allNodes);
        assertNotNull(allNodes.getTopLevelList());
        assertEquals(3, allNodes.getTopLevelList().size());

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
        removalTransaction.removeConfigurationData(node1);

        /**
         * We commit transaction
         */
        Future<RpcResult<TransactionStatus>> commitResult2 = removalTransaction.commit();
        assertNotNull(commitResult2);

        RpcResult<TransactionStatus> result3 = commitResult2.get();

        assertNotNull(result3);
        assertNotNull(result3.getResult());
        assertEquals(TransactionStatus.COMMITED, result2.getResult());

        DataObject readedData2 = baDataService.readConfigurationData(node1);
        assertNull(readedData2);
    }

    private static InstanceIdentifier<TopLevelList> createNodePath(final String string) {
        TopLevelListKey key = new TopLevelListKey(string);
        InstanceIdentifier<TopLevelList> path = InstanceIdentifier.builder(Top.class)
                .child(TopLevelList.class, key)
                .build();
        return path;
    }

    private static TopLevelList createNode(final String string) {
        TopLevelListBuilder ret = new TopLevelListBuilder()
            .setKey(new TopLevelListKey(string))
            .setName("node-name-"+string);
        return ret.build();
    }
}
