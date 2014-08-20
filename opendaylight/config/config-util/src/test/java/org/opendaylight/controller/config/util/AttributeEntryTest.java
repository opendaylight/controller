package org.opendaylight.controller.config.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class AttributeEntryTest {

    private AttributeEntry attributeEntryClient;
    private String key = "myKey";
    private String description = "myDescription";
    private Object value;
    private String type = "myType";
    private boolean boolValue = false;

    @Before
    public void setUp() throws Exception {
        attributeEntryClient = new AttributeEntry("myKey", "myDescription", null, "myType", false);
    }

    @After
    public void cleanUp() throws Exception {

    }

    @Test
    public void testAttributeEntryGetters() throws Exception{
        assertEquals(key, attributeEntryClient.getKey());
        assertEquals(description, attributeEntryClient.getDescription());
        value = attributeEntryClient.getValue();
        assertNull(value);
        assertEquals(type, attributeEntryClient.getType());
        assertEquals(boolValue, attributeEntryClient.isRw());
    }
}
