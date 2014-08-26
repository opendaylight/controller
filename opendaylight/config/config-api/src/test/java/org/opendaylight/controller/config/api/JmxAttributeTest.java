package org.opendaylight.controller.config.api;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

public class JmxAttributeTest {

    @Test
    public void testJmxAttribute() throws Exception {
        JmxAttribute attr = new JmxAttribute("test");
        assertEquals("test", attr.getAttributeName());
    }

    @Test
    public void testToString() throws Exception {
        JmxAttribute attr = new JmxAttribute("test");
        assertEquals(attr.toString(), new JmxAttribute("test").toString());
    }

    @Test(expected = Exception.class)
    public void testJmxAttributeInvalid() throws Exception {
        JmxAttribute attr = new JmxAttribute(null);
    }

    @Test
    public void testJmxAttributeEqual() throws Exception {
        JmxAttribute a1 = new JmxAttribute("test_string");
        JmxAttribute a2 = new JmxAttribute("test_string");
        assertEquals(a1, a2);
    }

    @Test
    public void testJmxAttributeNotEqual() throws Exception {
        JmxAttribute a1 = new JmxAttribute("test_string");
        JmxAttribute a2 = new JmxAttribute("different");
        assertNotEquals(a1, a2);
    }

    @Test
    public void testJmxAttributeEqual2() throws Exception {
        JmxAttribute a1 = new JmxAttribute("test_string");
        assertNotNull(a1);
    }

    @Test
    public void testJmxAttributeHashCode() throws Exception {
        JmxAttribute a1 = new JmxAttribute("test_string");
        assertEquals(a1.hashCode(), new String("test_string").hashCode());
    }
}
