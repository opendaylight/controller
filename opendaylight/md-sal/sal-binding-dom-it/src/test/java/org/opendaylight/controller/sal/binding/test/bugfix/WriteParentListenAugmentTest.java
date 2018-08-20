/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.binding.test.bugfix;

import static org.junit.Assert.assertEquals;
import static org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType.OPERATIONAL;

import com.google.common.collect.ImmutableSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.binding.test.AbstractDataTreeChangeListenerTest;
import org.opendaylight.mdsal.binding.spec.reflect.BindingReflections;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.augment.rev140709.TreeComplexUsesAugment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.augment.rev140709.TreeComplexUsesAugmentBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.augment.rev140709.complex.from.grouping.ContainerWithUsesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.Top;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.two.level.list.TopLevelList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.two.level.list.TopLevelListBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.two.level.list.TopLevelListKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.YangModuleInfo;

public class WriteParentListenAugmentTest extends AbstractDataTreeChangeListenerTest {

    private static final String TLL_NAME = "foo";

    private static final TopLevelListKey TLL_KEY = new TopLevelListKey(TLL_NAME);
    private static final InstanceIdentifier<TopLevelList> TLL_INSTANCE_ID_BA = InstanceIdentifier.builder(Top.class)
            .child(TopLevelList.class, TLL_KEY).build();

    private static final InstanceIdentifier<TreeComplexUsesAugment> AUGMENT_WILDCARDED_PATH = InstanceIdentifier
            .builder(Top.class).child(TopLevelList.class).augmentation(TreeComplexUsesAugment.class).build();

    private static final InstanceIdentifier<TreeComplexUsesAugment> AUGMENT_TLL_PATH = InstanceIdentifier
            .builder(Top.class).child(TopLevelList.class, TLL_KEY).augmentation(TreeComplexUsesAugment.class).build();

    @Override
    protected Set<YangModuleInfo> getModuleInfos() throws Exception {
        return ImmutableSet.of(BindingReflections.getModuleInfo(Top.class),
                BindingReflections.getModuleInfo(TreeComplexUsesAugment.class));
    }

    @Test
    public void writeNodeListenAugment() throws Exception {

        DataBroker dataBroker = getDataBroker();

        final TreeComplexUsesAugment treeComplexUsesAugment = treeComplexUsesAugment("one");

        final TestListener<TreeComplexUsesAugment> listener = createListener(OPERATIONAL, AUGMENT_WILDCARDED_PATH,
                added(AUGMENT_TLL_PATH, treeComplexUsesAugment));

        final WriteTransaction transaction = dataBroker.newWriteOnlyTransaction();

        TopLevelList tll = new TopLevelListBuilder().withKey(TLL_KEY)
                .addAugmentation(TreeComplexUsesAugment.class, treeComplexUsesAugment).build();
        transaction.put(OPERATIONAL, TLL_INSTANCE_ID_BA, tll, true);
        transaction.submit().get(5, TimeUnit.SECONDS);

        listener.verify();

        final WriteTransaction transaction2 = dataBroker.newWriteOnlyTransaction();
        transaction2.put(OPERATIONAL, AUGMENT_TLL_PATH, treeComplexUsesAugment("two"));
        transaction2.submit().get(5, TimeUnit.SECONDS);

        TreeComplexUsesAugment readedAug = dataBroker.newReadOnlyTransaction().read(
                OPERATIONAL, AUGMENT_TLL_PATH).get(5, TimeUnit.SECONDS).get();
        assertEquals("two", readedAug.getContainerWithUses().getLeafFromGrouping());
    }

    private static TreeComplexUsesAugment treeComplexUsesAugment(final String value) {
        return new TreeComplexUsesAugmentBuilder()
                .setContainerWithUses(new ContainerWithUsesBuilder().setLeafFromGrouping(value).build())
                .build();
    }
}
