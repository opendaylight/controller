/*
 * Copyright (c) 2016 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.binding.test.xtendbeans;

import ch.vorburger.xtendbeans.AssertBeans;
import org.junit.ComparisonFailure;
import org.opendaylight.yangtools.yang.binding.DataContainer;
import org.opendaylight.yangtools.yang.binding.DataObject;

/**
 * Assert equals utility for YANG {@link DataObject}s.
 *
 * @see AssertBeans
 *
 * @author Michael Vorburger
 */
public class AssertDataObjects {

    private final static XtendYangBeanGenerator generator = new XtendYangBeanGenerator();

    private AssertDataObjects() {
    }

    /**
     * Assert that an actual YANG DataObject (DataContainer) is equals to an expected one.
     *
     * @param expected expected YANG DataObject
     * @param actual the (root) DataContainer to check against <code>expected</code>
     *
     * @see AssertBeans#assertEqualBeans(Object, Object)
     */
    public static void assertEqualDataContainers(DataContainer expected, DataContainer actual) throws ComparisonFailure {
        String expectedText = generator.getExpression(expected);
        assertEqualByText(expectedText, actual);
    }

    public static void assertEqualByText(String expectedText, DataContainer actual) throws ComparisonFailure {
        String actualText = generator.getExpression(actual);
        if (!expectedText.equals(actualText)) {
            throw new ComparisonFailure("Expected and actual beans do not match", expectedText, actualText);
        }
    }

}
