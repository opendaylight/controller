/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.util.messages;

import static org.junit.Assert.assertEquals;
import io.netty.buffer.Unpooled;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.opendaylight.controller.netconf.util.handler.NetconfXMLToHelloMessageDecoder;

import com.google.common.io.Files;

public class NetconfMessageFactoryTest {
    @Test
    public void testAuth() throws Exception {
        NetconfXMLToHelloMessageDecoder parser = new NetconfXMLToHelloMessageDecoder();
        File authHelloFile = new File(getClass().getResource("/netconfMessages/client_hello_with_auth.xml").getFile());

        final List<Object> out = new ArrayList<>();
        parser.decode(null, Unpooled.wrappedBuffer(Files.toByteArray(authHelloFile)), out);
        assertEquals(1, out.size());
    }
}
