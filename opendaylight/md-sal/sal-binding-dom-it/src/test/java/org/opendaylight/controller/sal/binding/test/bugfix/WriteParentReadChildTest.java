/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.binding.test.bugfix;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.sal.binding.api.data.DataModificationTransaction;
import org.opendaylight.controller.sal.binding.test.AbstractDataServiceTest;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.of.migration.test.model.rev150210.TllComplexAugment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.of.migration.test.model.rev150210.aug.grouping.List1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.of.migration.test.model.rev150210.aug.grouping.List1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.of.migration.test.model.rev150210.aug.grouping.List1Key;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.of.migration.test.model.rev150210.aug.grouping.list1.List11;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.of.migration.test.model.rev150210.aug.grouping.list1.List11Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.of.migration.test.model.rev150210.aug.grouping.list1.List11Key;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.Top;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.two.level.list.TopLevelList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.two.level.list.TopLevelListKey;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;

import com.google.common.collect.ImmutableList;

@SuppressWarnings("deprecation")
public class WriteParentReadChildTest extends AbstractDataServiceTest {

    private static final int LIST11_ID = 1234;
    private static final String LIST1_NAME = "bar";
    private static final String TLL_NAME = "foo";

    private static final TopLevelListKey TLL_KEY = new TopLevelListKey(TLL_NAME);
    private static final List11Key LIST11_KEY = new List11Key(LIST11_ID);
    private static final List1Key LIST1_KEY = new List1Key(LIST1_NAME);

    private static final InstanceIdentifier<TopLevelList> TLL_INSTANCE_ID_BA = InstanceIdentifier.builder(Top.class) //
            .child(TopLevelList.class, TLL_KEY).toInstance();

    private static final InstanceIdentifier<List1> LIST1_INSTANCE_ID_BA = //
            TLL_INSTANCE_ID_BA.builder() //
            .augmentation(TllComplexAugment.class).child(List1.class, LIST1_KEY).build();

    private static final InstanceIdentifier<? extends DataObject> LIST11_INSTANCE_ID_BA = //
            LIST1_INSTANCE_ID_BA.child(List11.class, LIST11_KEY);
    /**
     *
     * The scenario tests writing parent node, which also contains child items
     * and then reading child directly, by specifying path to the child.
     *
     * Expected behaviour is child is returned.
     *
     * @throws Exception
     */
    @Test
    public void writeParentReadChild() throws Exception {

        DataModificationTransaction modification = baDataService.beginTransaction();

        List11 list11 = new List11Builder() //
                .setKey(LIST11_KEY) //
                .setAttrStr("primary")
                .build();

        List1 list1 = new List1Builder()
            .setKey(LIST1_KEY)
            .setList11(ImmutableList.of(list11))
        .build();

        modification.putConfigurationData(LIST1_INSTANCE_ID_BA, list1);
        RpcResult<TransactionStatus> ret = modification.commit().get();
        assertNotNull(ret);
        assertEquals(TransactionStatus.COMMITED, ret.getResult());

        DataObject readList1 = baDataService.readConfigurationData(LIST1_INSTANCE_ID_BA);
        assertNotNull("Readed table should not be nul.", readList1);
        assertTrue(readList1 instanceof List1);

        DataObject readList11 = baDataService.readConfigurationData(LIST11_INSTANCE_ID_BA);
        assertNotNull("Readed flow should not be null.",readList11);
        assertTrue(readList11 instanceof List11);
        assertEquals(list11, readList11);

    }
}
