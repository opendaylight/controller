package org.opendaylight.controller.config.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class AttributeEntryTest {

    private AttributeEntry attributeEntryClient;
    private final String key = "myKey";
    private final String description = "myDescription";
    private final String type = "myType";
    private final boolean boolValue = false;

    @Before
    public void setUp() throws Exception {
        attributeEntryClient = new AttributeEntry("myKey", "myDescription", null, "myType", false);
    }

    @Test
    public void testAttributeEntryGetters() throws Exception{
        assertEquals(key, attributeEntryClient.getKey());
        assertEquals(description, attributeEntryClient.getDescription());
        final Object value = attributeEntryClient.getValue();
        assertNull(value);
        assertEquals(type, attributeEntryClient.getType());
        assertEquals(boolValue, attributeEntryClient.isRw());
    }
}
