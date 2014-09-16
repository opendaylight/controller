/**
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.compatibility.test;

import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.controller.sal.compatibility.ToSalConversionsUtils;

/**
 * test of {@link ToSalConversionsUtils}
 */
public class ToSalConversionsUtilsTest {

    /**
     * Test method for {@link org.opendaylight.controller.sal.compatibility.ToSalConversionsUtils#tosToNwDscp(int)}.
     */
    @Test
    public void testTosToNwDscp() {
        Assert.assertEquals(0, ToSalConversionsUtils.tosToNwDscp(0));
        Assert.assertEquals(0, ToSalConversionsUtils.tosToNwDscp(1));
        Assert.assertEquals(1, ToSalConversionsUtils.tosToNwDscp(4));
        Assert.assertEquals(63, ToSalConversionsUtils.tosToNwDscp(252));
        Assert.assertEquals(63, ToSalConversionsUtils.tosToNwDscp(253));
        Assert.assertEquals(-1, ToSalConversionsUtils.tosToNwDscp(-1));
    }
}
