/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.test.model.util;

import com.google.common.collect.ImmutableList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.augment.rev140709.TreeComplexUsesAugment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.augment.rev140709.TreeComplexUsesAugmentBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.augment.rev140709.TreeLeafOnlyUsesAugment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.augment.rev140709.TreeLeafOnlyUsesAugmentBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.augment.rev140709.complex.from.grouping.ListViaUses;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.augment.rev140709.complex.from.grouping.ListViaUsesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.augment.rev140709.complex.from.grouping.ListViaUsesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.Top;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.TopBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.two.level.list.TopLevelList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.two.level.list.TopLevelListBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.two.level.list.TopLevelListKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.two.level.list.top.level.list.NestedList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.two.level.list.top.level.list.NestedListKey;
import org.opendaylight.yangtools.yang.binding.Augmentation;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import java.util.Arrays;

public class ListsBindingUtils {

    private static final InstanceIdentifier<Top> TOP_PATH = InstanceIdentifier.create(Top.class);

    private ListsBindingUtils() {
        throw new UnsupportedOperationException();
    }

    public static final TopLevelListKey TOP_FOO_KEY = new TopLevelListKey("foo");
    public static final TopLevelListKey TOP_BAR_KEY = new TopLevelListKey("bar");
    public static final ListViaUsesKey USES_ONE_KEY = new ListViaUsesKey("one");
    public static final ListViaUsesKey USES_TWO_KEY = new ListViaUsesKey("two");


    public static InstanceIdentifier<TopLevelList> path(final TopLevelListKey key) {
        return TOP_PATH.child(TopLevelList.class, key);
    }

    public static InstanceIdentifier<NestedList> path(final TopLevelListKey top,final NestedListKey nested) {
        return path(top).child(NestedList.class, nested);
    }

    public static InstanceIdentifier<ListViaUses> path(final TopLevelListKey top,final ListViaUsesKey uses) {
        return path(top).augmentation(TreeComplexUsesAugment.class).child(ListViaUses.class, uses);
    }

    public static <T extends DataObject & Augmentation<TopLevelList>> InstanceIdentifier<T> path(final TopLevelListKey key, final Class<T> augmentation) {
        return path(key).augmentation(augmentation);
    }

    public static Top top(final TopLevelList... listItems) {
        return new TopBuilder().setTopLevelList(Arrays.asList(listItems)).build();
    }

    public static TopLevelList topLevelList(final TopLevelListKey key) {
        return new TopLevelListBuilder().setKey(key).build();
    }

    public static TopLevelList topLevelList(final TopLevelListKey key, final TreeComplexUsesAugment augment) {
        TopLevelListBuilder builder = new TopLevelListBuilder().setKey(key);
        builder.addAugmentation(TreeComplexUsesAugment.class, augment);
        return builder.build();
    }

    public static TreeComplexUsesAugment complexUsesAugment(final ListViaUsesKey... keys) {
        ImmutableList.Builder<ListViaUses> listViaUses = ImmutableList.<ListViaUses> builder();
        for (ListViaUsesKey key : keys) {
            listViaUses.add(new ListViaUsesBuilder().setKey(key).build());
        }
        return new TreeComplexUsesAugmentBuilder().setListViaUses(listViaUses.build()).build();
    }

    public static TreeLeafOnlyUsesAugment leafOnlyUsesAugment(String leafFromGroupingValue) {

        return new TreeLeafOnlyUsesAugmentBuilder().setLeafFromGrouping(leafFromGroupingValue).build();
    }

}
