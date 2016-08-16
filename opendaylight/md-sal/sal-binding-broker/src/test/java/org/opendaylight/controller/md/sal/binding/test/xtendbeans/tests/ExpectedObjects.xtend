/*
 * Copyright (c) 2016 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.binding.test.xtendbeans.tests

import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.augment.rev140709.TreeComplexUsesAugment
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.augment.rev140709.TreeComplexUsesAugmentBuilder
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.augment.rev140709.complex.from.grouping.ContainerWithUsesBuilder
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.two.level.list.TopLevelListBuilder
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.two.level.list.TopLevelListKey

import static extension org.opendaylight.controller.md.sal.binding.test.xtendbeans.XtendBuilderExtensions.operator_doubleGreaterThan
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.TopBuilder

class ExpectedObjects {

    def static topLevelList() {
        new TopLevelListBuilder >> [
            key = new TopLevelListKey("foo")
            name = "foo"
            addAugmentation(TreeComplexUsesAugment, new TreeComplexUsesAugmentBuilder >> [
                containerWithUses = new ContainerWithUsesBuilder >> [
                    leafFromGrouping = "foo"
                ]
            ])
        ]
    }

    def static top() {
        new TopBuilder >> [
            topLevelList = #[
                new TopLevelListBuilder >> [
                    name = "foo"
                    addAugmentation(TreeComplexUsesAugment, new TreeComplexUsesAugmentBuilder >> [
                        containerWithUses = new ContainerWithUsesBuilder >> [
                            leafFromGrouping = "foo"
                        ]
                    ])
                ]
            ]
        ]
    }
}
