/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.binding.impl.test;

import com.google.common.base.Optional;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.binding.impl.CachedForwardedBrokerBuilder;
import org.opendaylight.controller.md.sal.binding.test.AbstractDataBrokerTest;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.Top;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.two.level.list.TopLevelList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.two.level.list.TopLevelListKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.two.level.list.top.level.list.NestedList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.two.level.list.top.level.list.NestedListBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.two.level.list.top.level.list.NestedListKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertTrue;
import static org.opendaylight.controller.md.sal.test.model.util.ListsBindingUtils.top;
import static org.opendaylight.controller.md.sal.test.model.util.ListsBindingUtils.topLevelList;

public class CachedBindingBokerTest extends AbstractDataBrokerTest {

    public static final TopLevelListKey TLL_ONE_KEY = new TopLevelListKey("one");
    public static final TopLevelListKey TLL_TWO_KEY = new TopLevelListKey("two");
    public static final NestedListKey NESTED_ONE_KEY = new NestedListKey("nested_one");
    public static final NestedListKey NESTED_TWO_KEY = new NestedListKey("nested_two");

    public static final InstanceIdentifier<Top> TOP_PATH = InstanceIdentifier.create(Top.class);
    public static final InstanceIdentifier<TopLevelList> TLL_ONE_PATH = TOP_PATH.builder()
            .child(TopLevelList.class, TLL_ONE_KEY).build();
    public static final InstanceIdentifier<TopLevelList> TLL_TWO_PATH = TOP_PATH.builder()
            .child(TopLevelList.class, TLL_TWO_KEY).build();
    public static final InstanceIdentifier<NestedList> NL_1_PATH =TLL_ONE_PATH.builder()
            .child(NestedList.class, NESTED_ONE_KEY).build();
    public static final InstanceIdentifier<NestedList> NL_2_PATH =TLL_TWO_PATH.builder()
            .child(NestedList.class, NESTED_ONE_KEY).build();


    @Test
    public void cachedBrokerTest() throws ExecutionException, InterruptedException {
        DataBroker cachedBroker = CachedForwardedBrokerBuilder.create(getDataBroker(), TOP_PATH);
        WriteTransaction writeTx = cachedBroker.newWriteOnlyTransaction();
        writeTx.put(LogicalDatastoreType.CONFIGURATION, TOP_PATH, top(topLevelList(new TopLevelListKey("one")), topLevelList(new TopLevelListKey("two"))));
        writeTx.put(LogicalDatastoreType.CONFIGURATION, NL_1_PATH, nestedList(NESTED_ONE_KEY));
        assertCommit(writeTx.submit());
        ReadOnlyTransaction readOnlyTx = cachedBroker.newReadOnlyTransaction();
        Optional<NestedList> nestedListPotential = readOnlyTx.read(LogicalDatastoreType.CONFIGURATION, NL_1_PATH).get();
        assertTrue(nestedListPotential.isPresent());
        writeTx = cachedBroker.newReadWriteTransaction();
        // here nested list should be replaced by cached object
        writeTx.put(LogicalDatastoreType.CONFIGURATION, NL_2_PATH, nestedList(NESTED_ONE_KEY));
        assertCommit(writeTx.submit());
        readOnlyTx = cachedBroker.newReadOnlyTransaction();
        Optional<NestedList> nestedListPotential2 = readOnlyTx.read(LogicalDatastoreType.CONFIGURATION, NL_2_PATH).get();
        assertTrue(nestedListPotential2.isPresent());
    }

    public NestedList nestedList(NestedListKey nestedListKey) {
        NestedListBuilder nlBuilder = new NestedListBuilder();
        return nlBuilder.setKey(nestedListKey).build();
    }

}
