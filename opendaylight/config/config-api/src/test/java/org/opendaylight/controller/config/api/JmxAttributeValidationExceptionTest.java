/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import com.google.common.collect.Lists;
import java.nio.file.AccessDeniedException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.hamcrest.CoreMatchers;
import org.junit.Test;

public class JmxAttributeValidationExceptionTest {

    private final JmxAttribute jmxAttribute = new JmxAttribute("attr1");

    @Test
    public void testJmxAttributeValidationExceptionElement() throws Exception {
        JmxAttribute attributeName = new JmxAttribute("attr_name");
        JmxAttributeValidationException e = new JmxAttributeValidationException(attributeName);
        assertThat(e.getAttributeNames(), CoreMatchers.hasItem(attributeName));
    }

    @Test
    public void testJmxAttributeValidationExceptionList() throws Exception {
        List<JmxAttribute> attributeNames = new ArrayList<>();
        attributeNames.add(new JmxAttribute("att1"));
        attributeNames.add(new JmxAttribute("att2"));
        attributeNames.add(new JmxAttribute("att3"));
        JmxAttributeValidationException e = new JmxAttributeValidationException(attributeNames);
        assertEquals(e.getAttributeNames(), attributeNames);
    }

    @Test
    public void testJmxAttributeValidationExceptionList2() throws Exception {
        List<JmxAttribute> attributeNames = new ArrayList<>();
        attributeNames.add(new JmxAttribute("att1"));
        attributeNames.add(new JmxAttribute("att2"));
        attributeNames.add(new JmxAttribute("att3"));
        JmxAttributeValidationException e = new JmxAttributeValidationException("exception str",
                new AccessDeniedException(""), attributeNames);
        assertEquals(e.getAttributeNames(), attributeNames);
    }

    @Test
    public void testJmxAttributeValidationExceptionJmxElement() throws Exception {
        JmxAttribute attributeName = new JmxAttribute("attr_name");
        JmxAttributeValidationException e = new JmxAttributeValidationException("exception str",
                new AccessDeniedException(""), attributeName);
        assertEquals(e.getAttributeNames(), Arrays.asList(attributeName));
    }

    @Test
    public void testCheckNotNull() throws Exception {
        try {
            JmxAttributeValidationException.checkNotNull(false, jmxAttribute);
        } catch (JmxAttributeValidationException e) {
            assertJmxEx(e, jmxAttribute.getAttributeName() + " " + "message", jmxAttribute);
        }
    }

    @Test
    public void testCheckCondition() throws Exception {
        JmxAttributeValidationException.checkCondition(true, "message", jmxAttribute);
    }

    @Test(expected = JmxAttributeValidationException.class)
    public void testJmxAttributeValidationException() throws Exception {
        JmxAttributeValidationException.wrap(new Exception("tmp"), jmxAttribute);
    }

    @Test(expected = JmxAttributeValidationException.class)
    public void testJmxAttributeValidationException2() throws Exception {
        JmxAttributeValidationException.wrap(new Exception("tmp"), "message", jmxAttribute);
    }

    @Test(expected = JmxAttributeValidationException.class)
    public void testCheckConditionFalse() throws Exception {
        JmxAttributeValidationException.checkCondition(false, "message", jmxAttribute);
    }

    private void assertJmxEx(final JmxAttributeValidationException e, final String message, final JmxAttribute... attrNames) {
        assertEquals(message, e.getMessage());
        assertEquals(Lists.newArrayList(attrNames), e.getAttributeNames());
    }
}