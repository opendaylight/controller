/*
 * Copyright (c) 2014 NEC Corporation
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this
 * distribution, and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.action;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * JUnit test for {@link SetNwTos}.
 */
public class SetNwTosTest {
    /**
     * Test case for {@link SetNwTos#getNwTos()}.
     */
    @Test
    public void testGetNwTos() {
        int[] dscps = {0x00, 0x01, 0x03, 0x20, 0x21, 0x3E, 0x3F};
        for (int dscp : dscps) {
            SetNwTos action = new SetNwTos(dscp);
            int tos = dscp << SetNwTos.ECN_FIELD_SIZE;
            assertEquals(tos, action.getNwTos());
        }
    }

    /**
     * Test case for {@link SetNwTos#getDscp()}.
     */
    @Test
    public void testGetDscp() {
        int[] dscps = {0x00, 0x01, 0x20, 0x21, 0x3E, 0x3F};
        for (int dscp : dscps) {
            SetNwTos action = new SetNwTos(dscp);
            assertEquals(dscp, action.getDscp());
        }
    }
}
