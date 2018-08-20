/*
 * Copyright (c) 2014, 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.md.sal.binding.impl.test;

import static org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType.CONFIGURATION;
import static org.opendaylight.controller.md.sal.test.model.util.ListsBindingUtils.TOP_FOO_KEY;
import static org.opendaylight.controller.md.sal.test.model.util.ListsBindingUtils.complexUsesAugment;
import static org.opendaylight.controller.md.sal.test.model.util.ListsBindingUtils.leafOnlyUsesAugment;
import static org.opendaylight.controller.md.sal.test.model.util.ListsBindingUtils.path;
import static org.opendaylight.controller.md.sal.test.model.util.ListsBindingUtils.top;
import static org.opendaylight.controller.md.sal.test.model.util.ListsBindingUtils.topLevelList;

import com.google.common.collect.ImmutableSet;
import java.util.Set;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.binding.test.AbstractDataTreeChangeListenerTest;
import org.opendaylight.mdsal.binding.spec.reflect.BindingReflections;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.augment.rev140709.TreeComplexUsesAugment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.augment.rev140709.TreeLeafOnlyUsesAugment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.augment.rev140709.complex.from.grouping.ListViaUsesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.Top;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.two.level.list.TopLevelList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.two.level.list.TopLevelListKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.YangModuleInfo;

public class Bug1418AugmentationTest extends AbstractDataTreeChangeListenerTest {
    private static final InstanceIdentifier<Top> TOP = InstanceIdentifier.create(Top.class);
    private static final InstanceIdentifier<TopLevelList> TOP_FOO = TOP.child(TopLevelList.class, TOP_FOO_KEY);
    private static final InstanceIdentifier<TreeLeafOnlyUsesAugment> SIMPLE_AUGMENT =
            TOP.child(TopLevelList.class, TOP_FOO_KEY).augmentation(TreeLeafOnlyUsesAugment.class);
    private static final InstanceIdentifier<TreeComplexUsesAugment> COMPLEX_AUGMENT =
            TOP.child(TopLevelList.class, TOP_FOO_KEY).augmentation(TreeComplexUsesAugment.class);
    private static final ListViaUsesKey LIST_VIA_USES_KEY =
            new ListViaUsesKey("list key");
    private static final ListViaUsesKey LIST_VIA_USES_KEY_MOD =
            new ListViaUsesKey("list key modified");

    @Override
    protected Set<YangModuleInfo> getModuleInfos() throws Exception {
        return ImmutableSet.of(BindingReflections.getModuleInfo(Top.class),
                BindingReflections.getModuleInfo(TreeComplexUsesAugment.class),
                BindingReflections.getModuleInfo(TreeLeafOnlyUsesAugment.class));
    }

    @Test
    public void leafOnlyAugmentationCreatedTest() {
        TreeLeafOnlyUsesAugment leafOnlyUsesAugment = leafOnlyUsesAugment("test leaf");
        final TestListener<TreeLeafOnlyUsesAugment> listener = createListener(CONFIGURATION, SIMPLE_AUGMENT,
                added(path(TOP_FOO_KEY, TreeLeafOnlyUsesAugment.class), leafOnlyUsesAugment));

        WriteTransaction writeTx = getDataBroker().newWriteOnlyTransaction();
        writeTx.put(CONFIGURATION, TOP, top());
        writeTx.put(CONFIGURATION, TOP_FOO, topLevelList(new TopLevelListKey(TOP_FOO_KEY)));
        writeTx.put(CONFIGURATION, SIMPLE_AUGMENT, leafOnlyUsesAugment);
        assertCommit(writeTx.submit());

        listener.verify();
    }

    @Test
    public void leafOnlyAugmentationUpdatedTest() {
        WriteTransaction writeTx = getDataBroker().newWriteOnlyTransaction();
        writeTx.put(CONFIGURATION, TOP, top());
        writeTx.put(CONFIGURATION, TOP_FOO, topLevelList(new TopLevelListKey(TOP_FOO_KEY)));
        TreeLeafOnlyUsesAugment leafOnlyUsesAugmentBefore = leafOnlyUsesAugment("test leaf");
        writeTx.put(CONFIGURATION, SIMPLE_AUGMENT, leafOnlyUsesAugmentBefore);
        assertCommit(writeTx.submit());

        TreeLeafOnlyUsesAugment leafOnlyUsesAugmentAfter = leafOnlyUsesAugment("test leaf changed");
        final TestListener<TreeLeafOnlyUsesAugment> listener = createListener(CONFIGURATION, SIMPLE_AUGMENT,
                added(path(TOP_FOO_KEY, TreeLeafOnlyUsesAugment.class), leafOnlyUsesAugmentBefore),
                replaced(path(TOP_FOO_KEY, TreeLeafOnlyUsesAugment.class), leafOnlyUsesAugmentBefore,
                    leafOnlyUsesAugmentAfter));

        writeTx = getDataBroker().newWriteOnlyTransaction();
        writeTx.put(CONFIGURATION, SIMPLE_AUGMENT, leafOnlyUsesAugmentAfter);
        assertCommit(writeTx.submit());

        listener.verify();
    }

    @Test
    public void leafOnlyAugmentationDeletedTest() {
        WriteTransaction writeTx = getDataBroker().newWriteOnlyTransaction();
        writeTx.put(CONFIGURATION, TOP, top());
        writeTx.put(CONFIGURATION, TOP_FOO, topLevelList(new TopLevelListKey(TOP_FOO_KEY)));
        TreeLeafOnlyUsesAugment leafOnlyUsesAugment = leafOnlyUsesAugment("test leaf");
        writeTx.put(CONFIGURATION, SIMPLE_AUGMENT, leafOnlyUsesAugment);
        assertCommit(writeTx.submit());

        final TestListener<TreeLeafOnlyUsesAugment> listener = createListener(CONFIGURATION, SIMPLE_AUGMENT,
                added(path(TOP_FOO_KEY, TreeLeafOnlyUsesAugment.class), leafOnlyUsesAugment),
                deleted(path(TOP_FOO_KEY, TreeLeafOnlyUsesAugment.class), leafOnlyUsesAugment));

        writeTx = getDataBroker().newWriteOnlyTransaction();
        writeTx.delete(CONFIGURATION, SIMPLE_AUGMENT);
        assertCommit(writeTx.submit());

        listener.verify();
    }

    @Test
    public void complexAugmentationCreatedTest() {
        TreeComplexUsesAugment complexUsesAugment = complexUsesAugment(LIST_VIA_USES_KEY);
        final TestListener<TreeComplexUsesAugment>  listener = createListener(CONFIGURATION, COMPLEX_AUGMENT,
                added(path(TOP_FOO_KEY, TreeComplexUsesAugment.class), complexUsesAugment));

        WriteTransaction writeTx = getDataBroker().newWriteOnlyTransaction();
        writeTx.put(CONFIGURATION, TOP, top());
        writeTx.put(CONFIGURATION, TOP_FOO, topLevelList(new TopLevelListKey(TOP_FOO_KEY)));
        writeTx.put(CONFIGURATION, COMPLEX_AUGMENT, complexUsesAugment);
        assertCommit(writeTx.submit());

        listener.verify();
    }

    @Test
    public void complexAugmentationUpdatedTest() {
        WriteTransaction writeTx = getDataBroker().newWriteOnlyTransaction();
        writeTx.put(CONFIGURATION, TOP, top());
        writeTx.put(CONFIGURATION, TOP_FOO, topLevelList(new TopLevelListKey(TOP_FOO_KEY)));
        TreeComplexUsesAugment complexUsesAugmentBefore = complexUsesAugment(LIST_VIA_USES_KEY);
        writeTx.put(CONFIGURATION, COMPLEX_AUGMENT, complexUsesAugmentBefore);
        assertCommit(writeTx.submit());

        TreeComplexUsesAugment complexUsesAugmentAfter = complexUsesAugment(LIST_VIA_USES_KEY_MOD);

        final TestListener<TreeComplexUsesAugment> listener = createListener(CONFIGURATION, COMPLEX_AUGMENT,
                added(path(TOP_FOO_KEY, TreeComplexUsesAugment.class), complexUsesAugmentBefore),
                replaced(path(TOP_FOO_KEY, TreeComplexUsesAugment.class), complexUsesAugmentBefore,
                        complexUsesAugmentAfter));

        writeTx = getDataBroker().newWriteOnlyTransaction();
        writeTx.put(CONFIGURATION, COMPLEX_AUGMENT, complexUsesAugmentAfter);
        assertCommit(writeTx.submit());

        listener.verify();
    }
}
