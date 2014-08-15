package org.opendaylight.controller.config.api;

import static org.junit.Assert.assertEquals;

import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Test;

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
    public void testCheckNotNull() throws Exception {
        try {
            JmxAttributeValidationException.checkNotNull(false, "message", jmxAttribute);
        } catch (JmxAttributeValidationException e) {
            assertJmxEx(e, jmxAttribute.getAttributeName() + " " + "message", jmxAttribute);
        }
    }

    @Test
    public void testWrap() throws Exception {

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