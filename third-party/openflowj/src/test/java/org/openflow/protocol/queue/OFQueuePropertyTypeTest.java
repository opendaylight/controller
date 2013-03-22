
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.openflow.protocol.queue;


import junit.framework.TestCase;

import org.junit.Test;


public class OFQueuePropertyTypeTest extends TestCase {
    @Test
    public void testMapping() throws Exception {
        TestCase.assertEquals(OFQueuePropertyType.NONE,
                OFQueuePropertyType.valueOf((short) 0));
        TestCase.assertEquals(OFQueuePropertyType.MIN_RATE,
                OFQueuePropertyType.valueOf((short) 1));
    }
}
