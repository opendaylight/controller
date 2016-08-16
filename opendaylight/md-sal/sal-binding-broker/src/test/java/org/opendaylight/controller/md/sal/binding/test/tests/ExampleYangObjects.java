/*
 * Copyright (c) 2016 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.binding.test.tests;

import static org.opendaylight.controller.md.sal.test.model.util.ListsBindingUtils.TOP_FOO_KEY;
import static org.opendaylight.controller.md.sal.test.model.util.ListsBindingUtils.path;
import java.util.AbstractMap;
import java.util.AbstractMap.SimpleImmutableEntry;
import org.opendaylight.controller.md.sal.test.model.util.ListsBindingUtils;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.augment.rev140709.TreeComplexUsesAugment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.augment.rev140709.TreeComplexUsesAugmentBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.augment.rev140709.complex.from.grouping.ContainerWithUsesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.Top;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.TopBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.two.level.list.TopLevelList;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class ExampleYangObjects {

    public static AbstractMap.SimpleImmutableEntry<InstanceIdentifier<Top>, Top> topEmpty() {
        return new SimpleImmutableEntry<>(InstanceIdentifier.create(Top.class), new TopBuilder().build());
    }

    public static AbstractMap.SimpleImmutableEntry<InstanceIdentifier<TopLevelList>, TopLevelList> topLevelList() {
        TreeComplexUsesAugment fooAugment = new TreeComplexUsesAugmentBuilder()
                .setContainerWithUses(new ContainerWithUsesBuilder().setLeafFromGrouping("foo").build()).build();
        return new SimpleImmutableEntry<>(path(TOP_FOO_KEY), ListsBindingUtils.topLevelList(TOP_FOO_KEY, fooAugment));
    }
}
