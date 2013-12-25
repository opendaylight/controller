package org.opendaylight.controller.northbound.commons;

import org.junit.Assert;
import org.junit.Test;

public class CommonsNorthboundTest {

    @Test
    public void testRestMessages() {
        Assert.assertTrue(RestMessages.SUCCESS.toString().equals("Success"));
        Assert.assertTrue(RestMessages.INTERNALERROR.toString().equals(
                "Internal Error"));
        Assert.assertTrue(RestMessages.INVALIDDATA.toString().equals(
                "Data is invalid or conflicts with URI"));
    }

}
