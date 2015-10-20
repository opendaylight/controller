package org.openflow.protocol;


import junit.framework.TestCase;

import org.junit.Test;
import org.openflow.protocol.statistics.OFStatisticsType;


public class OFStatisticsTypeTest extends TestCase {
    @Test
    public void testMapping() throws Exception {
        TestCase.assertEquals(OFStatisticsType.DESC,
                OFStatisticsType.valueOf((short) 0, OFType.STATS_REQUEST));
        TestCase.assertEquals(OFStatisticsType.QUEUE,
                OFStatisticsType.valueOf((short) 5, OFType.STATS_REQUEST));
        TestCase.assertEquals(OFStatisticsType.VENDOR,
                OFStatisticsType.valueOf((short) 0xffff, OFType.STATS_REQUEST));
    }
}
