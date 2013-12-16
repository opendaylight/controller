package org.opendaylight.controller.sal.restconf.impl.cnsn.to.json.test;

import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.opendaylight.controller.sal.restconf.impl.test.TestUtils;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;

public class ToJsonWithAugmentTest {

    /**
     * Test of json output when as input are specified composite node with empty
     * data + YANG file
     */
    @Test
    public void augmentedElementsToJson() {

        CompositeNode compositeNode = TestUtils.loadCompositeNode("/cnsn-to-json/augmentation/xml/data.xml");
        String jsonOutput = TestUtils.convertCompositeNodeDataAndYangToJson(compositeNode,
                "/cnsn-to-json/augmentation", "/cnsn-to-json/augmentation/xml", "yang", "cont");

        assertTrue(jsonOutput.contains("\"augment-leaf:lf2\": \"lf2\""));
        assertTrue(jsonOutput.contains("\"augment-container:cont1\": {"));
        assertTrue(jsonOutput.contains("\"augment-container:lf11\": \"lf11\""));
        assertTrue(jsonOutput.contains("\"augment-list:lst1\": ["));
        assertTrue(jsonOutput.contains("\"augment-list:lf11\": \"lf1_1\""));
        assertTrue(jsonOutput.contains("\"augment-list:lf11\": \"lf1_2\""));
        assertTrue(jsonOutput.contains("\"augment-leaflist:lflst1\": ["));
    }
}
