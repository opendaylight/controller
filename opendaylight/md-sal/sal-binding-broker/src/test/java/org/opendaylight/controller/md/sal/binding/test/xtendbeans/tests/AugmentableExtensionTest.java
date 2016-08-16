/*
 * Copyright (c) 2016 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.binding.test.xtendbeans.tests;

import static org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType.OPERATIONAL;

import ch.vorburger.xtendbeans.AssertBeans;
import java.util.Map;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.test.AbstractDataBrokerTest;
import org.opendaylight.controller.md.sal.binding.test.tests.DataBrokerUtils;
import org.opendaylight.controller.md.sal.binding.test.xtendbeans.AssertDataObjects;
import org.opendaylight.controller.md.sal.binding.test.xtendbeans.AugmentableExtension;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.Top;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.two.level.list.TopLevelList;
import org.opendaylight.yangtools.yang.binding.Augmentation;

/**
 * Test for {@link AugmentableExtension}.
 *
 * @author Michael Vorburger
 */
public class AugmentableExtensionTest extends AbstractDataBrokerTest {

    private AugmentableExtension augmentableExtension = new AugmentableExtension();

    @Test
    public void testAugmentableExtensionOnYangObjectByBuilder() {
        TopLevelList topLevelList = ExampleYangObjects.topLevelList().getValue();
        Map<Class<? extends Augmentation<?>>, Augmentation<?>> augmentations = augmentableExtension.getAugmentations(topLevelList);
        AssertBeans.assertEqualByText("#{\n" +
                "    org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.augment.rev140709.TreeComplexUsesAugment -> (new TreeComplexUsesAugmentBuilder => [\n" +
                "        containerWithUses = (new ContainerWithUsesBuilder => [\n" +
                "            leafFromGrouping = \"foo\"\n" +
                "        ]).build()\n" +
                "    ]).build()\n" +
                "}", augmentations);
    }

    @Test
    public void testAugmentableExtensionWithDataBroker() throws Exception {
        DataBrokerUtils dataBrokerUtils = new DataBrokerUtils(getDataBroker());
        // TODO needed? dataBrokerUtils.write(OPERATIONAL, ExampleYangObjects.topEmpty());
        dataBrokerUtils.write(OPERATIONAL, ExampleYangObjects.topLevelList());

        Top actualTop = dataBrokerUtils.read(OPERATIONAL, Top.class);
        AssertBeans.assertEqualByText("#{\n}", augmentableExtension.getAugmentations(actualTop));

        TopLevelList topLevelList = actualTop.getTopLevelList().get(0);
        AssertDataObjects.assertEqualByText("#{\n" +
                "    TreeComplexUsesAugment -> new TreeComplexUsesAugmentBuilder >> [\n" +
                "        containerWithUses = new ContainerWithUsesBuilder >> [\n" +
                "            leafFromGrouping = \"foo\"\n" +
                "        ]\n" +
                "    ]\n" +
                "}", augmentableExtension.getAugmentations(topLevelList));
    }
}
