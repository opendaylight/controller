/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.nettyutil.handler;

import static org.junit.Assert.assertEquals;

import com.google.common.collect.Lists;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import org.junit.Test;

public class NetconfXMLToMessageDecoderTest {

    @Test
    public void testDecodeNoMoreContent() throws Exception {
        final ArrayList<Object> out = Lists.newArrayList();
        new NetconfXMLToMessageDecoder().decode(null, Unpooled.buffer(), out);
        assertEquals(0, out.size());
    }

    @Test
    public void testDecode() throws Exception {
        final ArrayList<Object> out = Lists.newArrayList();
        new NetconfXMLToMessageDecoder().decode(null, Unpooled.wrappedBuffer("<msg/>".getBytes()), out);
        assertEquals(1, out.size());
    }
}