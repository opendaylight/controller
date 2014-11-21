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
import static org.junit.Assert.assertNull;
import com.google.common.collect.ImmutableSet;
import org.junit.Test;
import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.sal.binding.api.data.DataModificationTransaction;
import org.opendaylight.controller.sal.binding.test.AbstractDataServiceTest;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.of.migration.test.model.rev150210.BitFlags;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.of.migration.test.model.rev150210.TllComplexAugment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.of.migration.test.model.rev150210.aug.grouping.List1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.of.migration.test.model.rev150210.aug.grouping.List1Key;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.of.migration.test.model.rev150210.aug.grouping.list1.List11;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.of.migration.test.model.rev150210.aug.grouping.list1.List11Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.of.migration.test.model.rev150210.aug.grouping.list1.List11Key;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.Top;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.two.level.list.TopLevelList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.two.level.list.TopLevelListKey;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;

public class FlagsSerializationTest extends AbstractDataServiceTest {
    private static final TopLevelListKey TLL_KEY = new TopLevelListKey("foo");
    private static final List11Key LIST11_KEY = new List11Key(1234);
    private static final List1Key LIST1_KEY = new List1Key("1");

    private static final InstanceIdentifier<TopLevelList> TLL_INSTANCE_ID_BA = InstanceIdentifier.builder(Top.class) //
            .child(TopLevelList.class, TLL_KEY).toInstance();

    private static final InstanceIdentifier<? extends DataObject> LIST11_INSTANCE_ID_BA = //
            TLL_INSTANCE_ID_BA.builder() //
            .augmentation(TllComplexAugment.class)
            .child(List1.class,LIST1_KEY)
            .child(List11.class, LIST11_KEY) //
            .toInstance();
    private static final QName LIST11_FLAGS_QNAME = QName.create(List11.QNAME, "flags");

    @Test
    public void testIndirectGeneration() throws Exception {

        BitFlags checkOverlapFlags = new BitFlags(true,false,false,false,false);
        ImmutableSet<String> domCheckOverlapFlags = ImmutableSet.<String>of("FLAG_FIVE");
        testFlags(checkOverlapFlags,domCheckOverlapFlags);



        BitFlags allFalseFlags = new BitFlags(false,false,false,false,false);
        ImmutableSet<String> domAllFalseFlags = ImmutableSet.<String>of();
        testFlags(allFalseFlags,domAllFalseFlags);

        BitFlags allTrueFlags = new BitFlags(true,true,true,true,true);
        ImmutableSet<String> domAllTrueFlags = ImmutableSet.<String>of("FLAG_ONE","FLAG_TWO","FLAG_THREE","FLAG_FOUR","FLAG_FIVE");
        testFlags(allTrueFlags,domAllTrueFlags);

        testFlags(null,null);



    }

    private void testFlags(final BitFlags flagsToTest, final ImmutableSet<String> domFlags) throws Exception {
        List11 list11 = createList11(flagsToTest);
        assertNotNull(list11);

        CompositeNode domList11 = biDataService.readConfigurationData(mappingService.toDataDom(LIST11_INSTANCE_ID_BA));

        assertNotNull(domList11);
        org.opendaylight.yangtools.yang.data.api.Node<?> readedFlags = domList11.getFirstSimpleByName(LIST11_FLAGS_QNAME);

        if(domFlags != null) {
            assertNotNull(readedFlags);
            assertEquals(domFlags,readedFlags.getValue());
        } else {
            assertNull(readedFlags);
        }
        assertEquals(flagsToTest, list11.getFlags());

        DataModificationTransaction transaction = baDataService.beginTransaction();
        transaction.removeConfigurationData(LIST11_INSTANCE_ID_BA);
        RpcResult<TransactionStatus> result = transaction.commit().get();
        assertEquals(TransactionStatus.COMMITED, result.getResult());

    }

    private List11 createList11(final BitFlags flagsToTest) throws Exception {

        DataModificationTransaction modification = baDataService.beginTransaction();

        List11Builder list11b = new List11Builder();

        list11b.setKey(LIST11_KEY);
        list11b.setAttrStr("list:1:1");

        list11b.setFlags(flagsToTest);

        modification.putConfigurationData(LIST11_INSTANCE_ID_BA, list11b.build());
        RpcResult<TransactionStatus> ret = modification.commit().get();
        assertNotNull(ret);
        assertEquals(TransactionStatus.COMMITED, ret.getResult());
        return (List11) baDataService.readConfigurationData(LIST11_INSTANCE_ID_BA);
    }
}
