package org.opendaylight.controller.config.api;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import com.google.common.collect.Lists;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.management.Query;

public class JmxAttributeValidationExceptionTest {

    private JmxAttribute jmxAttribute = new JmxAttribute("attr1");
    private JmxAttribute jmxAttribute2 = new JmxAttribute("attr2");

    @Before
    public void setUp() throws Exception {

    }

    @Test
    public void testGetAttributeNames() throws Exception {

    }

    @Test
    public void testJmxAttribute() throws Exception {
        JmxAttribute attr = new JmxAttribute("test");
        assertEquals("test", attr);
    }

    @Test(expected = Exception.class)
    public void testJmxAttributeInvalid() throws Exception {
            JmxAttribute attr = new JmxAttribute(null);
    }

    @Test
    public void testJmxAttributeEqual() throws Exception {
        JmxAttribute a1 = new JmxAttribute("test_string");
        JmxAttribute a2 = new JmxAttribute("test_string");
        if (a1.equals(a2)) {
            return;
        }
        fail("strings are not equal");
    }

    @Test
    public void testJmxAttributeEqual2() throws Exception {
        JmxAttribute a1 = new JmxAttribute("test_string");
        if (a1.equals(null) == false) {
            return;
        }
        fail("strings are not equal");
    }

    @Test
    public void testJmxAttributeHashCode() throws Exception {
        JmxAttribute a1 = new JmxAttribute("test_string");
        assertEquals(a1.hashCode(), new String("test_string").hashCode());
    }

    @Test
    public void testJmxAttributeValidationExceptionElement() throws Exception {
        JmxAttribute attributeName = new JmxAttribute("attr_name");
        JmxAttributeValidationException e = new JmxAttributeValidationException(attributeName);
        if (e.getAttributeNames().contains(new JmxAttribute("attr_name"))) {
            return;
        }
        fail("instance JmxAttributeValidationException does not contain expected value");
    }

    @Test
    public void testJmxAttributeValidationExceptionList() throws Exception {
        List attributeNames = new ArrayList<JmxAttribute>();
        attributeNames.add(new JmxAttribute("att1"));
        attributeNames.add(new JmxAttribute("att2"));
        attributeNames.add(new JmxAttribute("att3"));
        JmxAttributeValidationException e = new JmxAttributeValidationException(attributeNames);
        assertEquals(e.getAttributeNames(), attributeNames);
    }

    @Test
    public void testCheckNotNull() throws Exception {
        try {
            JmxAttributeValidationException.checkNotNull(false, "message", jmxAttribute);
        } catch (JmxAttributeValidationException e) {
            assertJmxEx(e, jmxAttribute.getAttributeName() + " " + "message", jmxAttribute);
        }
    }

    @Test
    public void testCheckCondition() throws Exception {
        try {
            JmxAttributeValidationException.checkCondition(false, "message", jmxAttribute);
        } catch (JmxAttributeValidationException e) {
            assertJmxEx(e, jmxAttribute.getAttributeName() + " " + "message", jmxAttribute);
        }
    }

    private void assertJmxEx(JmxAttributeValidationException e, String message, JmxAttribute... attrNames) {
        assertEquals(message, e.getMessage());
        assertEquals(Lists.newArrayList(attrNames), e.getAttributeNames());
    }
}