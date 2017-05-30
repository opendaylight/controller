/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.binding.test.connect.dom;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.common.base.Optional;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.sal.binding.test.AbstractDataServiceTest;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.Top;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.two.level.list.TopLevelList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.two.level.list.TopLevelListBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.two.level.list.TopLevelListKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

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

        DataBroker dataBroker = testContext.getDataBroker();
        Optional<TopLevelList> tllFoo = dataBroker.newReadOnlyTransaction().read(
                LogicalDatastoreType.CONFIGURATION, FOO_PATH).checkedGet(5, TimeUnit.SECONDS);
        assertFalse(tllFoo.isPresent());

        TopLevelList tllFooData = createTll(TLL_FOO_KEY);

        final WriteTransaction transaction = dataBroker.newWriteOnlyTransaction();
        transaction.put(LogicalDatastoreType.CONFIGURATION, FOO_PATH, tllFooData);
        transaction.submit().get(5, TimeUnit.SECONDS);

        Optional<TopLevelList> readedData = dataBroker.newReadOnlyTransaction().read(
                LogicalDatastoreType.CONFIGURATION, FOO_PATH).checkedGet(5, TimeUnit.SECONDS);
        assertTrue(readedData.isPresent());
        assertEquals(tllFooData.getKey(), readedData.get().getKey());

        TopLevelList nodeBarData = createTll(TLL_BAR_KEY);
        TopLevelList nodeBazData = createTll(TLL_BAZ_KEY);

        final WriteTransaction insertMoreTr = dataBroker.newWriteOnlyTransaction();
        insertMoreTr.put(LogicalDatastoreType.CONFIGURATION, BAR_PATH, nodeBarData);
        insertMoreTr.put(LogicalDatastoreType.CONFIGURATION, BAZ_PATH, nodeBazData);
        insertMoreTr.submit().get(5, TimeUnit.SECONDS);

        Optional<Top> top = dataBroker.newReadOnlyTransaction().read(LogicalDatastoreType.CONFIGURATION, TOP_PATH)
                .checkedGet(5, TimeUnit.SECONDS);
        assertTrue(top.isPresent());
        assertEquals(3, top.get().getTopLevelList().size());

         // We create transaction no 2
        final WriteTransaction removalTransaction = dataBroker.newWriteOnlyTransaction();

         // We remove node 1
        removalTransaction.delete(LogicalDatastoreType.CONFIGURATION, BAR_PATH);

         // We commit transaction
        removalTransaction.submit().get(5, TimeUnit.SECONDS);

        Optional<TopLevelList> readedData2 = dataBroker.newReadOnlyTransaction().read(
                LogicalDatastoreType.CONFIGURATION, BAR_PATH).checkedGet(5, TimeUnit.SECONDS);
        assertFalse(readedData2.isPresent());
    }

    private static TopLevelList createTll(final TopLevelListKey key) {
        TopLevelListBuilder ret = new TopLevelListBuilder();
        ret.setKey(key);
        return ret.build();
    }
}
