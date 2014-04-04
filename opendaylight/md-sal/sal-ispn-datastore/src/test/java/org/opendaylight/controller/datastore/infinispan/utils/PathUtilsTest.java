package org.opendaylight.controller.datastore.infinispan.utils;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PathUtilsTest {

    private static final String PARENT_PATH = "/(urn:TBD:params:xml:ns:yang:network-topology?revision=2013-10-21)network-topology" +
                "/(urn:TBD:params:xml:ns:yang:network-topology?revision=2013-10-21)topology";

    private static final String CHILD_PATH = "/(urn:TBD:params:xml:ns:yang:network-topology?revision=2013-10-21)network-topology" +
            "/(urn:TBD:params:xml:ns:yang:network-topology?revision=2013-10-21)topology" +
            "/(urn:TBD:params:xml:ns:yang:network-topology?revision=2013-10-21)topology[{(urn:TBD:params:xml:ns:yang:network-topology?revision=2013-10-21)topology-id=flow:1}]";

    @Test
    public void testGetParentPath(){
        String parentPath = PathUtils.getParentPath(CHILD_PATH);

        assertTrue(parentPath.length() < CHILD_PATH.length());

        assertEquals(PARENT_PATH, parentPath);
    }
}
