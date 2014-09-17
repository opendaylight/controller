/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.util.messages;


import com.google.common.base.Optional;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.mockito.internal.util.collections.Sets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class NetconfHelloMessageTest {

    Set<String> caps;

    @Before
    public void setUp() throws Exception {
        caps = Sets.newSet("cap1");
    }

    @Test
    public void testConstructor() throws Exception {
        NetconfHelloMessageAdditionalHeader additionalHeader = new NetconfHelloMessageAdditionalHeader("name","host","1","transp","id");
        NetconfHelloMessage message = NetconfHelloMessage.createClientHello(caps, Optional.of(additionalHeader));
        assertTrue(message.isHelloMessage(message));
        assertEquals(Optional.of(additionalHeader), message.getAdditionalHeader());

        NetconfHelloMessage serverMessage = NetconfHelloMessage.createServerHello(caps, 100L);
        assertTrue(serverMessage.isHelloMessage(serverMessage));
    }
}
