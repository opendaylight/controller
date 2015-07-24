/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.util.xml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import org.junit.Test;

public class HardcodedNamespaceResolverTest {

    @Test
    public void testResolver() throws Exception {
        final HardcodedNamespaceResolver hardcodedNamespaceResolver = new HardcodedNamespaceResolver("prefix", "namespace");

        assertEquals("namespace", hardcodedNamespaceResolver.getNamespaceURI("prefix"));
        try{
            hardcodedNamespaceResolver.getNamespaceURI("unknown");
            fail("Unknown namespace lookup should fail");
        } catch(IllegalStateException e) {}

        assertNull(hardcodedNamespaceResolver.getPrefix("any"));
        assertNull(hardcodedNamespaceResolver.getPrefixes("any"));
    }
}