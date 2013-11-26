package org.opendaylight.controller.sal.restconf.impl.test;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ToJsonWithAugmentTest {

    /**
     * Test of json output when as input are specified composite node with empty
     * data + YANG file
     */
    @Test
    public void augmentedElementsToJson() {
        String jsonOutput = TestUtils.convertCompositeNodeDataAndYangToJson(
                TestUtils.loadCompositeNode("/yang-to-json-conversion/augmentation/xml/data.xml"),
                "/yang-to-json-conversion/augmentation", "/yang-to-json-conversion/augmentation/xml", "yang", "cont");

        assertTrue(jsonOutput.contains("\"augment-leaf:lf2\": \"lf2\""));
        assertTrue(jsonOutput.contains("\"augment-container:cont1\": {"));
        assertTrue(jsonOutput.contains("\"augment-container:lf11\": \"lf11\""));
        assertTrue(jsonOutput.contains("\"augment-list:lst1\": ["));
        assertTrue(jsonOutput.contains("\"augment-list:lf11\": \"lf1_1\""));
        assertTrue(jsonOutput.contains("\"augment-list:lf11\": \"lf1_2\""));
        assertTrue(jsonOutput.contains("\"augment-leaflist:lflst1\": ["));
    }
}
