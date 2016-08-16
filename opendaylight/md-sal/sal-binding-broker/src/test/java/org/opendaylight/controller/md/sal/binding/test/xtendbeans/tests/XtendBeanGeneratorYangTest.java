/*
 * Copyright (c) 2016 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.binding.test.xtendbeans.tests;

import ch.vorburger.xtendbeans.XtendBeanGenerator;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.test.xtendbeans.AssertDataObjects;

/**
 * Test the {@link XtendBeanGenerator} with YANG objects.
 *
 * @author Michael Vorburger
 */
public class XtendBeanGeneratorYangTest {

    private static final String HEADER = "import static extension org.opendaylight.controller.md.sal.binding.test.xtendbeans.XtendBuilderExtensions.operator_doubleGreaterThan\n\n";

    @Test
    public void testTop() {
        AssertDataObjects.assertEqualByText(HEADER + "new TopBuilder\n", ExampleYangObjects.topEmpty().getValue());
    }

    @Test
    public void testTopLevelList() {
        AssertDataObjects.assertEqualByText(HEADER + "new TopLevelListBuilder\n", ExampleYangObjects.topLevelList().getValue());
    }

}
