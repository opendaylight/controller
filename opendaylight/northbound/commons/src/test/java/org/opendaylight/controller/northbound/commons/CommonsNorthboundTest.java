/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
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
