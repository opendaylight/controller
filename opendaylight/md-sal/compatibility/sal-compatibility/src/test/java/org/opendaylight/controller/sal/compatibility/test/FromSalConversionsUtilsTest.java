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
import org.opendaylight.controller.sal.compatibility.FromSalConversionsUtils;

/**
 * test of {@link FromSalConversionsUtils}
 */
public class FromSalConversionsUtilsTest {

    /**
     * Test method for {@link org.opendaylight.controller.sal.compatibility.FromSalConversionsUtils#dscpToTos(int)}.
     */
    @Test
    public void testDscpToTos() {
        Assert.assertEquals(0, FromSalConversionsUtils.dscpToTos(0));
        Assert.assertEquals(4, FromSalConversionsUtils.dscpToTos(1));
        Assert.assertEquals(252, FromSalConversionsUtils.dscpToTos(63));
        Assert.assertEquals(256, FromSalConversionsUtils.dscpToTos(64));
        Assert.assertEquals(-4, FromSalConversionsUtils.dscpToTos(-1));
    }

}
