/*
 * Copyright (c) 2016 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.binding.test.xtendbeans.tests;

import ch.vorburger.xtendbeans.XtendBeanGenerator;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test the {@link XtendBeanGenerator} with YANG objects.
 *
 * @author Michael Vorburger
 */
public class XtendBeanGeneratorYangTest {

    @Test
    public void testTop() {
        assertEqualXtendStringToBean("(new TopBuilder\n).build()", ExampleYangObjects.topEmpty().getValue());
    }

    @Test
    public void testTopLevelList() {
        assertEqualXtendStringToBean("..foo..", ExampleYangObjects.topLevelList().getValue());
    }

    private void assertEqualXtendStringToBean(String expectedString, Object actualBean) {
        String actualString = new XtendBeanGenerator().getExpression(actualBean);
        Assert.assertEquals(expectedString, actualString);
    }
}
