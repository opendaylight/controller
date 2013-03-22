
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.openflow.protocol;


import org.junit.Test;
import org.openflow.protocol.action.OFActionType;

import junit.framework.TestCase;


public class OFActionTypeTest extends TestCase {
    @Test
    public void testMapping() throws Exception {
        TestCase.assertEquals(OFActionType.OUTPUT,
                OFActionType.valueOf((short) 0));
        TestCase.assertEquals(OFActionType.OPAQUE_ENQUEUE,
                OFActionType.valueOf((short) 11));
        TestCase.assertEquals(OFActionType.VENDOR,
                OFActionType.valueOf((short) 0xffff));
    }
}
